package atg.test;

import static atg.test.AtgDustTestCase.AtgDustSystemProperties.ATG_DUST_DROP_TABLES;
import static atg.test.AtgDustTestCase.AtgDustSystemProperties.HSQL_DB_LOCATION;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBDefaultInMemoryDBConnection;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBFileDBConnection;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBRegularDBConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.adapter.gsa.GSATestUtils;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.nucleus.logging.ConsoleLogListener;
import atg.test.util.DBUtils;
import atg.test.util.FileUtils;

/**
 * Base class comparable to Junit's TestCase. Extend this class and use the
 * following 'pattern' when using it:
 * <ul>
 * <li>call: {@link AtgDustTestCase#setConfigPath(String)} in combination with:
 * {@link AtgDustTestCase#createPropertyFile(String, Class)} or
 * {@link AtgDustTestCase#createPropertyFiles(Map)} or
 * {@link AtgDustTestCase#useExistingPropertyFiles(String, String[])}</li>
 * <li>and or call: call:
 * {@link AtgDustTestCase#useExistingPropertyFiles(String, String[])}</li>
 * <li>and finally call: {@link AtgDustTestCase#getService(String)}</li>
 * <li>when test has finished call: {@link AtgDustTestCase#tearDown()} for
 * cleaning up and stopping nucleus, db</li>
 * </ul>
 * Example usage can be found in test.SongRepositoryTest.
 * 
 * @author robert
 */
public class AtgDustTestCase extends TestCase {

  private static final Log log = LogFactory.getLog(AtgDustTestCase.class);

  private transient File configDir;

  private transient String configPath;

  private transient DBUtils dbUtils;

  private transient Nucleus nucleus;

  private List<String> excludes = new ArrayList<String>();

  /**
   * An enum containing frequently set System properties
   * 
   * @author robert
   * 
   */
  public static enum AtgDustSystemProperties {

    /**
     * Override Property. If this one is set to true at the beginning of your
     * test (System.setProperty(ATG_DUST_DO_NOT_DROP_TABLES.getPropertyName(),
     * "true")) then the InitializingGSA will not drop tables. This one was
     * needed (?) because never the less I configured the property file with
     * 'dropTablesIfExist=false' the tables would still be dropped because some
     * generated config file overwrote the existing one. <br/><br/> <b>This
     * should be a temporarily solution and should be fixed in future! </b>
     * 
     */
    ATG_DUST_DROP_TABLES("ATG_DUST_DROP_TABLES"),

    HSQL_DB_LOCATION(System.getProperty("java.io.tmpdir") + File.separator
        + "hsql_db");

    private final String propertyName;

    private AtgDustSystemProperties(final String propertyName) {
      this.propertyName = propertyName;
    }

    public String getPropertyName() {
      return propertyName;
    }

  }

  public static enum DbVendor {
    DB2DBConnection, HSQLDBDefaultInMemoryDBConnection, HSQLDBFileDBConnection, HSQLDBRegularDBConnection, MSSQLDBConnection, MySQLDBConnection, OracleDBConnection, SolidDBConnection, SybaseDBConnection;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (dbUtils != null) {
      dbUtils.shutdown();
    }
    stopNucleus();
    
    log.info("ConfigDir: "+configDir);
    FileUtils.deleteDir(configDir);
    NucleusTestUtils.emptyConfigDirMap();
  }

  /**
   * @param servicePath
   *          where to store the created property file for the given service,
   *          relative to the {@link AtgDustTestCase#configDir} directory, set
   *          by {@link #AtgDuster()#setConfigPath(String)}
   * @param serviceClass
   *          a class that will be resolved by
   *          {@link AtgDustTestCase#getService(String)} in a later step
   * @throws IOException
   */
  protected final void createPropertyFile(final String servicePath,
      final Class<?> serviceClass) throws IOException {
    final Map<String, Class<?>> map = new HashMap<String, Class<?>>();
    map.put(servicePath, serviceClass);
    createPropertyFiles(map);
  }

  /**
   * The same as {@link AtgDuster#createPropertyFile(String, Class)} but this
   * time uses a map with servicePath/serviceClass key/value pairs to facilitate
   * the creation of multiple config files at once
   * 
   * @param pathsAndClasses
   * @throws IOException
   */
  protected final void createPropertyFiles(
      final Map<String, Class<?>> pathsAndClasses) throws IOException {

    for (final Iterator<Entry<String, Class<?>>> it = pathsAndClasses
        .entrySet().iterator(); it.hasNext();) {
      final Entry<String, Class<?>> entry = it.next();
      final File property = NucleusTestUtils.createProperties(entry.getKey(),
          configDir, entry.getValue().getName(), new Properties());
      // log.info("created properties file: " + property.getAbsolutePath());
      property.deleteOnExit();
    }
  }

  /**
   * @param serviceName
   *          the service to lookup and to be returned as a running usable
   *          instance. Before calling this method make sure that the needed
   *          property files are generated correctly using either
   *          {@link AtgDustTestCase#createPropertyFile(String, Class)} or
   *          {@link AtgDustTestCase#createPropertyFiles(Map)}. Another option
   *          is to use existing property files by
   *          {@link AtgDustTestCase#useExistingPropertyFiles(String, String[])}
   * @return a running usable instance of the serviceName or <code>null</code>
   *         if there is an error.
   */
  protected final Object getService(final String serviceName) {
    startNucleus();
    return prepareLogService(nucleus.resolveName(serviceName));
  }

  /**
   * Will prepare a test against an default in-memory hsql db.
   * 
   * @param configpath
   *          the location of the configuration directory
   * @param definitionFiles
   *          the location of the repository definition files without trailing
   *          slash
   * @param repoPath
   *          the location of the repository in the 'nucleus'
   */
  public void prepareRepositoryTest(final File configpath,
      final String[] definitionFiles, final String repoPath) throws Exception {
    prepareRepositoryTest(configpath, definitionFiles, repoPath, null, null,
        null, null, null, HSQLDBDefaultInMemoryDBConnection, false);

  }

  /**
   * Will prepare a repository test against an existing or an on-the-fly created
   * database. See SongRepositoryTest for example usage.
   * 
   * 
   * If {@link AtgDustTestCase.DbVendor} is {@link DbVendor#HSQLDBInMemoryDB}
   * then we only need configpath, definitionFiles, repoPath and dbName. <br/>
   * 
   * If {@link AtgDustTestCase.DbVendor} is
   * {@link DbVendor#HSQLDBRegularDBConnection} then we only need the
   * configpath, definitionFiles, repoPath, dbName, host, userName and password
   * <br/>
   * 
   * If {@link AtgDustTestCase.DbVendor} is
   * {@link DbVendor#HSQLDBFileDBConnection} then we only need the configpath,
   * definitionFiles, repoPath. The database will be created in
   * {@link System#getProperty("java.io.tmpdir")} + File.separator + "hsql_db")<br/>
   * 
   * 
   * If {@link AtgDustTestCase.DbVendor} is
   * {@link DbVendor#HSQLDBDefaultInMemoryDBConnection} then we only need the
   * configpath, definitionFiles, repoPath. The database will be created in
   * memory <br/>
   * 
   * 
   * @param configpath
   *          the location of the configuration directory
   * @param definitionFiles
   *          the location of the repository definition files without trailing
   *          slash
   * @param repoPath
   *          the location of the repository in the 'nucleus'
   * @param userName
   *          the name of the schema owner. Can be null if
   *          {@link AtgDustTestCase.DbVendor} is
   *          {@link DbVendor#HSQLDBInMemoryDB} or
   *          {@link DbVendor#HSQLDBFileDBConnection} or
   *          {@link DbVendor#HSQLDBDefaultInMemoryDBConnection}
   * @param password
   *          the schema owner password. Can be null if
   *          {@link AtgDustTestCase.DbVendor} is
   *          {@link DbVendor#HSQLDBInMemoryDB} or
   *          {@link DbVendor#HSQLDBFileDBConnection} or
   *          {@link DbVendor#HSQLDBDefaultInMemoryDBConnection}
   * @param host
   *          the host of the database to use. Can be null if
   *          {@link AtgDustTestCase.DbVendor} is
   *          {@link DbVendor#HSQLDBInMemoryDB} or
   *          {@link DbVendor#HSQLDBFileDBConnection} or
   *          {@link DbVendor#HSQLDBDefaultInMemoryDBConnection}
   * @param port
   *          the port of the database to use
   * @param dbName
   *          the name of the database to use. Can be null if
   *          {@link AtgDustTestCase.DbVendor} is
   *          {@link DbVendor#HSQLDBFileDBConnection} or
   *          {@link DbVendor#HSQLDBDefaultInMemoryDBConnection}
   * @param dbVendor
   *          the vendor of the Database. Must be one of the
   *          {@link AtgDustTestCase.DbVendor} types.
   * 
   * @param dropTable
   *          if set to <code>true</code> then existing tables will be dropped
   *          and re-created if set to <code>false</code> the exiting tables
   *          will be used
   * @throws Exception
   */
  protected void prepareRepositoryTest(final File configpath,
      final String[] definitionFiles, final String repoPath,
      final String userName, final String password, final String host,
      final String port, final String dbName, final DbVendor dbVendor,
      final boolean dropTable) throws Exception {

    this.configDir = configpath;

    Properties props = null;

    switch (dbVendor) {

    case HSQLDBRegularDBConnection:
      props = DBUtils.getHSQLDBRegularDBConnection(dbName, host, userName,
          password);
      break;

    case HSQLDBFileDBConnection:
      props = DBUtils.getHSQLDBFileDBConnection(HSQL_DB_LOCATION
          .getPropertyName());
      break;

    case MSSQLDBConnection:
      props = DBUtils.getMSSQLDBConnection(host, port, dbName, userName,
          password);
      break;

    case MySQLDBConnection:
      props = DBUtils.getMySQLDBConnection(host, port, dbName, userName,
          password);
      break;

    case DB2DBConnection:
      props = DBUtils
          .getDB2DBConnection(host, port, dbName, userName, password);
      break;

    case OracleDBConnection:
      props = DBUtils.getOracleDBConnection(host, port, dbName, userName,
          password);
      break;

    case SolidDBConnection:
      props = DBUtils.getSolidDBConnection(host, port, userName, password);
      break;

    case SybaseDBConnection:
      props = DBUtils.getSybaseDBConnection(host, port, dbName, userName,
          password);
      break;

    case HSQLDBDefaultInMemoryDBConnection:
      props = DBUtils.getHSQLDBInMemoryDBConnection();
      break;

    default:
      log.error("Unsupported Database Vendor: " + dbVendor);
      break;
    }

    // TODO: Still have to come up with something better...
    System.setProperty(ATG_DUST_DROP_TABLES.getPropertyName(), Boolean
        .toString(dropTable));

    if (dbVendor == HSQLDBDefaultInMemoryDBConnection
        || dbVendor == HSQLDBFileDBConnection
        || dbVendor == HSQLDBRegularDBConnection) {

      // Start up our database
      dbUtils = new DBUtils(props.getProperty("URL"), props
          .getProperty("driver"), props.getProperty("user"), props
          .getProperty("password"));

    }

    // Setup our test configpath
    // disabled logging (last argument to false) to get rid of the double
    // logging statements
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(configDir,
        repoPath, definitionFiles, props, null, null, null, false);

    forceGlobalScopeOnAllConfigs(configDir.getPath());

  }

  protected void startNucleus() {
    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = NucleusTestUtils.startNucleus(configDir);
    }
  }

  protected void stopNucleus() {
    if (nucleus != null) {
      log.info("Nucleus service stopping");
      try {
        nucleus.stopService();
        nucleus.doStopService();
        nucleus.destroy();
      }
      catch (ServiceException e) {
        log.error("Error: ", e);
      }
    }
  }

  /**
   * @param configPath
   *          the configPath to be used. If the configuration files are located
   *          in project/config then set this value to config.
   */
  protected final void setConfigPath(final String configPath) {
    this.configPath = configPath;
    configDir = NucleusTestUtils.getConfigpath(this.getClass(), configPath);
  }

  /**
   * @param confPath
   *          the configPath to be used. If the configuration files are for
   *          example located in project/config then set this value to config
   * @param propertyFiles
   *          an {@link String[]} with file path's as {@link java.lang.String}'s
   *          with all needed properties files relative to
   *          {@link AtgDustTestCase#configDir} <br/> Example:
   *          /some/service/SomeService (Pay attention to the leading slash!)
   * @throws IOException
   */
  protected final void useExistingPropertyFiles(final String confPath,
      final String[] propertyFiles) throws IOException {

    this.configPath = confPath.replace("/", File.separator);
    configDir = NucleusTestUtils.getConfigpath(this.getClass(), configPath);

    // log.info("configPath: " + configPath);

    for (final String service : propertyFiles) {
      // log.info("Service: " + service);

      final String serviceDir = service.substring(0, service.lastIndexOf('/'));
      final File path = new File(configDir + File.separator + serviceDir);
      if (path.exists() ? true : path.mkdirs()) {
        log.info("Created: " + path.getPath());
      }
      else {
        log.error("unable to create: " + path.getPath());
      }

      FileUtils.copyFile(configPath + File.separator
          + service.replace("/", File.separator) + ".properties", configDir
          .getPath()
          + service + ".properties".replace("/", File.separator));
      forceGlobalScope(configDir.getPath() + service
          + ".properties".replace("/", File.separator));

    }
  }

  /**
   * 
   * This method should be called if you have properties in another
   * module/project and need them in the current test.
   * 
   * @param srcDir
   *          the location of existing properties that are needed for the
   *          test(s)
   * @param dstDir
   *          the destination of the 'external' properties that are needed for
   *          the test(s).
   * @param excludes
   *          a list of excludes (Example: .svn, CVS)
   * @throws IOException
   */
  public void copyFiles(final String srcDir, final String dstDir,
      final String[] excludes) throws IOException {

    if (excludes != null && excludes.length > 0) {
      this.excludes = Arrays.asList(excludes);
    }

    FileUtils.copyDir(srcDir, dstDir, this.excludes);
    forceGlobalScopeOnAllConfigs(dstDir);

  }

  private void forceGlobalScope(final String propertyFile) throws IOException {
    final File f = new File(propertyFile);
    // find all .properties in config path
    if (f.getPath().contains(".properties")) {
      // find scope other than global and replace with global
      FileUtils.searchAndReplace("$scope=request", "$scope=global\n", f);
      FileUtils.searchAndReplace("$scope=session", "$scope=global\n", f);

    }

  }

  private void forceGlobalScopeOnAllConfigs(final String configurationDirectory)
      throws IOException {

    // find all .properties in config path
    for (final File f : getFileListing(new File(configurationDirectory))) {
      if (f.getPath().contains(".properties")) {
        // find scope other than global and replace with global
        FileUtils.searchAndReplace("$scope=request", "$scope=global\n", f);
        FileUtils.searchAndReplace("$scope=session", "$scope=global\n", f);
      }

    }

  }

  protected List<File> getFileListing(File aStartingDir)
      throws FileNotFoundException {
    List<File> result = new ArrayList<File>();

    File[] filesAndDirs = aStartingDir.listFiles();
    List<File> filesDirs = Arrays.asList(filesAndDirs);
    for (File file : filesDirs) {
      result.add(file); // always add, even if directory
      if (!file.isFile()) {
        // must be a directory
        // recursive call!
        List<File> deeperList = getFileListing(file);
        result.addAll(deeperList);
      }

    }
    Collections.sort(result);
    return result;
  }

  /**
   * Will enable logging on the object that was passed in (as a method argument)
   * if it's an instance of {@link GenericService}. Automatically called by
   * {@link AtgDustTestCase#getService(String)}
   * 
   * @param service
   *          an instance of GenericService
   * 
   * @return the object that was passed in with all log levels enabled, if it's
   *         a {@link GenericService}
   */
  protected Object prepareLogService(final Object service) {
    if (service instanceof GenericService) {
      ((GenericService) service).setLoggingDebug(true);
      ((GenericService) service).setLoggingInfo(true);
      ((GenericService) service).setLoggingWarning(true);
      ((GenericService) service).setLoggingError(true);
      ((GenericService) service).addLogListener(new ConsoleLogListener());
    }
    return service;
  }

}
