/**
 * 
 */
package atg.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.adapter.gsa.GSATestUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.nucleus.servlet.NucleusServlet;

/**
 * Wrapper class to make life a bit easier when using the
 * http://atgdust.sourceforge.net test harness (which in fact is a wrapper for
 * the junit test harness). More or less modeled to spring's way of handling
 * props/application context and looking up beans. Not suited for Repository
 * based tests. For these tests use {@link atg.adapter.gsa.GSATest}}
 * 
 * @author robert
 */
public class AtgDustTestCase extends TestCase {

  public static enum DB_VENDOR {
    HSQLDBInMemoryDB, HSQLDBRegularDBConnection, HSQLDBFileDBConnection, MSSQLDBConnection, MySQLDBConnection, DB2DBConnection, OracleDBConnection, SolidDBConnection, SybaseDBConnection, HSQLDBDefaultInMemoryDBConnection;
  }

  private final Log log = LogFactory.getLog(getClass());

  private File configpath;

  private Nucleus nucleus;

  private String cpath;

  private atg.test.DBUtils db;

  /**
   * @param configPath
   *          the configPath to set. Default location is
   *          src/main/resources/config e.g. the root ATG config directory
   *          relative to the project dir.F <br/> Example: project/config then
   *          set this value to config
   */
  protected final void setConfigPath(final String cpath) {
    this.cpath = cpath;
    configpath = NucleusTestUtils.getConfigpath(this.getClass(), cpath);
  }

  /**
   * Will start the {@link Nucleus} if {@link AtgDustTestCase#configpath} is set
   * correctly
   */
  protected final void startNucleus() {
    nucleus = NucleusTestUtils.startNucleus(configpath);
  }

  /**
   * Will stop the {@link Nucleus} if {@link AtgDustTestCase#configpath} is set
   * correctly
   * 
   * @throws ServiceException
   */
  protected final void stopNucleus() throws ServiceException {
    if (nucleus != null) {
      nucleus.stopService();
    }
  }

  /**
   * @param serviceName
   *          the service to lookup and to be returned as a running usable
   *          instance. Before calling this method make sure that the needed
   *          property files are generated corretly using either
   *          {@link AtgDustTestCase#createPropertyFile(String, Class)} or
   *          {@link AtgDustTestCase#createPropertyFiles(Map)}
   * @return a running usable instance of the serviceName
   */
  protected final Object getService(final String serviceName) {
    if (nucleus == null || !nucleus.isRunning()) {
      startNucleus();
    }
    return nucleus.resolveName(serviceName);
  }

  /**
   * @param servicePath
   *          where to store the created property file for the given service,
   *          relative to the {@link AtgDustTestCase#configpath} directory, set
   *          by {@link #AtgDuster()#setConfigPath(String)}
   * @param serviceClass
   *          a class that will be resolved by
   *          {@link AtgDustTestCase#getService(String)} in a later step
   * @throws IOException
   */
  protected final void createPropertyFile(final String servicePath,
      final Class<?> serviceClass) throws IOException {
    final Map<String, Class<?>> m = new HashMap<String, Class<?>>();
    m.put(servicePath, serviceClass);
    createPropertyFiles(m);
  }

  /**
   * The same as {@link AtgDuster#createPropertyFile(String, Class)} but this
   * time uses a map with servicePath/serviceClass key/value pairs to facilitate
   * the creation of multiple config files at once
   * 
   * @param servicePathsServiceClasses
   * @throws IOException
   */
  protected final void createPropertyFiles(
      final Map<String, Class<?>> servicePathsServiceClasses)
      throws IOException {

    for (final Iterator<String> it = servicePathsServiceClasses.keySet()
        .iterator(); it.hasNext();) {
      final String path = it.next();
      final File property = NucleusTestUtils.createProperties(path, configpath,
          servicePathsServiceClasses.get(path).getName(), new Properties());
      System.out.println("file: " + property.getAbsolutePath());
      property.deleteOnExit();
    }
  }

  /**
   * @param services
   *          a {@link java.util.List} with file path's as
   *          {@link java.lang.String}'s with all needed properties files
   *          relative to {@link AtgDustTestCase#configpath} <br/> Example:
   *          /some/service/SomeService (Pay attention to the leading slash!)
   * @throws IOException
   */
  protected final void useExistingPropertyFiles(final List<String> services)
      throws IOException {

    log.info("default config path: " + cpath);
    for (final String service : services) {
      log.info("Service: " + service);

      final String serviceDir = service.substring(0, service.lastIndexOf('/'));
      log.info("Service dir: " + serviceDir);
      final File f = new File(configpath + File.separator + serviceDir);
      if (f.exists() ? true : f.mkdirs()) {
        log.info("Created: " + f.getPath());
      } else {
        log.error("unable to create: " + f.getPath());
      }

      try {
        final String src = cpath + File.separator
            + service.replace("/", File.separator) + ".properties";

        final String dst = configpath.getPath() + service
            + ".properties".replaceAll("/", File.separator);

        log.info("Source: " + src);
        log.info("Dest: " + dst);
        final FileChannel srcChannel = new FileInputStream(src).getChannel();
        final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
      } catch (IOException e) {
        log.error("Error: ", e);
      }
    }
  }

  /**
   * Will stop the embedded test database
   */
  protected void stopDb() {
    try {
      if (db != null)
        db.shutdown();
    } catch (SQLException e) {
      log.error("Error: ", e);
    }
  }

  /**
   * @param configpath
   *          the location of the configuration directory
   * @param definitionFiles
   *          the location of the repository definition files without trailing
   *          slash
   * @param repoPath
   *          the location of the repository in the 'nucleus'
   * @throws Exception
   */
  protected void prepareGsaTest(final File configpath,
      final String[] definitionFiles, final String repoPath) throws Exception {

    // just make sure that it's not running before setting up the db path's
    stopNucleus();

    // Use the DBUtils utility class to get JDBC properties for an in memory
    // HSQL DB called "testdb".
    Properties props = DBUtils.getHSQLDBInMemoryDBConnection("testdb");

    // Start up our database
    db = new DBUtils(props.getProperty("URL"), props.getProperty("driver"),
        props.getProperty("user"), props.getProperty("password"));

    // Setup our testing configpath
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(configpath,
        repoPath, definitionFiles, props, null, null, null, true);

    System.setProperty("atg.dynamo.license.read", "true");
    System.setProperty("atg.license.read", "true");
    NucleusServlet.addNamingFactoriesAndProtocolHandlers();

    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = Nucleus.startNucleus(new String[] { configpath
          .getAbsolutePath() });
    }

  }

  /**
   * Will prepare a repository test against an existing or an on-the-fly craeted
   * database. See {@link SongRepositoryTest} for example usage.
   * 
   * 
   * If {@link DB_VENDOR} is {@link DB_VENDOR#HSQLDBInMemoryDB} then we only
   * need configpath, definitionFiles, repoPath and dbName. <br/>
   * 
   * If {@link DB_VENDOR} is {@link DB_VENDOR#HSQLDBRegularDBConnection} then we
   * only need the configpath, definitionFiles, repoPath, dbName, host, userName
   * and password <br/>
   * 
   * If {@link DB_VENDOR} is {@link DB_VENDOR#HSQLDBFileDBConnection} then we
   * only need the configpath, definitionFiles, repoPath. The database will be
   * created in {@link System#getProperty("java.io.tmpdir")} + File.separator +
   * "hsql_db")<br/>
   * 
   * 
   * If {@link DB_VENDOR} is {@link DB_VENDOR#HSQLDBDefaultInMemoryDBConnection}
   * then we only need the configpath, definitionFiles, repoPath. The database
   * will be created in memory <br/>
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
   *          the name of the schema owner. Can be null if {@link DB_VENDOR} is
   *          {@link DB_VENDOR#HSQLDBInMemoryDB} or
   *          {@link DB_VENDOR#HSQLDBFileDBConnection} or
   *          {@link DB_VENDOR#HSQLDBDefaultInMemoryDBConnection}
   * @param password
   *          the schema owner password. Can be null if {@link DB_VENDOR} is
   *          {@link DB_VENDOR#HSQLDBInMemoryDB} or
   *          {@link DB_VENDOR#HSQLDBFileDBConnection} or
   *          {@link DB_VENDOR#HSQLDBDefaultInMemoryDBConnection}
   * @param host
   *          the host of the database to use. Can be null if {@link DB_VENDOR}
   *          is {@link DB_VENDOR#HSQLDBInMemoryDB} or
   *          {@link DB_VENDOR#HSQLDBFileDBConnection} or
   *          {@link DB_VENDOR#HSQLDBDefaultInMemoryDBConnection}
   * @param port
   *          the port of the database to use
   * @param dbName
   *          the name of the database to use. Can be null if {@link DB_VENDOR}
   *          is {@link DB_VENDOR#HSQLDBFileDBConnection} or
   *          {@link DB_VENDOR#HSQLDBDefaultInMemoryDBConnection}
   * @param dbVendor
   *          the vendor of the Database. Must be one of the {@link DB_VENDOR}
   *          types.
   * @throws Exception
   */
  protected void prepareGsaTest(final File configpath,
      final String[] definitionFiles, final String repoPath,
      final String userName, final String password, final String host,
      final String port, final String dbName, final DB_VENDOR dbVendor)
      throws Exception {

    Properties props = null;

    switch (dbVendor) {
    case HSQLDBInMemoryDB:
      props = DBUtils.getHSQLDBInMemoryDBConnection(dbName);
      break;

    case HSQLDBRegularDBConnection:
      props = DBUtils.getHSQLDBRegularDBConnection(dbName, host, userName,
          password);
      break;

    case HSQLDBFileDBConnection:
      props = DBUtils.getHSQLDBFileDBConnection(System
          .getProperty("java.io.tmpdir")
          + File.separator + "hsql_db");
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

    // Start up our database
    db = new DBUtils(props.getProperty("URL"), props.getProperty("driver"),
        props.getProperty("user"), props.getProperty("password"));

    // Setup our testing configpath
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(configpath,
        repoPath, definitionFiles, props, null, null, null, true);

    System.setProperty("atg.dynamo.license.read", "true");
    System.setProperty("atg.license.read", "true");
    NucleusServlet.addNamingFactoriesAndProtocolHandlers();

    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = Nucleus.startNucleus(new String[] { configpath
          .getAbsolutePath() });
    }

  }

  protected void cleanUp() {
    // delete old properties that are on the configpath
    // configpath.getPath()
    log.info("Deleting: " + configpath.getAbsolutePath());
    deleteDir(configpath);

  }

  // Deletes all files and subdirectories under dir.
  // Returns true if all deletions were successful.
  // If a deletion fails, the method stops attempting to delete and returns
  // false.
  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }

}
