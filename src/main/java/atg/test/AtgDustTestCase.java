package atg.test;

import static atg.test.AtgDustTestCase.AtgDustSystemProperties.ATG_DUST_DROP_TABLES;
import static atg.test.AtgDustTestCase.AtgDustSystemProperties.HSQL_DB_LOCATION;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBDefaultInMemoryDBConnection;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBFileDBConnection;
import static atg.test.AtgDustTestCase.DbVendor.HSQLDBRegularDBConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.adapter.gsa.GSATestUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;

/**
 * Wrapper class to make life a bit easier when using the
 * http://atgdust.sourceforge.net test harness. Example usage can be found in
 * test.SongRepositoryTest.
 * 
 * @author robert
 */
public class AtgDustTestCase extends TestCase {

  private static final Log log = LogFactory.getLog(AtgDustTestCase.class);

  private transient File configDir;

  private transient String configPath;

  private transient DBUtils dbUtils;

  private transient Nucleus nucleus;

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
    if (nucleus != null) {
      log.info("Nucleus service stopping");
      nucleus.stopService();
      nucleus.doStopService();
      nucleus.destroy();
    }
    // log.info("Deleting: " + configDir.getAbsolutePath());
    // deleteDir(configDir);
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
      log.info("file: " + property.getAbsolutePath());
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
    return nucleus.resolveName(serviceName);
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
  public void prepareGsaTest(final File configpath,
      final String[] definitionFiles, final String repoPath) throws Exception {
    prepareGsaTest(configpath, definitionFiles, repoPath, null, null, null,
        null, null, HSQLDBDefaultInMemoryDBConnection, false);

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
  protected void prepareGsaTest(final File configpath,
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

    stopNucleus();
    tearDown();

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
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(configDir,
        repoPath, definitionFiles, props, null, null, null, true);

  }

  protected void startNucleus() {
    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = NucleusTestUtils.startNucleus(configDir);
    }
  }

  protected void stopNucleus() {
    if (nucleus != null) {
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

    log.info("configPath: " + configPath);

    for (final String service : propertyFiles) {
      log.debug("Service: " + service);

      final String serviceDir = service.substring(0, service.lastIndexOf('/'));
      final File path = new File(configDir + File.separator + serviceDir);
      if (path.exists() ? true : path.mkdirs()) {
        log.debug("Created: " + path.getPath());
      }
      else {
        log.error("unable to create: " + path.getPath());
      }

      copyFile(configPath + File.separator
          + service.replace("/", File.separator) + ".properties", configDir
          .getPath()
          + service + ".properties".replaceAll("/", File.separator));

    }
  }

  private void copyFile(final String src, final String dst) {
    try {
      log.info("Source: " + src);
      log.info("Dest: " + dst);
      final FileChannel srcChannel = new FileInputStream(src).getChannel();
      final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
      dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
      srcChannel.close();
      dstChannel.close();
    }
    catch (IOException e) {
      log.error("Error: ", e);
    }

  }

  // Delete all files and sub directories under dir.
  // Returns true if all deletions were successful.
  // If a deletion fails, the method stops attempting to delete and returns
  // false.
  @SuppressWarnings("unused")
  private boolean deleteDir(final File dir) {
    if (dir.isDirectory()) {
      final String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        final boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }

}
