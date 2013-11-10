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
import atg.adapter.gsa.xml.VersioningContextUtil;
import atg.adapter.version.VersionRepository;
import atg.naming.NameContext;
import atg.nucleus.Configuration;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusNameResolver;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceEvent;
import atg.nucleus.ServiceException;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.test.util.DBUtils;
import atg.versionmanager.VersionManager;
import atg.versionmanager.Workspace;
import atg.versionmanager.exceptions.VersionException;
import junit.framework.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A utility class to simplify testing with GSARepositories inside of junit
 * tests.
 *
 * @author adamb
 * @version $Revision: #28 $
 */
public class GSATestUtils {

    private static List<File> mFilesCreated = new ArrayList<File>();

    private static String sClassName = "atg.adapter.gsa.InitializingGSA";

    private static String sVersionedClassName = "atg.adapter.gsa.InitializingVersionRepository";

    private boolean mVersioned = false;

    private static GSATestUtils SINGLETON_DEFAULT = null;

    private static GSATestUtils SINGLETON_VERSIONED = null;

    private static Logger log = LogManager.getLogger();

    /**
     * @param pB
     */
    public GSATestUtils(boolean pB) {
        mVersioned = pB;

    }

    /**
     * Duplicates the given array of repositories.
     * This method first binds the repositories into nucleus under the name XXXX-Shadow,
     * where XXXX is the original name.
     * After all repositories are bound, then they are started.
     * This allows for repositories with circular references to each other to be deplicated.
     * The pRepositories array and pDS array should be in sync. That is the first item in the
     * repository array, pRepositories,  will use the first data source in the pDS array and so on.
     *
     * @param pRepositories
     * @param pDS
     *
     * @return
     */
    public static Repository[] duplicateRepositories(GSARepository[] pRepositories,
                                                     DataSource[] pDS)
            throws ServiceException {
        GSARepository[] newReps = new GSARepository[pRepositories.length];
        for ( int i = 0; i < pRepositories.length; i++ ) {
            newReps[i] = (GSARepository) duplicateRepository(pRepositories[i], pDS[i], false);
        }
        for ( int i = 0; i < newReps.length; i++ ) {
            startRepository(
                    pRepositories[i],
                    pRepositories[i].getServiceConfiguration(),
                    pRepositories[i].getNucleus(),
                    newReps[i]
            );
        }
        return newReps;
    }

    public static Repository duplicateRepository(GSARepository pRepository, DataSource pDS)
            throws ServiceException {
        return duplicateRepository(pRepository, pDS, true);
    }

    /**
     * Duplicates the given repository, binds it into nucleus with the suffix "-Shadow"
     *
     * @param pStart If true, also starts the repository
     */
    public static Repository duplicateRepository(GSARepository pRepository,
                                                 DataSource pDS,
                                                 boolean pStart)
            throws ServiceException {
        Configuration c = pRepository.getServiceConfiguration();
        Nucleus n = pRepository.getNucleus();
        NucleusNameResolver r = new NucleusNameResolver(
                n, n, pRepository.getNameContext(), true
        );
        GSARepository newRepository = null;
        if ( pRepository instanceof VersionRepository ) {
            newRepository = (GSARepository) c.createNewInstance(((VersionRepository) pRepository).getWrappedRepository());
            c.configureService(
                    newRepository, r, ((VersionRepository) pRepository).getWrappedRepository()
            );
        } else {
            newRepository = (GSARepository) c.createNewInstance(pRepository);
            c.configureService(newRepository, r, pRepository);
        }
        newRepository.setDataSource(pDS);
        newRepository.setForeignRepositorySuffix("-Shadow");

    /*
    // Fool this new repository into thinking that it has been
    // bound to the same name context as the original repository
    // This changes will make sure that getAbsoluteName() returns
    // a correct value.
    NameContext nc = ((GenericService) pRepository).getNameContext();
    NameContextBindingEvent bindingEvent = new NameContextBindingEvent(pRepository
        .getName()+"-Shadow", newRepository, pRepository.getNameContext());
    newRepository.nameContextElementBound(bindingEvent);
    */
        NameContext nc = ((GenericService) pRepository).getNameContext();
        nc.putElement(pRepository.getName() + "-Shadow", newRepository);

        if ( pStart ) {
            startRepository(pRepository, c, n, newRepository);
        }
        return newRepository;
    }

    /**
     * @param pRepository
     * @param c
     * @param n
     * @param newRepository
     *
     * @throws ServiceException
     */
    private static void startRepository(GSARepository pRepository,
                                        Configuration c,
                                        Nucleus n,
                                        GSARepository newRepository)
            throws ServiceException {
        ServiceEvent ev = new ServiceEvent(pRepository, newRepository, n, c);
    /*
     * We are purposefully not putting the new repository into the parent's name
     * context. The existing repository is always the valid one. We're starting
     * this new guy, then we're going to synchronize on the repository and get
     * all of its info into us.
     */

        // we have to set the new repository as temporary so it won't call
        // restart and start an infinite recursion
        //    newRepository.setIsTemporaryInstantiation(true);

        // But don't load data
        if ( newRepository instanceof InitializingGSA ) {
            ((InitializingGSA) newRepository).setImportFiles(null);
        }


        newRepository.startService(ev);
        if ( newRepository.isRunning() ) {
            synchronized ( pRepository ) {
                //        newRepository.copyFromOtherRepository(pRepository);
                newRepository.invalidateCaches();
            }
        }
    }

    /**
     * Given a directory, pRoot, this method creates the minimal .properties files
     * required to startup a GSA Repository from Nucleus.
     * <p/>
     * The pJDBCProperties parameter should contain the JDBC properties used to
     * create a FakeXADataSource. Required Properties are: driver URL user
     * password
     * <p/>
     * For example, driver=solid.jdbc.SolidDriver URL=jdbc:solid://localhost:1313
     * user=admin password=admin
     * <p/>
     * <BR>
     * One should call the <code>cleanup()</code> method to remove any files
     * created by this method call.
     *
     * @param pRoot            The root directory of the testing configpath
     * @param pRepositoryPath  The Nucleus path of your testing repository
     * @param pDefinitionFiles Array of Nucleus paths to definition files
     * @param pJDBCProperties  properties object containing JDBC connection information
     * @param pImportFile
     *
     * @throws IOException
     * @throws Exception
     * @see atg.adapter.gsa.GSATestUtils#cleanup()
     */
    public void initializeMinimalConfigpath(File pRoot,
                                            String pRepositoryPath,
                                            String[] pDefinitionFiles,
                                            Properties pJDBCProperties,
                                            String pCreateSQLAbsolutePath,
                                            String pDropSQLAbsolutePath,
                                            String[] pImportFile)
            throws IOException, Exception {
        initializeMinimalConfigpath(
                pRoot,
                pRepositoryPath,
                pDefinitionFiles,
                pJDBCProperties,
                pCreateSQLAbsolutePath,
                pDropSQLAbsolutePath,
                pImportFile,
                true
        );
    }

    /**
     * Given a directory, pRoot, this method creates the minimal .properties files
     * required to startup a GSA Repository from Nucleus.
     * <p/>
     * The pJDBCProperties parameter should contain the JDBC properties used to
     * create a FakeXADataSource. Required Properties are: driver URL user
     * password
     * <p/>
     * For example, driver=solid.jdbc.SolidDriver URL=jdbc:solid://localhost:1313
     * user=admin password=admin
     * <p/>
     * <BR>
     * One should call the <code>cleanup()</code> method to remove any files
     * created by this method call.
     *
     * @param pRoot            The root directory of the testing configpath
     * @param pRepositoryPath  The Nucleus path of your testing repository
     * @param pDefinitionFiles Array of Nucleus paths to definition files
     * @param pJDBCProperties  properties object containing JDBC connection information
     * @param pImportFile
     * @param pLogging         if true log to stdout, else logging is disabled
     *
     * @throws IOException
     * @throws Exception
     * @see atg.adapter.gsa.GSATestUtils#cleanup()
     */
    public void initializeMinimalConfigpath(File pRoot,
                                            String pRepositoryPath,
                                            String[] pDefinitionFiles,
                                            Properties pJDBCProperties,
                                            String pCreateSQLAbsolutePath,
                                            String pDropSQLAbsolutePath,
                                            String[] pImportFile,
                                            boolean pLogging)
            throws IOException, Exception {
        initializeMinimalConfigpath(
                pRoot,
                pRepositoryPath,
                pDefinitionFiles,
                pJDBCProperties,
                pCreateSQLAbsolutePath,
                pDropSQLAbsolutePath,
                pImportFile,
                pLogging,
                null,
                null
        );

    }


    public void initializeMinimalConfigpath(File pRoot,
                                            String pRepositoryPath,
                                            String[] pDefinitionFiles,
                                            Properties pJDBCProperties,
                                            String pCreateSQLAbsolutePath,
                                            String pDropSQLAbsolutePath,
                                            String[] pImportFile,
                                            boolean pLogging,
                                            String pFakeXADataSourceComponentName,
                                            String pJTDataSourceComponentName)
            throws IOException, Exception {
        if ( pRepositoryPath != null ) {
            createRepositoryPropertiesFile(
                    pRoot,
                    pRepositoryPath,
                    pDefinitionFiles,
                    pCreateSQLAbsolutePath,
                    pDropSQLAbsolutePath,
                    pImportFile,
                    pJTDataSourceComponentName
            );
        }
        createTransactionManager(pRoot);
        createUserTransaction(pRoot);
        createXMLToolsFactory(pRoot);
        createIdGenerator(pRoot);
        createClientLockManager(pRoot);
        DBUtils.createJTDataSource(pRoot);
        if ( pJDBCProperties != null ) {
            GSATestUtils.createFakeXADataSource(
                    pRoot, pJDBCProperties, pFakeXADataSourceComponentName
            );
        } else {
            GSATestUtils.createFakeXADataSource(
                    pRoot, DBUtils.getHSQLDBInMemoryDBConnection(), pFakeXADataSourceComponentName
            );
        }
        if ( pFakeXADataSourceComponentName == null && pJTDataSourceComponentName == null ) {
            DBUtils.createJTDataSource(pRoot);
        } else {
            DBUtils.createJTDataSource(
                    pRoot, pJTDataSourceComponentName, pFakeXADataSourceComponentName
            );
        }
        createGlobal(pRoot);
        createScreenLog(pRoot, pLogging);
        createIdSpaces(pRoot);
        if ( pJDBCProperties != null ) {
            createIDGeneratorTables(new DBUtils(pJDBCProperties));
        } else {
            createIDGeneratorTables(new DBUtils(DBUtils.getHSQLDBInMemoryDBConnection()));
        }
        createSQLRepositoryEventServer(pRoot);
        createNucleus(pRoot);

    }

    // ---------------------------

    /**
     * Deletes any files created by initializing the configpath
     */
    public void cleanup() {
        Iterator<File> iter = mFilesCreated.iterator();
        while ( iter.hasNext() ) {
            File f = iter.next();
            f.delete();
        }
        mFilesCreated.clear();
    }

    // ---------------------------

    /**
     * Writes the idspaces.xml file
     */
    public File createIdSpaces(File pRoot)
            throws IOException {
        String idspaces = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE id-spaces SYSTEM \"http://www.atg.com/dtds/idgen/idgenerator_1.0.dtd\"><id-spaces><id-space name=\"__default__\" seed=\"1\" batch-size=\"100000\"/><id-space name=\"jms_msg_ids\" seed=\"0\" batch-size=\"10000\"/><id-space name=\"layer\" seed=\"0\" batch-size=\"100\"/></id-spaces>";
        File idspacesFile = new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service/idspaces.xml"
        );
        if ( idspacesFile.exists() ) {
            idspacesFile.delete();
        }
        idspacesFile.getParentFile().mkdirs();
        idspacesFile.createNewFile();
        FileWriter out = new FileWriter(idspacesFile);
        try {
            out.write(idspaces);
            out.write("\n");
        } catch ( IOException e ) {
            e.printStackTrace();

        } finally {
            out.flush();
            out.close();
        }
        return idspacesFile;
    }

    // ---------------------------------

    /**
     * Creates Nucleus' Nucleus.properties
     */
    public File createNucleus(File pRoot)
            throws IOException {
        Properties prop = new Properties();
        prop.put("initialServiceName", "/Initial");
        return NucleusTestUtils.createProperties("Nucleus", pRoot, "atg.nucleus.Nucleus", prop);
    }


    //---------------------------
    public static File createFakeXADataSource(File pRoot, Properties pJDBCProperties, String pName)
            throws IOException {
    /*---- #
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/service/jdbc/FakeXADataSource.properties#3
     *          $$Change: 410369 $
     *          #-------------------------------------------------------------------
     *          #------------------------------------------------------------------- #
     *          This is a non-XA DataSource that creates simulated
     *          XAConnections. # It is useful when a true XADataSource cannot be
     *          obtained. Note that # the behaviour of the Connections will not
     *          be that of normal # XAConnections, i.e. they will not be able to
     *          participate in # two-phase commits in the true two-phase commit
     *          style.
     *          #-------------------------------------------------------------------
     * 
     * $class=atg.service.jdbc.FakeXADataSource
     * 
     * driver=solid.jdbc.SolidDriver URL=jdbc:solid://localhost:1313 user=admin
     * password=admin
     *  
     */
        String name = pName;
        if ( name == null ) {
            name = "FakeXADataSource";
        }
        pJDBCProperties.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        return NucleusTestUtils.createProperties(
                name, new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service/jdbc"
        ), "atg.service.jdbc.FakeXADataSource", pJDBCProperties
        );
    }

    // ---------------------------------

    /**
     * @param pRoot
     *
     * @throws IOException
     */
    public static File createJTDataSource(File pRoot)
            throws IOException {
        return createJTDataSource(pRoot, null, null);
    }

    // ------------------------------------

    /**
     * Creates a new JTDataSource component. The name of the component may
     * be specified by passing in a non null value for pName.
     * Also the name of the FakeXADataSource may be specified by passing in a non null name.
     * Otherwise the defaults are JTDataSource and FakeXADataSource.
     *
     * @param pRoot
     * @param pName
     * @param pFakeXAName
     *
     * @return
     * @throws IOException
     */
    public static File createJTDataSource(File pRoot, String pName, String pFakeXAName)
            throws IOException {
    /*
     * ---- #
     * 
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/service/jdbc/JTDataSource.properties#3
     *          $$Change: 410369 $
     *          #-------------------------------------------------------------------
     *          #------------------------------------------------------------------- #
     *          This is a pooling DataSource that creates Connections registered #
     *          with the calling threads current Transaction. It must always be #
     *          given a TransactionManager and an XADataSource.
     *          #-------------------------------------------------------------------
     * 
     * $class=atg.service.jdbc.MonitoredDataSource # only use this data source
     * if you do not have an JDBC driver # which provides true XA data sources
     * dataSource=/atg/dynamo/service/jdbc/FakeXADataSource # Minimum and
     * maximum number of connections to keep in the pool min=10 max=10
     * blocking=true
     * 
     * #maxBlockTime= #maxCreateTime= #maxCreateAttempts= # # This will log any
     * SQLWarnings that are generated. By default, we turn # these off since
     * they tend to be informational, not really warnings. If # you want the
     * full traceback for where these messages are generated, # set
     * loggingWarning to true. # loggingSQLWarning=false # # The monitored
     * connection by default logs all sql through the log info # path. #
     * loggingSQLInfo=false
     */
        String name = pName;
        if ( name == null ) {
            name = "JTDataSource";
        }

        String fakeXAName = pFakeXAName;
        if ( fakeXAName == null ) {
            fakeXAName = "FakeXADataSource";
        }

        Properties props = new Properties();
        props.put("dataSource", "/atg/dynamo/service/jdbc/" + fakeXAName);
        props.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        props.put("min", "10");
        props.put("max", "20");
        props.put("blocking", "true");
        props.put("loggingSQLWarning", "false");
        props.put("loggingSQLInfo", "false");
        props.put("loggingSQLDebug", "false");


        return NucleusTestUtils.createProperties(
                name, new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service/jdbc"
        ), "atg.service.jdbc.MonitoredDataSource", props
        );
    }

    // ---------------------------------

    /**
     * Creates a SQLRepositoryEventServer
     *
     * @param pRoot
     *
     * @return
     */
    public File createSQLRepositoryEventServer(File pRoot)
            throws IOException {

        Properties prop = new Properties();
        prop.put("handlerCount", "0");
        return NucleusTestUtils.createProperties(
                "SQLRepositoryEventServer",
                new File(pRoot.getAbsolutePath() + "/atg/dynamo/server"),
                "atg.adapter.gsa.event.GSAEventServer",
                prop
        );
    }

    // ---------------------------

    /**
     * Creates a ScreenLog component
     *
     * @param pRoot
     * @param pLogging TODO
     *
     * @return
     * @throws IOException
     */
    public File createScreenLog(File pRoot, boolean pLogging)
            throws IOException {
        Properties prop = new Properties();
        prop.put("cropStackTrace", "false");
        prop.put("loggingEnabled", String.valueOf(pLogging));
        return NucleusTestUtils.createProperties(
                "ScreenLog", new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service/logging"
        ), "atg.nucleus.logging.PrintStreamLogger", prop
        );
    }

    // ---------------------------

    /**
     * Creates a GLOBAL.properties
     *
     * @param pRoot
     *
     * @return
     * @throws IOException
     */
    public File createGlobal(File pRoot)
            throws IOException {
        Properties prop = new Properties();
        prop.put("logListeners", "atg/dynamo/service/logging/ScreenLog");
        prop.put("loggingDebug", "false");
        return NucleusTestUtils.createProperties(
                "GLOBAL", new File(
                pRoot.getAbsolutePath() + "/"
        ), null, prop
        );

    }

    // ---------------------------------

    /**
     * @param pRoot
     *
     * @throws IOException
     */
    public File createClientLockManager(File pRoot)
            throws IOException {
    /*
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/service/ClientLockManager.properties#3
     *          $$Change: 410369 $
     *          $class=atg.service.lockmanager.ClientLockManager
     *          lockServerAddress=localhost lockServerPort=9010
     *          useLockServer=false
     */
        Properties props = new Properties();
        props.put("lockServerAddress", "localhost");
        props.put("lockServerPort", "9010");
        props.put("useLockServer", "false");
        return NucleusTestUtils.createProperties(
                "ClientLockManager", new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service"
        ), "atg.service.lockmanager.ClientLockManager", props
        );
    }
    // ---------------------------------

    /**
     * @param pRoot
     *
     * @throws IOException
     */
    public File createIdGenerator(File pRoot)
            throws IOException {
    /*
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/service/IdGenerator.properties#3
     *          $$Change: 410369 $
     *          #-------------------------------------------------------------------
     *          #------------------------------------------------------------------- #
     *          Default id generator service. This service generates ids using
     *          an # SQL database table. The ids are suitable for use with
     *          persistent # objects.
     *          #-------------------------------------------------------------------
     * 
     * $class=atg.service.idgen.SQLIdGenerator
     * 
     * dataSource=/atg/dynamo/service/jdbc/JTDataSource
     * transactionManager=/atg/dynamo/transaction/TransactionManager
     * XMLToolsFactory=/atg/dynamo/service/xml/XMLToolsFactory # all properties
     * of type XMLFile *MUST* use an absolute # component path. Applications
     * should append generally # append to this property.
     * initialIdSpaces=/atg/dynamo/service/idspaces.xml
     * 
     * ---- #
     */
        Properties props = new Properties();
        props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
        props.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
        // props.put("initialIdSpaces", "/atg/dynamo/service/idspaces.xml ");
        return NucleusTestUtils.createProperties(
                "IdGenerator", new File(
                pRoot.getAbsolutePath() + "/atg/dynamo/service/"
        ), "atg.service.idgen.SQLIdGenerator", props
        );
    }
    // ---------------------------------

    /**
     * @param pRoot
     *
     * @throws IOException
     */
    public File createXMLToolsFactory(File pRoot)
            throws IOException {
    /*
     * ---- #
     * 
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/service/xml/XMLToolsFactory.properties#3
     *          $$Change: 410369 $
     *          $class=atg.xml.tools.apache.ApacheXMLToolsFactory $scope=global
     *          parserFeatures=
     * 
     * ---- #
     */
        File root = new File(pRoot.getAbsolutePath() + "/atg/dynamo/service/xml");
        return NucleusTestUtils.createProperties(
                "XMLToolsFactory",
                root,
                "atg.xml.tools.apache.ApacheXMLToolsFactory",
                new Properties()
        );
    }
    // ---------------------------------

    /**
     * @param pRoot
     *
     * @throws IOException
     */
    public File createTransactionManager(File pRoot)
            throws IOException {
    /*
     * *
     * 
     * @version $Id:
     *          //product/DAS/main/templates/DAS/config/config/atg/dynamo/transaction/TransactionManager.properties#3
     *          $$Change: 410369 $ ############################## # # The Dynamo
     *          implementation of javax.transaction.TransactionManager #
     * 
     * $class=atg.dtm.TransactionManagerImpl
     */
        Properties props = new Properties();
        props.put("loggingDebug", "false");
        File root = new File(pRoot, "/atg/dynamo/transaction");
        root.mkdirs();
        NucleusTestUtils.createProperties(
                "TransactionDemarcationLogging",
                root,
                "atg.dtm.TransactionDemarcationLogging",
                props
        );

        return NucleusTestUtils.createProperties(
                "TransactionManager", root, "atg.dtm.TransactionManagerImpl", props
        );
    }
    // ---------------------------------

    /**
     * Creates the UserTransaction component
     */
    public File createUserTransaction(File pRoot)
            throws IOException {
        Properties props = new Properties();
        props.put("transactionManager", "/atg/dynamo/transaction/TransactionManager");
        return NucleusTestUtils.createProperties(
                "UserTransaction",
                new File(pRoot, "/atg/dynamo/transaction"),
                "atg.dtm.UserTransactionImpl",
                props
        );
    }

    // ---------------------------------

    /**
     * Creates a .properties file for the given repository.
     * The actual repository implementation is a
     * <code>atg.adapter.gsa.InitializingGSA</code> class.
     * This implementation is used instead because it has the ability
     * to create tables and import data before the repository starts.
     *
     * @param pRoot
     * @param pRepositoryPath
     * @param pDefinitionFiles
     * @param pCreateSQLAbsolutePath
     * @param pDropSQLAbsolutePath
     * @param pImportFiles
     *
     * @throws IOException
     */
    public File createRepositoryPropertiesFile(File pRoot,
                                               String pRepositoryPath,
                                               String[] pDefinitionFiles,
                                               String pCreateSQLAbsolutePath,
                                               String pDropSQLAbsolutePath,
                                               String[] pImportFiles)
            throws IOException {
        return createRepositoryPropertiesFile(
                pRoot,
                pRepositoryPath,
                pDefinitionFiles,
                pCreateSQLAbsolutePath,
                pDropSQLAbsolutePath,
                pImportFiles,
                null
        );
    }
    // ---------------------------------

    /**
     * Creates a .properties file for the given repository.
     * The actual repository implementation is a
     * <code>atg.adapter.gsa.InitializingGSA</code> class.
     * This implementation is used instead because it has the ability
     * to create tables and import data before the repository starts.
     *
     * @param pRoot
     * @param pRepositoryPath
     * @param pDefinitionFiles
     * @param pCreateSQLAbsolutePath
     * @param pDropSQLAbsolutePath
     * @param pImportFiles
     *
     * @throws IOException
     */
    public File createRepositoryPropertiesFile(File pRoot,
                                               String pRepositoryPath,
                                               String[] pDefinitionFiles,
                                               String pCreateSQLAbsolutePath,
                                               String pDropSQLAbsolutePath,
                                               String[] pImportFiles,
                                               String pJTDataSourceName)
            throws IOException {
    /*
     * #
     * 
     * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/GSATestUtils.java#28 $$Change: 410369 $ $class=atg.adapter.gsa.GSARepository
     * 
     * repositoryName=RefRepository definitionFiles=/atg/repository/lv/ref.xml
     * XMLToolsFactory=/atg/dynamo/service/xml/XMLToolsFactory
     * transactionManager=/atg/dynamo/transaction/TransactionManager
     * idGenerator=/atg/dynamo/service/IdGenerator
     * dataSource=/atg/dynamo/service/jdbc/JTDataSource
     * lockManager=/atg/dynamo/service/ClientLockManager
     * groupContainerPath=/atg/registry/RepositoryGroups
     * useSetUnicodeStream=true ---- #
     */
        //    String clazz = "atg.adapter.gsa.GSARepository";
        String clazz = sClassName;
        if ( mVersioned ) {
            clazz = sVersionedClassName;
        }

        Properties props = new Properties();
        props.put("repositoryName", "TestRepository" + System.currentTimeMillis());
        StringBuffer definitionFiles = new StringBuffer();
        for ( int i = 0; i < pDefinitionFiles.length; i++ ) {
            Object obj = this.getClass().getClassLoader().getResource(pDefinitionFiles[i]);
            if ( obj != null ) {
                log.debug(
                        "Repository definition file "
                        + pDefinitionFiles[i]
                        + " Does not exist in configpath. But it does in classpath. Copying over to configpath"
                );
                copyToConfigpath(pRoot, pDefinitionFiles[i]);
            } else if ( obj == null && !new File(pRoot, pDefinitionFiles[i]).exists() ) {
                throw new AssertionError(
                        "ERROR: Repository definition file "
                        + pDefinitionFiles[i]
                        + "  not found in classpath or configpath: "
                        + pRoot
                );
            }

            definitionFiles.append("/" + pDefinitionFiles[i]);
            if ( i < (pDefinitionFiles.length - 1) ) {
                definitionFiles.append(",");
            }
        }
        props.put("definitionFiles", definitionFiles.toString());
        if ( pImportFiles != null ) {
            StringBuffer importFiles = new StringBuffer();
            for ( int i = 0; i < pImportFiles.length; i++ ) {
                Object obj = this.getClass().getClassLoader().getResource(
                        pDefinitionFiles[i]
                );
                if ( obj != null ) {
                    System.out.println(
                            "DEBUG: import file "
                            + pDefinitionFiles[i]
                            + " Does not exist in configpath. But it does in classpath. Copying over to configpath"
                    );
                    copyToConfigpath(pRoot, pImportFiles[i]);
                } else if ( obj == null && !new File(pRoot, pImportFiles[i]).exists() ) {
                    throw new AssertionError(
                            "ERROR: Repository definition file "
                            + pDefinitionFiles[i]
                            + "  not found in classpath or configpath."
                    );
                }

                importFiles.append(new File(pRoot, pImportFiles[i]).getAbsolutePath());
                if ( i < (pImportFiles.length - 1) ) {
                    importFiles.append(",");
                }
            }
            props.put("importFiles", importFiles.toString());
            props.put("importEveryStartup", "true");
        }
        props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
        props.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        props.put("idGenerator", "/atg/dynamo/service/IdGenerator");
        if ( pJTDataSourceName == null ) {
            props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
        } else {
            props.put("dataSource", "/atg/dynamo/service/jdbc/" + pJTDataSourceName);
        }
        props.put("lockManager", "/atg/dynamo/service/ClientLockManager");
        props.put("idspaces", "/atg/dynamo/service/idspaces.xml");
        props.put("groupContainerPath", "/atg/registry/RepositoryGroups");
        props.put("restartingAfterTableCreation", "false");
        props.put("createTables", "true");
        props.put("loggingError", "true");
        if ( pCreateSQLAbsolutePath != null ) {
            props.put("sqlCreateFiles", "default=" + pCreateSQLAbsolutePath);
        }

        if ( pDropSQLAbsolutePath != null ) {
            props.put("sqlDropFiles", "default=" + pDropSQLAbsolutePath);
        }
        props.put("loggingDebug", "false");
        props.put("loggingCreateTables", "false");
        //    props.put("debugLevel", "7");

        // InitializingGSA specific properties
        props.put("dropTablesIfExist", "true");
        props.put("dropTablesAtShutdown", "false");
        props.put("stripReferences", "true");
        int endIndex = pRepositoryPath.lastIndexOf("/");
        String repositoryDir = pRepositoryPath.substring(0, endIndex);
        String repositoryName = pRepositoryPath.substring(
                endIndex + 1, pRepositoryPath.length()
        );
        File root = new File(pRoot, repositoryDir);
        root.mkdirs();

        if ( mVersioned ) {
            props.putAll(additionalVersionProperties(pRoot, pRepositoryPath, repositoryName));
        }
        return NucleusTestUtils.createProperties(repositoryName, root, clazz, props);
    }

    /**
     * @param pRoot
     * @param pRepositoryPath
     * @param pRepositoryName
     *
     * @return
     * @throws IOException
     */
    protected Map<String, String> additionalVersionProperties(File pRoot,
                                                              String pRepositoryPath,
                                                              String pRepositoryName)
            throws IOException {

        copyToConfigpath(pRoot, "atg/adapter/version/versionmanager/versionManagerRepository.xml");
        GSATestUtils.getGSATestUtils().createRepositoryPropertiesFile(
                pRoot,
                "/atg/adapter/version/versionmanager/VersionManagerRepository",
                new String[] { "/atg/adapter/version/versionmanager/versionManagerRepository.xml" },
                null,
                null,
                null
        );
        // create the AssetFactory
        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/AssetFactory",
                pRoot,
                "atg.versionmanager.impl.AssetFactoryRepositoryImpl",
                null
        );
        // create the AssetVersionFactory
        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/AssetVersionFactory",
                pRoot,
                "atg.versionmanager.impl.AssetVersionFactoryRepositoryImpl",
                null
        );

        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/BranchFactory",
                pRoot,
                "atg.versionmanager.impl.BranchFactoryRepositoryImpl",
                null
        );

        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/DevelopmentLineFactory",
                pRoot,
                "atg.versionmanager.impl.DevelopmentLineFactoryRepositoryImpl",
                null
        );

        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/SnapshotFactory",
                pRoot,
                "atg.versionmanager.impl.SnapshotFactoryRepositoryImpl",
                null
        );

        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/WorkspaceFactory",
                pRoot,
                "atg.versionmanager.impl.WorkspaceFactoryRepositoryImpl",
                null
        );
        Properties props = new Properties();

        props.put("assetFactory", "AssetFactory");
        props.put("repository", "VersionManagerRepository");
        props.put("branchFactory", "BranchFactory");
        props.put("developmentLineFactory", "DevelopmentLineFactory");
        props.put("snapshotFactory", "SnapshotFactory");
        props.put("assetVersionFactory", "AssetVersionFactory");
        props.put("workspaceFactory", "WorkspaceFactory");
        props.put("versionedRepositories", pRepositoryName + "=" + pRepositoryPath);
        props.put("sendCheckinEvents", "false");
        props.put("clientLockManager", "/atg/dynamo/service/ClientLockManager");

        NucleusTestUtils.createProperties(
                "/atg/adapter/version/versionmanager/VersionManagerService",
                pRoot,
                "atg.versionmanager.VersionManager",
                props
        );

        //create a version manager and version manager repository
        //createVersionManager();
        Map<String, String> extraProps = new HashMap<String, String>();
        props.put("versionManager", "/atg/adapter/version/versionmanager/VersionManagerService");
        props.put("versionItemsByDefault", "true");
        return extraProps;
    }

    /**
     * @param pConfigRoot
     * @param  pString
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void copyToConfigpath(File pConfigRoot, String pString)
            throws FileNotFoundException, IOException {
        copyToConfigpath(pConfigRoot, pString, null);
    }

    /**
     * @param pConfigRoot
     * @param pString
     * @param configPath where in config path the file must be copied.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void copyToConfigpath(final File pConfigRoot, final String path, String configPath)
            throws FileNotFoundException, IOException {
        // create the version manager repository
        pConfigRoot.mkdirs();
        if ( configPath == null ) {
            configPath = path.substring(0, path.lastIndexOf('/'));
        }
        File dir = new File(pConfigRoot, configPath);
        dir.mkdirs();
        File prop = new File(dir, path.substring(path.lastIndexOf('/')));
        if ( prop.exists() ) {
            prop.delete();
        }
        prop.createNewFile();
        OutputStream os = new FileOutputStream(prop);
        InputStream dataStream = this.getClass().getClassLoader().getResourceAsStream(path);
        while ( dataStream.available() != 0 ) {
            byte[] buff = new byte[1024];
            int available = dataStream.available();
            dataStream.read(buff);
            os.write(buff, 0, available >= 1024 ? 1024 : available);
        }
        os.flush();
    }

    //---------------------------------

    /**
     * Creates a .properties file for the given repository.
     * The actual repository implementation is a
     * <code>atg.adapter.gsa.InitializingGSA</code> class.
     * This implementation is used instead because it has the ability
     * to create tables and import data before the repository starts.
     *
     * @param pRoot
     * @param pRepositoryPath
     * @param pDefinitionFiles
     *
     * @throws IOException
     */
    public File createVersionRepositoryPropertiesFile(File pRoot,
                                                      String pRepositoryPath,
                                                      String[] pDefinitionFiles)
            throws IOException {
    /*
     * #
     * 
     * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/GSATestUtils.java#28 $$Change: 410369 $ $class=atg.adapter.gsa.GSARepository
     * 
     * repositoryName=RefRepository definitionFiles=/atg/repository/lv/ref.xml
     * XMLToolsFactory=/atg/dynamo/service/xml/XMLToolsFactory
     * transactionManager=/atg/dynamo/transaction/TransactionManager
     * idGenerator=/atg/dynamo/service/IdGenerator
     * dataSource=/atg/dynamo/service/jdbc/JTDataSource
     * lockManager=/atg/dynamo/service/ClientLockManager
     * groupContainerPath=/atg/registry/RepositoryGroups
     * useSetUnicodeStream=true ---- #
     */
        //    String clazz = "atg.adapter.gsa.GSARepository";
        String clazz = "atg.adapter.gsa.InitializingVersionRepository";
        Properties props = new Properties();
        props.put("repositoryName", "TestRepository");
        StringBuffer definitionFiles = new StringBuffer();
        for ( int i = 0; i < pDefinitionFiles.length; i++ ) {
            definitionFiles.append(pDefinitionFiles[i]);
            if ( i < (pDefinitionFiles.length - 1) ) {
                definitionFiles.append(",");
            }
        }
        props.put("definitionFiles", definitionFiles.toString());
        props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
        props.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        props.put("idGenerator", "/atg/dynamo/service/IdGenerator");
        props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
        props.put("lockManager", "/atg/dynamo/service/ClientLockManager");
        props.put("groupContainerPath", "/atg/registry/RepositoryGroups");
        props.put("versionManager", "/atg/test/version/VersionManager");
        props.put("versionItemByDefault", "true");
        props.put("loggingDebug", "true");
        //    props.put("debugLevel", "7");

        // InitializingGSA specific properties
        props.put("dropTablesIfExist", "true");
        props.put("dropTablesAtShutdown", "false");
        props.put("stripReferences", "true");
        int endIndex = pRepositoryPath.lastIndexOf("/");
        String repositoryDir = pRepositoryPath.substring(0, endIndex);
        String repositoryName = pRepositoryPath.substring(
                endIndex + 1, pRepositoryPath.length()
        );
        File root = new File(pRoot, repositoryDir);
        root.mkdirs();

        return NucleusTestUtils.createProperties(repositoryName, root, clazz, props);
    }

    // ---------------------------------

    /**
     * Returns all the tables names used for the given repository.
     */
    public String[] getTableNames(GSARepository pRepository)
            throws Exception {
        ArrayList<String> names = new ArrayList<String>();
        String[] descriptorNames = pRepository.getItemDescriptorNames();

        GSAItemDescriptor itemDescriptors[];

        int i, length = descriptorNames.length;

        itemDescriptors = new GSAItemDescriptor[length];
        for ( i = 0; i < length; i++ ) {
            itemDescriptors[i] = (GSAItemDescriptor) pRepository.getItemDescriptor(descriptorNames[i]);
        }

        //  String create = null;
        //   String index = null;
        for ( i = 0; i < length; i++ ) {
            GSAItemDescriptor desc = itemDescriptors[i];
            Table[] tables = desc.getTables();
            if ( tables != null ) {
                for ( int j = 0; j < tables.length; j++ ) {
                    String name = tables[j].getName();
                    names.add(name);
                }
            }
        }
        return (String[]) names.toArray(new String[0]);
    }
    // ---------------------------------

    /**
     * Given a repository, and the DBUtils object used to create the connection for that
     * Repository, this method asserts that all the tables are empty
     *
     * @param dbTwo
     * @param storeRepository
     *
     * @throws Exception
     * @throws SQLException
     */
    public void assertEmptyRepository(DBUtils dbTwo, GSARepository storeRepository)
            throws Exception, SQLException {
        String[] namesAfter = getTableNames(storeRepository);
        for ( int i = 0; i < namesAfter.length; i++ ) {
            log.info(namesAfter[i] + ":" + dbTwo.getRowCount(namesAfter[i]));
            Assert.assertEquals(0, dbTwo.getRowCount(namesAfter[i]));
        }
    }
    // ---------------------------------

    /**
     * Creates the das_id_generator tables using the given database
     *
     * @param db
     *
     * @throws SQLException
     */
    public void createIDGeneratorTables(DBUtils db)
            throws SQLException {
        try {
            if ( !db.isDB2() ) {
                db.update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                        + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                        + " suffix  varchar(10)     null, primary key (id_space_name)) "
                );
            } else {
                db.update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                        + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)  default   null,"
                        + " suffix  varchar(10)   default  null, primary key (id_space_name)) "
                );
            }
        } catch ( SQLException e ) {
            // drop and try again
            log.info("DROPPING DAS_ID_GENERATOR");
            db.update("drop table das_id_generator");
            if ( !db.isDB2() ) {
                db.update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                        + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                        + " suffix  varchar(10)     null, primary key (id_space_name)) "
                );
            } else {
                db.update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                        + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)  default   null,"
                        + " suffix  varchar(10)   default  null, primary key (id_space_name)) "
                );
            }


        }
    }

    /**
     * @param pVerRep
     * @param pImportFiles
     * @param pWorkspaceName
     * @param pDoWithoutTransaction
     * @param pWorkspaceComment
     *
     * @throws VersionException
     */
    public static void importFiles(VersionRepository pVerRep,
                                   String[] pImportFiles,
                                   String pWorkspaceName,
                                   boolean pDoWithoutTransaction,
                                   String pWorkspaceComment,
                                   boolean pCheckin)
            throws VersionException {
        VersionManager vm = pVerRep.getVersionManager();
        Workspace ws = vm.getWorkspaceByName(pWorkspaceName);
        if ( ws == null ) {
            throw new IllegalArgumentException("No such workspace " + pWorkspaceName);
        }

        if ( TemplateParser.importFiles(
                pVerRep, pImportFiles, new PrintWriter(
                System.out
        ), pDoWithoutTransaction, VersioningContextUtil.createVersioningContext(
                ws.getName(), pWorkspaceComment, pCheckin
        )
        ) != 0 ) {
            throw new AssertionError("Versioned import failed");
        }
    }

    /**
     * @return
     */
    public static GSATestUtils getGSATestUtils() {
        if ( SINGLETON_DEFAULT == null ) {
            SINGLETON_DEFAULT = new GSATestUtils(false);
        }
        return SINGLETON_DEFAULT;
    }

    /**
     * @return
     */
    public static GSATestUtils getVersionedGSATestUtils() {
        if ( SINGLETON_VERSIONED == null ) {
            SINGLETON_VERSIONED = new GSATestUtils(true);
        }
        return SINGLETON_VERSIONED;

    }


    /**
     * Dump all the data from a table to the console
     *
     * @param pTable
     *
     * @throws SQLException
     */
    public static void dumpTable(Table pTable, Collection<String> pPrintColumnNames)
            throws SQLException {
        GSARepository gsa = pTable.getItemDescriptor().getGSARepository();
        Connection c = null;
        PreparedStatement st = null;
        try {
            c = gsa.getConnection();
            pTable.loadColumnInfo(c);
            Collection<?> colNames = pPrintColumnNames;
            Map<?, ?> map = ((Map<?, ?>) gsa.getColumnInfoCache().get(
                    pTable.mName
            ));
            if ( map == null ) {
                map = ((Map<?, ?>) gsa.getColumnInfoCache().get(
                        pTable.mName.toUpperCase()
                ));
            }
            if ( pPrintColumnNames.isEmpty() ) {
                colNames = map.keySet();
            }
            Iterator<?> iter0 = colNames.iterator();
            String sql = "SELECT ";
            while ( iter0.hasNext() ) {
                Object obj = iter0.next();
                if ( iter0.hasNext() ) {
                    sql += obj + ",";
                } else {
                    sql += obj;
                }
            }
            sql += " FROM " + pTable.getBaseName();
            st = c.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            System.out.print(
                    "DUMP FOR TABLE: " + pTable.getBaseName().toUpperCase() + "\n"
            );
            Iterator<?> iter1 = colNames.iterator();
            int truncateThreshold = 20;
            while ( iter1.hasNext() ) {
                String colname = (String) iter1.next();
                System.out
                      .print(colname.substring(0, colname.length() > 18 ? 18 : colname.length()));
                for ( int i = 0; i < truncateThreshold - colname.length(); i++ ) {
                    System.out.print(" ");
                }
                System.out.print(" ");
            }
            System.out.print("\n");
            while ( rs.next() ) {
                int i = 1;
                Iterator<?> iter = colNames.iterator();
                while ( iter.hasNext() ) {
                    //String columnName =  iter.next();
                    iter.next();
                    Object obj = rs.getObject(i++);
                    if ( obj == null ) {
                        obj = "NULL";
                    }
                    System.out.print(
                            obj.toString().substring(
                                    0,
                                    obj.toString().length()
                                    > truncateThreshold ? truncateThreshold : obj.toString()
                                                                                 .length()
                            )
                    );
                    for ( int j = 0; j < truncateThreshold - obj.toString().length(); j++ ) {
                        System.out.print(" ");
                    }
                    System.out.print(" ");
                    //            for (int j = 0; j < columnName.length() - obj.toString().length(); j++) {
                    //              System.out.print(" ");
                    //            }
                    //          System.out.print(obj.toString().substring(0, (obj.toString().length() > columnName.length()+18 ? columnName.length(): obj.toString().length()) ) + "        ");
                }
            }
        } finally {
            if ( st != null ) {
                st.close();
            }
        }
    }

    /**
     * @param pRepository
     * @param pItemDescriptorName
     */
    public static void dumpTables(GSARepository pRepository, String pItemDescriptorName)
            throws RepositoryException, SQLException {
        GSAItemDescriptor itemdesc = (GSAItemDescriptor) pRepository.getItemDescriptor(
                pItemDescriptorName
        );
        Table[] tables = itemdesc.getTables();
        HashSet<String> doneTables = new HashSet<String>();
        for ( int i = 0; tables != null && i < tables.length; i++ ) {
            Table table = tables[i];
            if ( doneTables.contains(table.getName()) ) {
                continue;
            }
            dumpTable(table, new ArrayList<String>());
            doneTables.add(table.getName());
        }
    }
}
