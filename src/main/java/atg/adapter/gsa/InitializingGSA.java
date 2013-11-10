/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.adapter.gsa;

import atg.adapter.gsa.xml.TemplateParser;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.junit.nucleus.TestUtils;
import atg.naming.NameContext;
import atg.naming.NameContextBindingEvent;
import atg.nucleus.Configuration;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusNameResolver;
import atg.nucleus.ServiceEvent;
import atg.nucleus.ServiceException;
import atg.nucleus.logging.LogListener;
import atg.repository.RepositoryException;
import org.apache.ddlutils.DatabaseOperationException;

import javax.transaction.TransactionManager;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * This class is an extension of atg.adapter.gsa.GSARepository. It's purpose is
 * to create tables and initial data required for starting a given repository.
 *
 * @author mfrenzel
 * @version 1.0
 */

public class InitializingGSA
        extends GSARepository {

    // -------------------------------------

    /**
     * Class version string
     */

    public static String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/test/apiauto/util/InitializingGSA.java#11 $$Change: 550950 $";

    // -----------------------------------
    // ---From Properties File------------

    private boolean mUseDDLUtils = true;

    /**
     * If true then Apache DDLUtils will be used to generate the schema. Otherwise
     * the GSA generated SQL will be used.
     *
     * @return the useDDLUtils
     */
    public boolean isUseDDLUtils() {
        return mUseDDLUtils;
    }

    /**
     * If true then Apache DDLUtils will be used to generate the schema. Otherwise
     * the GSA generated SQL will be used.
     *
     * @param pUseDDLUtils the useDDLUtils to set
     */
    public void setUseDDLUtils(boolean pUseDDLUtils) {
        mUseDDLUtils = pUseDDLUtils;
    }

    // do we want to create tables if they don't exist
    private boolean mCreateTables = true;

    public void setCreateTables(boolean pCreate) {
        mCreateTables = pCreate;
    }

    public boolean isCreateTables() {
        return mCreateTables;
    }

    // do we want to drop tables that exist if we want to create
    // a table with the same name
    private boolean mDropTables = false;

    public void setDropTablesIfExist(boolean pDrop) {
        mDropTables = pDrop;
    }

    public boolean isDropTablesIfExist() {
        return mDropTables;
    }

    // the XML files containing export data from the TemplateParser
    // it will be imported into the database after tables are created
    // we load the files as Files instead of XMLFiles because the
    // TemplateParser requires a full file path to the import file
    // instead of using the CONFIGPATH
    private File[] mImportFiles = null;

    public void setImportFiles(File[] pFiles) {
        mImportFiles = pFiles;
    }

    public File[] getImportFiles() {
        return mImportFiles;
    }

    public String[] getImportFilesAsStrings() {
        File[] f = getImportFiles();
        if ( f == null ) {
            return null;
        }

        List<String> v = new ArrayList<String>();
        for ( int i = 0; i < f.length; i++ ) {
            if ( !v.contains(f[i].getAbsolutePath()) ) {
                v.add(f[i].getAbsolutePath());
            }
        }

        return v.toArray(new String[v.size()]);
    }

    // do we want to strip the 'references(..)' statements from SQL
    // created by the GSA
    private boolean mStripReferences = true;

    public void setStripReferences(boolean pStrip) {
        mStripReferences = pStrip;
    }

    public boolean isStripReferences() {
        return mStripReferences;
    }

    // do we want to show the create table statements that are executed
    private boolean mShowCreate = false;

    public void setloggingCreateTables(boolean pLog) {
        mShowCreate = pLog;
    }

    public boolean isLoggingCreateTables() {
        return mShowCreate;
    }

    /*
     * the SQLProcessorEngine to use for creating tables this property is optional
     * because we'll create a default SQLProcessorEngine if the property isn't set
     */
    private SQLProcessorEngine mSQLProcessor = null;

    public void setSQLProcessor(SQLProcessorEngine pEngine) {
        mSQLProcessor = pEngine;
    }

    public SQLProcessorEngine getSQLProcessor() {
        // create a new processor if one isn't set
        if ( mSQLProcessor == null ) {
            mSQLProcessor = new SQLProcessorEngine(this);
            mSQLProcessor.setLoggingDebug(this.isLoggingDebug());
            mSQLProcessor.setLoggingError(this.isLoggingError());
            mSQLProcessor.setLoggingInfo(this.isLoggingInfo());
            mSQLProcessor.setLoggingWarning(this.isLoggingWarning());
            LogListener[] listeners = this.getLogListeners();
            for ( int i = 0; i < listeners.length; i++ ) {
                mSQLProcessor.addLogListener(listeners[i]);
            }
        }

        return mSQLProcessor;
    }

    /**
     * boolean indicating whether we should perform the import every time Dynamo
     * starts, or only perform the import if we created at least one table. NOTE:
     * if dropTablesIfExist is true, the import will occur every time because we
     * will create tables every time. default: false - only perform the import
     * when tables are created
     */
    private boolean mImportEveryStartup = false;

    public void setImportEveryStartup(boolean pImport) {
        mImportEveryStartup = pImport;
    }

    public boolean isImportEveryStartup() {
        return mImportEveryStartup;
    }

    /**
     * boolean indicating whether we should drop all tables associated with this
     * repository when Dynamo is shut down. NOTE: this will only work properly is
     * Dynamo is shutdown properly. It will not work if Dynamo is just killed
     * default: false
     */
    private boolean mDropTablesAtShutdown = false;

    public void setDropTablesAtShutdown(boolean pDrop) {
        mDropTablesAtShutdown = pDrop;
    }

    public boolean isDropTablesAtShutdown() {
        return mDropTablesAtShutdown;
    }

    /**
     * boolean indicating whether to wrap each imported file in it's own
     * transaction. this is a new option in D5.5 that has changed the method
     * signature of atg.adapter.gsa.xml.TemplateParser.importFiles() default: true
     */
    private boolean mImportWithTransaction = true;

    public void setImportWithTransaction(boolean pTran) {
        mImportWithTransaction = pTran;
    }

    public boolean isImportWithTransaction() {
        return mImportWithTransaction;
    }

    private Properties mSqlCreateFiles = new Properties();

    /**
     * Optional mapping of user-specified sql files that should be executed
     * instead of the SQL generated by startSQLRepository. Key values must be one
     * of (case sensitive): <b>default </b>, <b>oracle </b>, <b>solid </b>,
     * <b>informix </b>, <b>microsoft </b>, <b>sybase </b>, or <b>db2 </b>. Mapped
     * values should be a colon (:) separated ordered list of files to execute for
     * that database type. <br>
     * Specified files may use
     * <p/>
     * <pre>
     * {....}
     * </pre>
     * <p/>
     * notation to indicate a System variable that should be substituted at
     * runtime, such as
     * <p/>
     * <pre>
     * { atg.dynamo.root }
     * </pre>
     * <p/>
     * .
     * <p/>
     * The following behavior is observed:
     * <p/>
     * <pre>
     *
     *           a) database meta data is used to determine specific database type
     *           b) when &lt;b&gt;default&lt;/b&gt; not specified
     *             - if mapping exists for specific db type, those files are executed
     *             - otherwise, output from startSQLRepository is executed
     *           c) when &lt;b&gt;default&lt;/b&gt; is specified
     *             - if mapping exists for specific db type, those files are executed
     *             - otherwise, files mapped under default are executed
     *           d) if a mapping exists for a db type in 'sqlCreateFiles' then a corresponding
     *              entry (to the specific db type, or to default) must exist.  Otherwise an
     * exception
     *              is thrown at starup.
     *
     * </pre>
     * <p/>
     * Also, when a file specified in the property 'sqlCreateFiles' is used (i.e.
     * output from startSQLRepository is not being used) then the initializingGSA
     * will always do the following at startup, unless property
     * 'executeCreateAndDropScripts' is set to false:
     * <p/>
     * <pre>
     *
     *           a) execute the appropriate dropSqlFile(s)
     *           b) execute the appropriate createSqlFile(s)
     *
     * </pre>
     * <p/>
     * If 'executeCreateAndDropScripts' is false then in the case where scripts
     * normally would be run they will instead be skipped and no SQL (from scripts
     * or startSQLRepository) will be executed. The reason for this restriction is
     * that it's too difficult to know whether a database has been properly reset
     * for the 'createSqlFile(s)' to run properly, so we err on the conservative
     * side and always reset it.
     */
    public void setSqlCreateFiles(Properties pFiles) {
        mSqlCreateFiles = pFiles;
    }

    /**
     * returns optional mapping of user-specified sql files that should be
     * executed instead of the SQL generated by startSQLRepository. see
     * 'setSqlCreateFiles' for detailed explanation of this property.
     */
    public Properties getSqlCreateFiles() {
        return mSqlCreateFiles;
    }

    private Properties mSqlDropFiles = new Properties();

    /**
     * returns optional mapping of user-specified sql files that should be
     * executed during 'tear-down' instead of basing it on the SQL generated by
     * startSQLRepository. see 'setSqlCreateFiles' for detailed explanation of
     * this property.
     */
    public void setSqlDropFiles(Properties pFiles) {
        mSqlDropFiles = pFiles;
    }

    /**
     * returns optional mapping of user-specified sql files that should be
     * executed during 'tear-down' instead of basing it on the SQL generated by
     * startSQLRepository. see 'setSqlCreateFiles' for detailed explanation of
     * this property.
     */
    public Properties getSqlDropFiles() {
        return mSqlDropFiles;
    }

    private boolean mAllowNoDrop = true;

    /**
     * If true, one may specify create scripts, but no drop scripts. Otherwise it
     * is an error to specify a create script but no drop script
     *
     * @return
     */
    public boolean isAllowNoDrop() {
        return mAllowNoDrop;
    }

    public void setAllowNotDrop(boolean pDrop) {
        mAllowNoDrop = pDrop;
    }

    private boolean mExecuteCreateDropScripts = true;

    /**
     * if set to true then create and drop scripts mapped through properties
     * 'setSqlCreateFiles' and 'getSqlCreateFiles' will be executed. otherwise the
     * scripts will not be executed at startup.
     */
    public void setExecuteCreateAndDropScripts(boolean pExec) {
        mExecuteCreateDropScripts = pExec;
    }

    /**
     * returns true if create and drop scripts mapped through properties
     * 'setSqlCreateFiles' and 'getSqlCreateFiles' should be executed at startup.
     */
    public boolean isExecuteCreateAndDropScripts() {
        return mExecuteCreateDropScripts;
    }

    private boolean mLoadColumnInfosAtInitialStartup = false;

    /**
     * returns true if the GSA should load JDBC metadata when starting the initial
     * instantiation of the component. default: false
     */
    public boolean isLoadColumnInfosAtInitialStartup() {
        return mLoadColumnInfosAtInitialStartup;
    }

    /**
     * set to true if the GSA should load JDBC metadata when starting the initial
     * instantiation of the component. the default is false b/c the initial
     * instantiation is only used to create tables and loading the metadata before
     * the tables are created is unnecessary overhead which slows the startup
     * process. When the component is restarted after the tables are created it
     * uses the value of 'loadColumnInfosAtStartup' to determine whether to load
     * the metadata on the restart.
     */
    public void setLoadColumnInfosAtInitialStartup(boolean pLoad) {
        mLoadColumnInfosAtInitialStartup = pLoad;
    }

    // -------------------------------------------------------------------------
    // Member properties

    // this property is a little tricky and a bit of a hack, but it
    // allows us to create the tables, etc on startup. When the component
    // is initially started this will be false, but when it calls restart,
    // we set it to true for the new instantiation to avoid infinitely
    // recursing into new repositories
    private boolean mTemporaryInstantiation = false;

    public void setIsTemporaryInstantiation(boolean pTemp) {
        mTemporaryInstantiation = pTemp;
    }

    private boolean isTemporaryInstantiation() {
        return mTemporaryInstantiation;
    }

    private boolean mRestartAfterTableCreation = true;

    private GSARepositorySchemaGenerator mGenerator;

    /**
     * Returns true if this repository will attempt to "restart" after creating
     * tables.
     *
     * @return
     */
    public boolean isRestartingAfterTableCreation() {
        return mRestartAfterTableCreation;
    }

    /**
     * Sets if this repository will attempt to "restart" after creating tables. A
     * value of true means that it should restart.
     */
    public void setRestartingAfterTableCreation(boolean pRestart) {
        mRestartAfterTableCreation = pRestart;
    }

    // -------------------------------------------------------------------------
    // Methods

    /**
     * Overrides doStartService from GSARepository to make the repository
     * optionally create required tables and load data using the TemplateParser
     * -import flag.
     *
     * @throws RepositoryException (?)
     */
    public void doStartService() {
        // Loading Column Infos in a separate thread
        // can deadlock the Initializing GSA
        if ( isLoggingInfo() ) {
            logInfo("Setting loadColumnInfosInSeparateThread to false.");
        }
        setLoadColumnInfosInSeparateThread(false);
        // if this is the temporary instantiation, we just want to
        // call super.doStartService() and return
        if ( isTemporaryInstantiation() ) {
            if ( isLoggingInfo() ) {
                logInfo("Restarting the GSA component to successfully load XML templates...");
            }
            super.doStartService();
            return;
        }

        try {
            // otherwise, this is the 'real' instantiation and we want to
            // do more....

            if ( isLoggingInfo() ) {
                logInfo("\nInitializing the primary GSA component and checking tables...");
            }
            if ( isLoggingDebug() && this.getDebugLevel() <= 6 ) {
                logDebug("For additional debugging statements, set debugLevel > 6");
            }

            // make sure mappings for user specified SQL files are ok
            validateUserSpecifiedSqlFiles();

            // we set logError and checkTables to false because tables
            // probably won't exist and it'll just throw a bunch of
            // unnecessary errors. also, we don't want to see errors because
            // add-items, delete-items, etc. will fail
            boolean logErrors = isLoggingError();
            boolean checkTables = isCheckTables();
            boolean logWarnings = isLoggingWarning();
            setCheckTables(false);
            setLoggingError(true);

            // also set 'loadColumnInfosAtStartup' to false to prevent attempts at
            // loading
            // lots of unwanted metadata. that's very time consuming and only needed
            // by the
            // final instantiation. The setLoadColumnInfosAtStartup method is new so
            // use a
            // try / catch in case we're dealing with an old version of GSARepository
            boolean loadColumnInfosAtStartup = true;
            try {
                loadColumnInfosAtStartup = isLoadColumnInfosAtStartup();
                setLoadColumnInfosAtStartup(isLoadColumnInfosAtInitialStartup());
                if ( isLoadColumnInfosAtInitialStartup() ) {
                    if ( isLoggingInfo() ) {
                        logInfo("Enabled loading of column info for initial startup");
                    }
                } else {
                    if ( isLoggingInfo() ) {
                        logInfo("Disabled loading of column info for initial startup");
                    }
                }
            } catch ( Throwable t ) {
                if ( isLoggingDebug() ) {
                    logDebug("Could not modify loading of column metadata for preliminary startup.");
                }
            }

            // call GSA.doStartService to load XML definition files
            super.doStartService();

            // reset 'LoadColumnInfosAtStartup' to whatever it was originally
            try {
                setLoadColumnInfosAtStartup(loadColumnInfosAtStartup);
            } catch ( Throwable t ) {
                logError(t);
            }

            // reset check tables and loggingError
            setCheckTables(checkTables);
            setLoggingError(logErrors);
            setLoggingWarning(logWarnings);

            // now create the tables and restart the repository
            boolean createdTables = createTables();
            if ( isRestartingAfterTableCreation() ) {
                restart();
            }

            // it's a little hidden, but when we just called restart(),
            // we actually started up a temporary instantiation of this GSA
            // which should have successfully loaded it's XML definition file
            // because the tables were already created. This component
            // (the primary one) then copied all the properties from the
            // temporary instantiation so it's just like this component loaded
            // the XML definition file successfully :)

            // we're now ready to import specified XML files
            if ( isImportEveryStartup() || createdTables ) {
                importFiles();
            } else {
                if ( isLoggingInfo() ) {
                    logInfo("Import not performed because importEveryStartup is false and no tables were created.");
                }
            }

            if ( isLoggingInfo() ) {
                logInfo("Component finished starting up.");
            }

        } catch ( Exception e ) {
            logError("Caught an unexpected exception trying to start component...", e);
        }
    }

    // -----------------------------------------

    /**
     * Restarts the repository. This involves re-reading nucleus properties,
     * reloading definition files, and invalidating all cache entries. This method
     * is a convenience for development purposes (to avoid restarting dynamo when
     * a template has changed), and should not be used on a live site. This method
     * is modified slightly from the restart method of GSARepository because it
     * sets mTemporaryInstantiation to true so that the doStartService method of
     * the new instance does not reload import files or try to recreate tables
     */
    public boolean restart()
            throws ServiceException {
        Configuration c = getServiceConfiguration();
        NucleusNameResolver r = new NucleusNameResolver(
                getNucleus(), getNucleus(), getNameContext(), true
        );
        InitializingGSA newRepository = (InitializingGSA) c.createNewInstance(this);
        c.configureService(newRepository, r, this);

        // Fool this new repository into thinking that it has been
        // bound to the same name context as the original repository
        // This changes will make sure that getAbsoluteName() returns
        // a correct value.
        NameContext nc = this.getNameContext();
        NameContextBindingEvent bindingEvent = new NameContextBindingEvent(
                this.getName(), newRepository, this.getNameContext()
        );
        newRepository.nameContextElementBound(bindingEvent);

        ServiceEvent ev = new ServiceEvent(this, newRepository, getNucleus(), c);
    /*
     * We are purposefully not putting the new repository into the parent's name
     * context. The existing repository is always the valid one. We're starting
     * this new guy, then we're going to synchronize on the repository and get
     * all of its info into us.
     */

        // we have to set the new repository as temporary so it won't call
        // restart and start an infinite recursion
        newRepository.setIsTemporaryInstantiation(true);

        newRepository.startService(ev);
        if ( newRepository.isRunning() ) {
            synchronized ( this ) {
                invalidateCaches();
                copyFromOtherRepository(newRepository);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method is called when the repository is shutdown. If
     * dropTablesAtShutdown is true, it will attempt to drop all the tables.
     * IMPORTANT: There is an optional property that can be set to indicate that
     * all tables should be dropped at shutdown (dropTablesAtShutdown). Because of
     * the order in which Nucleus shuts down the components, this may or may not
     * work. It just depends on whether the datasource is shutdown before the
     * repository. If you want to guarantee that your tables are dropped, manually
     * invoke the doStopService method from the HTML admin pages.
     */
    public void doStopService() {
        try {
            // clear out state in SchemaTracker
            SchemaTracker.getSchemaTracker().reset();
            if ( isDropTablesAtShutdown() ) {
                if ( isLoggingInfo() ) {
                    logInfo("Dropping tables because 'dropTablesAtShutdown' is true....");
                }
                dropTables();
            }
        } catch ( Exception e ) {
            if ( isLoggingError() ) {
                logError(e);
            }
        } finally {
            super.doStopService();
        }
    }

    /**
     * This method drops all tables required by the GSARepository.
     *
     * @throws RepositoryException   if an error occurs while retrieving a list of the tables
     *                               associated with the repository
     * @throws SQLProcessorException if an error occured trying to drop the tables
     */
    public void dropTables()
            throws RepositoryException, SQLProcessorException {
        // execute SQL files, if specified
        String[] dropFiles = getSpecifiedDropFiles();
        if ( dropFiles != null ) {
            if ( isExecuteCreateAndDropScripts() ) {
                executeSqlFiles(dropFiles, false);
            } else if ( isLoggingInfo() ) {
                logInfo("Skipping execution of SQL scripts b/c property 'executeCreateAndDropScripts' is false or there are no drop scripts.");
            }
            return;
        }

        // otherwise, just drop tables based on startSQLRepository SQL

        if ( isUseDDLUtils() ) {
            try {
                if ( !Nucleus.getGlobalNucleus().isStopping() ) {
                    // build a new one
                    mGenerator = new GSARepositorySchemaGenerator(this);
                }
                if ( mGenerator != null ) {
                    mGenerator.dropSchema(true);
                }
            } catch ( DatabaseOperationException e ) {
                throw new RepositoryException(e);
            }
        } else {
            List<String> statements = getCreateStatements(null, null);
            SQLProcessorEngine processor = getSQLProcessor();
            processor.dropTablesFromCreateStatements(statements);

        }
    }

    /**
     * This method creates the tables required by the GSARepository. If desired,
     * check to make sure all the tables exist in the database. If a table doesn't
     * exist, create it; if it does exist, don't do anything to it unless user
     * wants to drop existing tables
     *
     * @return boolean - true if tables were created
     * @throws RepositoryException   if an error occurs while retrieving a list of the tables to
     *                               create
     * @throws SQLProcessorException if an error occured trying to create the tables
     */
    private boolean createTables()
            throws RepositoryException, SQLProcessorException {
        // execute SQL files, if specified
        String[] createFiles = getSpecifiedCreateFiles();
        if ( createFiles != null ) {
            if ( !isExecuteCreateAndDropScripts() ) {
                if ( isLoggingInfo() ) {
                    logInfo("Skipping execution of SQL scripts b/c property 'executeCreateAndDropScripts' is false.");
                }
                return false;
            }
            // before executing the createFiles we always execute the drop files
            dropTables();
            executeSqlFiles(createFiles, true);
            return true;
        }

        // otherwise, just execute sql from startSQLRepository
        boolean createdTables = false;

        if ( isUseDDLUtils() ) {
            if ( isCreateTables() ) {
                mGenerator = new GSARepositorySchemaGenerator(this);
                try {
                    mGenerator.createSchema(true, isDropTablesIfExist());
                    createdTables = true;
                } catch ( DatabaseOperationException e ) {
                    throw new RepositoryException(e);
                }
            }
        } else {
            // Use GSA Generated SQL
            SQLProcessorEngine spe = getSQLProcessor();
            // turn on debug for SQLProcessorEngine if GSA has debug on if
            // (isLoggingDebug())
            spe.setLoggingDebug(true);
            List<String> createStatements = getCreateStatements(null, null);
            createdTables = spe.createTables(createStatements, isDropTablesIfExist());

        }

        return createdTables;
    }

    /**
     * This method imports files using the TemplateParser
     *
     * @throws RepositoryException if an error occured while importing one of the xml files.
     */
    private void importFiles()
            throws RepositoryException {
        if ( isLoggingInfo() ) {
            logInfo("Importing files...");
        }

        String[] loadFiles = getImportFilesAsStrings();
        // just exit if no files were specified
        if ( loadFiles == null ) {
            if ( isLoggingInfo() ) {
                logInfo("No files specified for import.");
            }
            return;
        }

        if ( isLoggingDebug() ) {
            logDebug("The following files will be imported:");
            for ( int i = 0; i < loadFiles.length; i++ ) {
                logDebug("file: " + loadFiles[i]);
            }
        }

        // now load the import files if they were specified
        PrintWriter ps = new PrintWriter(System.out);
        if ( loadFiles.length > 0 ) {
            try {
                TemplateParser.importFiles(
                        this, loadFiles, ps, isImportWithTransaction()
                );
            } catch ( Exception e ) {
                throw new RepositoryException(
                        "Exception caught importing files into repository.", e
                );
            }
        }
    }

    /**
     * This method is used to remove the 'references...' parts from sql generated
     * by the GSA. Removing the references allows us to avoid problems of creating
     * tables in the wrong order and also allows you to easily drop / recreate
     * tables.
     */
    private String stripReferences(String pStr) {
        if ( isLoggingDebug() ) {
            logDebug("Removing references from SQL string...");
            if ( this.getDebugLevel() > 6 ) {
                logDebug("SQL string before references are removed: \n" + pStr);
            }
        }

        pStr = stripForeignKey(pStr);

        // must be of the following format
        // fieldname data-type null references foo(id),
        String ref = "references ";
        String endRef = ",";

        StringBuffer sb = new StringBuffer();
        int start = 0;
        int end = 0;
        end = pStr.indexOf(ref);

        // if unable to find "references ", try "REFERENCES " instead
        if ( end == -1 ) {
            ref = ref.toUpperCase();
            end = pStr.indexOf(ref);
        }

        while ( end != -1 ) {
            String temp = pStr.substring(start, end);
            sb.append(temp);
            pStr = pStr.substring(end + ref.length());
            start = pStr.indexOf(endRef);
            end = pStr.indexOf(ref);
        }
        String temp2 = pStr.substring(start);
        sb.append(temp2);

        if ( isLoggingDebug() ) {
            logDebug("Final sql string -> references removed: \n" + sb.toString());
        }

        return sb.toString();
    }

    private String stripForeignKey(String pStr) {
        if ( isLoggingDebug() ) {
            logDebug("Removing Foreign Key from SQL string...");
            if ( this.getDebugLevel() > 6 ) {
                logDebug("SQL string before Foreign Key are removed: \n" + pStr);
            }
        }

        String key = "foreign key";
        int flag = 0;
        int end = 0;
        end = pStr.toLowerCase().lastIndexOf(key);

        while ( end != -1 ) {
            flag = 1;
            pStr = pStr.substring(0, end);
            end = pStr.toLowerCase().lastIndexOf(key);
        }
        end = pStr.lastIndexOf(",");
        if ( flag == 0 ) {
            return pStr;
        } else {
            return pStr.substring(0, end) + " )";
        }
    }

    /**
     * This method is used to retrieve all of the CREATE TABLE statements that are
     * needed to generate tables for this GSA
     *
     * @throws RepositoryException if an error occurs with the Repository
     */
    private List<String> getCreateStatements(PrintWriter pOut, String pDatabaseName)
            throws RepositoryException {
        List<String> tableStatements = new ArrayList<String>();
        List<String> indexStatements = new ArrayList<String>();

        // use current database if none is supplied
        if ( pDatabaseName == null ) {
            pDatabaseName = getDatabaseName();
        }

        String[] descriptorNames = getItemDescriptorNames();
        OutputSQLContext sqlContext = new OutputSQLContext(pOut);
        GSAItemDescriptor itemDescriptors[];
        DatabaseTableInfo dti = getDatabaseTableInfo(pDatabaseName);
        int i, length = descriptorNames.length;

        itemDescriptors = new GSAItemDescriptor[length];
        for ( i = 0; i < length; i++ ) {
            itemDescriptors[i] = (GSAItemDescriptor) getItemDescriptor(descriptorNames[i]);
        }

        String create = null;
        String index = null;
        for ( i = 0; i < length; i++ ) {
            GSAItemDescriptor desc = itemDescriptors[i];
            Table[] tables = desc.getTables();
            if ( tables != null ) {
                for ( int j = 0; j < tables.length; j++ ) {
                    Table t = tables[j];
                    if ( !t.isInherited() ) {
                        sqlContext.clear();
                        create = t.generateSQL(sqlContext, pDatabaseName);
                        // get rid of any possible CREATE INDEX statements and store those
                        // in their own Vector of statements...
                        index = extractIndexStatement(create);
                        create = removeIndexStatements(create);
                        if ( isStripReferences() ) {
                            create = stripReferences(create);
                        }
                        if ( index != null && !indexStatements.contains(index) ) {
                            indexStatements.add(index);
                        }
                        if ( create != null && !tableStatements.contains(create) ) {
                            tableStatements.add(create);
                        }
                    }
                }
            }
        }
    /*
     * if (pOut != null) { pOut.print(buffer); pOut.flush(); }
     */

        return tableStatements;
    }

    /**
     * This method is used to extract a possible CREATE INDEX statement from a
     * CREATE TABLE statement that is generated by a Table. If no CREATE INDEX
     * statement is included, it returns null
     */
    private String extractIndexStatement(String pStatement) {
        String search = "CREATE INDEX ";
        String copy = pStatement.toUpperCase();
        int i = copy.indexOf(search);
        if ( i != -1 ) {
            return stripTrailingSemiColon(pStatement.substring(i));
        }

        return null;
    }

    /**
     * This method is used to remove any possible CREATE INDEX statements from the
     * end of a CREATE TABLE statement generated by a Table. It returns the CREATE
     * TABLE statement with all CREATE INDEX statements removed.
     */
    private String removeIndexStatements(String pStatement) {
        String search = "CREATE INDEX ";
        String copy = pStatement.toUpperCase();
        int i = copy.indexOf(search);
        if ( i != -1 ) {
            pStatement = pStatement.substring(0, i);
        }

        return stripTrailingSemiColon(pStatement);
    }

    /**
     * This method is used to remove the trailing semicolon from a String. It is
     * assumed that these strings will only possibly have one semicolon, and that
     * if there is one everything after the semicolon is junk.
     */
    private String stripTrailingSemiColon(String pStr) {
        if ( pStr == null ) {
            return pStr;
        }
        int idx = pStr.indexOf(";");
        if ( idx != -1 ) {
            pStr = pStr.substring(0, idx);
        }

        return pStr;
    }

    // ---------- methods to help with user-specified SQL files -----------
    // allowable db types to specify
    private final String SOLID = "solid";

    private final String ORACLE = "oracle";

    private final String MICROSOFT = "microsoft";

    private final String INFORMIX = "informix";

    private final String DB2 = "db2";

    private final String SYBASE = "sybase";

    private final String SYBASE2 = "Adaptive Server Enterprise"; // sybase 12.5

    private final String DEFAULT = "default";

    private final String[] dbTypes = {
            SOLID, ORACLE, MICROSOFT, INFORMIX, DB2, SYBASE, SYBASE2, DEFAULT
    };

    /**
     * returns the dbtype for the database being used. returned value will be one
     * of the constants SOLID, ORACLE, MICROSOFT, INFORMIX, DB2, SYBASE, or
     * DEFAULT if db type can not be determined.
     */
    private String getDatabaseType() {
        String type = getDatabaseName();
        for ( int i = 0; i < dbTypes.length; i++ ) {
            if ( type.toLowerCase().indexOf(dbTypes[i].toLowerCase()) > -1 ) {
                if ( dbTypes[i].equals(SYBASE2) ) {
                    return SYBASE;
                }
                return dbTypes[i];
            }
        }
        return DEFAULT;
    }

    /**
     * returns array of user-specified SQL files that should be executed, or null
     * if output from startSQLRepository should be used.
     *
     * @throws RepositoryException if an error occurs getting the array of files to execute
     */
    private String[] getSpecifiedCreateFiles()
            throws RepositoryException {
        // try to get mapped value for this specific db type, and if it's empty try
        // the default
        String files = (String) getSqlCreateFiles().get(getDatabaseType());
        if ( files == null ) {
            files = (String) getSqlCreateFiles().get(DEFAULT);
        }
        // if it's still empty then just return b/c there's nothing to execute
        if ( files == null ) {
            return null;
        }

        // if file list is not null, convert it and return the array
        try {
            return TestUtils.convertFileArray(files, ":");
        } catch ( Exception e ) {
            throw new RepositoryException(e);
        }
    }

    /**
     * returns array of user-specified SQL files that should be executed, or null
     * if output from startSQLRepository should be used.
     *
     * @throws RepositoryException if an error occurs getting the array of files to execute
     */
    private String[] getSpecifiedDropFiles()
            throws RepositoryException {
        // try to get mapped value for this specific db type, and if it's empty try
        // the default
        String files = (String) getSqlDropFiles().get(getDatabaseType());
        if ( files == null ) {
            files = (String) getSqlDropFiles().get(DEFAULT);
        }
        // if it's still empty then just return b/c there's nothing to execute
        if ( files == null ) {
            return null;
        }

        // if file list is not null, convert it and return the array
        try {
            return TestUtils.convertFileArray(files, ":");
        } catch ( Exception e ) {
            throw new RepositoryException(e);
        }
    }

    /**
     * verifies that SQL files specified by user are ok. in particular, that if
     * the user mapped a 'createSqlFile' for a db type there is a corresponding
     * 'dropSqlFile' entry, and vice-versa.
     *
     * @throws RepositoryException if anything is wrong
     */
    private void validateUserSpecifiedSqlFiles()
            throws RepositoryException {
        // don't let them be null
        if ( getSqlCreateFiles() == null ) {
            setSqlCreateFiles(new Properties());
        }
        if ( getSqlDropFiles() == null ) {
            setSqlDropFiles(new Properties());
        }
        // make sure all the keys are valid
        Set<Object> keys = new HashSet<Object>();
        keys.addAll(getSqlCreateFiles().keySet());
        keys.addAll(getSqlDropFiles().keySet());
        Set<String> allow_keys = new HashSet<String>();
        for ( int i = 0; i < dbTypes.length; i++ ) {
            keys.remove(dbTypes[i]);
            if ( !dbTypes[i].equals(SYBASE2) ) {
                allow_keys.add(dbTypes[i]);
            }
        }
        if ( keys.size() > 0 ) {
            throw new RepositoryException(
                    "The following keys used in the 'sqlCreateFiles' and/or 'sqlDropFiles' properties "
                    + "are invalid: "
                    + keys
                    + ".  Allowable keys are: "
                    + allow_keys
            );
        }

        boolean isDefaultCreate = (getSqlCreateFiles().get(DEFAULT) != null);
        boolean isDefaultDrop = (getSqlDropFiles().get(DEFAULT) != null);
        // if there are defaults it will always be ok, so just return
        if ( isDefaultCreate && isDefaultDrop ) {
            return;
        }

        // otherwise, check each dbType individually
        for ( int i = 0; i < dbTypes.length; i++ ) {
            boolean isCreate = (getSqlCreateFiles().get(dbTypes[i]) != null);
            boolean isDrop = (getSqlDropFiles().get(dbTypes[i]) != null);
            if ( !isAllowNoDrop() ) {
                if ( isCreate && !isDrop && !isDefaultDrop ) {
                    throw new RepositoryException(
                            "Mapping exists for database type "
                            + dbTypes[i]
                            + " in property 'sqlCreateFiles', but not in property 'sqlDropFiles', and "
                            + "there is no default specified."
                    );
                }
                if ( isDrop && !isCreate && !isDefaultCreate ) {
                    throw new RepositoryException(
                            "Mapping exists for database type "
                            + dbTypes[i]
                            + " in property 'sqlDropFiles', but not in property 'sqlCreateFiles', and "
                            + "there is no default specified."
                    );
                }
            }
        }
    }

    /**
     * executes the specified SQL files against this Repository's DataSource.
     *
     * @param pFiles       the files to execute
     * @param pStopAtError true if execution should stop at first error. if false, then
     *                     a warning will be printed for encountered errors.
     *
     * @throws RepositoryException if pStopAtError is true and an error occurs while executing
     *                             one of the sql statements.
     */
    private void executeSqlFiles(String[] pFiles, boolean pStopAtError)
            throws RepositoryException {
        // XXX: again with this bullshit
        SQLProcessor sp = new SQLProcessor(getTransactionManager(), getDataSource());
        boolean success = false;
        TransactionDemarcation td = new TransactionDemarcation();
        try {
            td.begin(
                    (TransactionManager) Nucleus.getGlobalNucleus().resolveName(
                            "/atg/dynamo/transaction/TransactionManager"
                    )
            );

            // for sql server auto-commit must be true
            // adamb: Hmm Marty added this, but it
            // breaks against MSSQL 8
            // if (getDatabaseType().equals(MICROSOFT))
            //  sp.setAutoCommit(true);
            SQLFileParser parser = new SQLFileParser();
            for ( int i = 0; i < pFiles.length; i++ ) {
                String file = pFiles[i];
                // switch the file path so everything is forward slashes
                file = file.replace('\\', '/');
                String cmd = null;
                Iterator cmds = null;
                if ( isLoggingInfo() ) {
                    logInfo("Executing SQL file: " + file);
                }
                if ( !new File(file).exists() ) {
                    throw new RepositoryException("SQL file " + file + " does not exist.");
                }

                // parse the file to get commands...
                try {
                    Collection c = parser.parseSQLFile(file);
                    if ( isLoggingDebug() ) {
                        logDebug("Parsed " + c.size() + " SQL command(s) from file.");
                    }
                    cmds = c.iterator();
                } catch ( Exception e ) {
                    // an error parsing the file indicates something very wrong, so bail
                    throw new RepositoryException(
                            "Error encountered parsing SQL file " + file, e
                    );
                }

                // then execute the commands...
                while ( cmds.hasNext() ) {
                    cmd = (String) cmds.next();
                    if ( cmd.trim().length() == 0 ) {
                        continue;
                    }
                    if ( isLoggingDebug() || isLoggingCreateTables() ) {
                        logDebug("Executing SQL cmd [" + cmd + "]");
                    }
                    try {
                        sp.executeSQL(cmd);
                    } catch ( Exception e ) {
                        if ( pStopAtError ) {
                            throw new RepositoryException(
                                    "Error received executing command ["
                                    + cmd
                                    + "] from SQL file "
                                    + file, e
                            );
                        } else {
                            if ( isLoggingWarning() ) {
                                logWarning(
                                        "Error received executing command ["
                                        + cmd
                                        + "] from SQL file "
                                        + file
                                        + ": "
                                        + e.getMessage()
                                );
                            }
                        }
                    }
                }
            }
            success = true;
        } catch ( TransactionDemarcationException e ) {
            logError(e);
        } finally {
            try {
                td.end(!success);
            } catch ( TransactionDemarcationException e ) {
                logError(e);
            }
        }
    }

} // end of class

