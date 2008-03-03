package atg.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.nucleus.logging.ConsoleLogListener;
import atg.test.configuration.BasicConfiguration;
import atg.test.configuration.RepositoryConfiguration;
import atg.test.util.FileUtil;
import atg.test.util.RepositoryManager;

/**
 * Replacement base class for {@link AtgDustTestCase}. Extend this class and
 * use the following 'pattern' whenever you want to junit test some atg
 * components:
 * <ul>
 * <li><b>Copy</b> all needed configuration and repository mapping files to a
 * staging location outside of your source tree using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String[], String, String...)}</b>.
 * The staging directory will automatically be used as the configuration
 * directory. Copying all needed priorities to a location outside of the source
 * tree is the preferred method, because this frameworks creates properties on
 * the fly and that could pollute your current source tree. </li>
 * <!--
 * <li><b>
 * 
 * <i>Or: </i></b>tell {@link AtgDustCase} class where the configuration
 * location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>,
 * but be aware that the location will also be used for properties file
 * generation. </li>
 * -->
 * </ul>
 * 
 * <!-- p> <b>Rule of thumb:</b> When running repository tests, copy everything
 * outside of your source tree (or when you use maven, use the target directory ).
 * If you run basic component/formhandler tests, pointing it to your existing
 * configuration directory might be sufficient.
 * 
 * </p-->
 * 
 * Repository based tests are depended on one of the two steps previously
 * described plus:
 * <ul>
 * <li> <b>{@link AtgDustCase#prepareRepository(String, String...)}</b> for
 * testing against an default in-memory hsql database or <b>{@link AtgDustCase#prepareRepository(String, Properties, boolean, String...)}
 * </b> for testing against an existing database.</li>
 * </ul>
 * 
 * If you need to generate some components "on the fly":
 * <ul>
 * <li><b>{@link AtgDustCase#createPropertyFile(String, String, Class)}</b></li>
 * </ul>
 * 
 * <p>
 * Example usage can be found in test.SongsRepositoryTest.
 * </p>
 * 
 * <p>
 * This class overrides Junit 3 and not Junit 4 because currently Junit 4 has
 * some test runner/eclipse related bugs which makes it impossible for me to
 * use.
 * </p>
 * 
 * @author robert
 */
@SuppressWarnings("unchecked")
public class AtgDustCase extends TestCase {

  private static Logger log = Logger.getLogger(AtgDustCase.class);
  private RepositoryManager repositoryManager = new RepositoryManager();
  private final BasicConfiguration basicConfiguration = new BasicConfiguration();
  private File configurationLocation;
  private Nucleus nucleus;
  private boolean isDebug;
  private static Map<String, Long> CONFIG_FILES_TIMESTAMPS = null;

  public static final File TIMESTAMP_SER = new File(System
      .getProperty("java.io.tmpdir")
      + File.separator + "atg-dust-rh.ser");
  private static final long CONFIG_FILES_TIMESTAMPS_TTL = 1800000L;

  /**
   * Every *.properties file copied using this method will have it's scope (if
   * one is available) set to global. Most of this method is implemented in a
   * '@BeforeSuite' TestNG comparable fashion, because copying configuration
   * files around the file system is a rather expensive operation that should
   * only be done once or the moment the file-set to copy has changed (but not
   * just blindly during every single setup or test call).
   * 
   * @param srcDirs
   *          One or more directories containing needed configuration files.
   * @param dstDir
   *          where to copy the above files to. This will also be the
   *          configuration location.
   * @param excludes
   *          One or more directories not to include during the copy process.
   *          Use this one to speeds up the test cycle considerably. You can
   *          also call it with an empty {@link String[]} or <code>null</code>
   *          if nothing should be excluded
   * @throws IOException
   *           Whenever some file related error's occur.
   */
  protected final void copyConfigurationFiles(final String[] srcDirs,
      final String dstDir, final String... excludes) throws IOException {

    setConfigurationLocation(dstDir);

    if (log.isDebugEnabled()) {
      log.debug("Copying configuration files and "
          + "forcing global scope on all configs");
    }
    boolean isDirty = false;
    for (final String srcs : srcDirs) {
      // log.info("config dir: " + srcs);
      for (final File file : FileUtil.getFileListing(new File(srcs))) {
        // log.info("config file: " + file);
        if (CONFIG_FILES_TIMESTAMPS.get(file.getPath()) != null
            && file.lastModified() == CONFIG_FILES_TIMESTAMPS.get(file
                .getPath())) {
        }
        else {
          CONFIG_FILES_TIMESTAMPS.put(file.getPath(), file.lastModified());
          isDirty = true;
        }
      }
    }

    if (isDirty) {
      if (log.isDebugEnabled()) {
        log
            .debug("Config files timestamps map is dirty an will be re serialized");
      }
      ObjectOutput out = null;
      try {
        out = new ObjectOutputStream(new FileOutputStream(TIMESTAMP_SER));
        out.writeObject(CONFIG_FILES_TIMESTAMPS);
      }
      finally {
        if (out != null) {
          out.close();
        }
      }
    }

    for (final String srcs : srcDirs) {
      FileUtil.copyDirectory(srcs, dstDir, Arrays
          .asList(excludes == null ? new String[] {} : excludes));
    }

    // forcing global scope on all configurations
    for (final File file : FileUtil.getFileListing(getConfigurationLocation())) {
      if (CONFIG_FILES_TIMESTAMPS.get(file.getPath()) == null
          && file.getPath().endsWith(".properties")) {
        FileUtil.searchAndReplace("$scope=", "$scope=global\n", file);
      }
    }
  }

  /**
   * @param configurationStagingLocation
   *          The location where the property file should be created. This will
   *          also set the {@link AtgDustCase#configurationLocation}.
   * 
   * @param nucleusComponentPath
   *          Nucleus component path (e.g /Some/Service/Impl).
   * 
   * @param clazz
   *          The {@link Class} implementing the nucleus component specified in
   *          previous argument.
   * 
   * @throws IOException
   *           If we have some File related errors
   */
  protected final void createPropertyFile(
      final String configurationStagingLocation,
      final String nucleusComponentPath, final Class<?> clazz)
      throws IOException {
    this.configurationLocation = new File(configurationStagingLocation);
    FileUtil.createPropertyFile(nucleusComponentPath,
        getConfigurationLocation(), clazz.getClass(),
        new HashMap<String, String>());
  }

  /**
   * 
   * @return the current configured 'configuration location path'
   * @throws IOException
   *           If configuration location is <code>null</code> or non existing.
   */
  private File getConfigurationLocation() throws IOException {
    if (configurationLocation == null || !configurationLocation.exists()) {
      throw new FileNotFoundException(
          "No or empty configuration location is specified. Unable to continue.");
    }
    return configurationLocation;
  }

  /**
   * Prepares a test against an default in-memory hsql database.
   * 
   * @param repoPath
   *          the nucleus component path of the repository to be tested.
   * 
   * @param definitionFiles
   *          one or more repository definition files.
   * @throws IOException
   *           The moment we have some properties/configuration related error
   * @throws SQLException
   *           Whenever there is a database related error
   * 
   */
  protected final void prepareRepository(final String repoPath,
      final String... definitionFiles) throws SQLException, IOException {

    final Properties properties = new Properties();
    properties.put("driver", "org.hsqldb.jdbcDriver");
    properties.put("url", "jdbc:hsqldb:mem:testDb");
    properties.put("user", "sa");
    properties.put("password", "");

    prepareRepository(repoPath, properties, true, definitionFiles);

  }

  /**
   * Prepares a test against an existing database.
   * 
   * @param repositoryPath
   *          The the repository to be tested, specified as nucleus component
   *          path.
   * @param connectionProperties
   *          A {@link Properties} instance with the following values (in this
   *          example the properties are geared towards an mysql database):
   * 
   * <pre>
   * final Properties properties = new Properties();
   * properties.put(&quot;driver&quot;, &quot;com.mysql.jdbc.Driver&quot;);
   * properties.put(&quot;url&quot;, &quot;jdbc:mysql://localhost:3306/someDb&quot;);
   * properties.put(&quot;user&quot;, &quot;someUserName&quot;);
   * properties.put(&quot;password&quot;, &quot;somePassword&quot;);
   * </pre>
   * 
   * 
   * @param dropTables
   *          If <code>true</code> then existing tables will be dropped and
   *          re-created, if set to <code>false</code> the existing tables
   *          will be used.
   * 
   * @param definitionFiles
   *          One or more needed repository definition files.
   * @throws IOException
   *           The moment we have some properties/configuration related error
   * @throws SQLException
   *           Whenever there is a database related error
   * 
   */
  protected final void prepareRepository(final String repositoryPath,
      final Properties connectionProperties, final boolean dropTables,
      final String... definitionFiles) throws SQLException, IOException {

    final Map<String, String> connectionSettings = new HashMap<String, String>();

    for (final Iterator<Entry<Object, Object>> it = connectionProperties
        .entrySet().iterator(); it.hasNext();) {
      final Entry<Object, Object> entry = it.next();
      connectionSettings
          .put((String) entry.getKey(), (String) entry.getValue());

    }
    final RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();

    repositoryConfiguration.setDebug(isDebug);
    // get configurationLocation once by getConfigurationLocation(), after that
    // it's safe to use the variable directly.
    repositoryConfiguration
        .createPropertiesByConfigurationLocation(getConfigurationLocation());
    repositoryConfiguration.createFakeXADataSource(configurationLocation,
        connectionSettings);
    repositoryConfiguration.createRepositoryConfiguration(
        configurationLocation, repositoryPath, dropTables, definitionFiles);

    repositoryManager.initializeMinimalRepositoryConfiguration(
        getConfigurationLocation(), repositoryPath, connectionSettings,
        dropTables, isDebug, definitionFiles);
  }

  /**
   * Method for retrieving a fully injected atg component
   * 
   * @param nucleusComponentPath
   *          Path to a nucleus component (e.g. /Some/Service/Impl).
   * @return Fully injected instance of the component registered under previous
   *         argument or <code>null</code> if there is an error.
   * @throws IOException
   */
  protected Object resolveNucleusComponent(final String nucleusComponentPath)
      throws IOException {
    startNucleus(getConfigurationLocation());
    return enableLoggingOnGenericService(nucleus
        .resolveName(nucleusComponentPath));
  }

  /**
   * Call this method to set the configuration location.
   * 
   * @param configurationLocation
   *          The configuration location to set. Most of the time this location
   *          is a directory containing all repository definition files and
   *          component property files which are needed for the test.
   */
  protected final void setConfigurationLocation(
      final String configurationLocation) {
    this.configurationLocation = new File(configurationLocation);
    if (log.isDebugEnabled()) {
      log.debug("Using configuration location: "
          + this.configurationLocation.getPath());
    }
  }

  /**
   * Always make sure to call this because it will do necessary clean up actions
   * (shutting down in-memory database (if it was used) and the nucleus) so he
   * next test can run safely.
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (repositoryManager != null) {
      repositoryManager.shutdownInMemoryDbAndCloseConnections();
    }
    if (nucleus != null) {
      nucleus.doStopService();
      nucleus.stopService();
      nucleus.destroy();
    }
  }

  /**
   * Enables or disables the debug level of nucleus components.
   * 
   * @param isDebug
   *          Setting this to <code>true</code> will enable debug on all
   *          (currently only on repository related) components, setting it to
   *          <code>false</code> turn's the debug off again.
   */
  protected void setDebug(boolean isDebug) {
    this.isDebug = isDebug;
  }

  /**
   * 
   * @param configpath
   * @return
   * @throws IOException
   */
  private Nucleus startNucleus(final File configpath) throws IOException {
    if (nucleus == null || !nucleus.isRunning()) {
      ClassLoggingFactory.getFactory();
      basicConfiguration.setDebug(isDebug);
      basicConfiguration.createPropertiesByConfigurationLocation(configpath);

      System.setProperty("atg.dynamo.license.read", "true");
      System.setProperty("atg.license.read", "true");
      
      // TODO: Can I safely keep this one disabled?
      // NucleusServlet.addNamingFactoriesAndProtocolHandlers();
      
      nucleus = Nucleus.startNucleus(new String[] { configpath
          .getAbsolutePath() });
    }
    return nucleus;
  }

  /**
   * Will enable logging on the object/service that was passed in (as a method
   * argument) if it's an instance of {@link GenericService}. This method is
   * automatically called from
   * {@link AtgDustCase#resolveNucleusComponent(String)}. Debug level is
   * enabled the moment {@link AtgDustCase#setDebug(boolean)} was called with
   * <code>true</code>.
   * 
   * @param service
   *          an instance of GenericService
   * 
   * @return the GenericService instance that was passed in with all log levels
   *         enabled, if it's a {@link GenericService}
   */
  private Object enableLoggingOnGenericService(final Object service) {
    if (service instanceof GenericService) {
      ((GenericService) service).setLoggingDebug(isDebug);
      ((GenericService) service).setLoggingInfo(true);
      ((GenericService) service).setLoggingWarning(true);
      ((GenericService) service).setLoggingError(true);
      ((GenericService) service).removeLogListener(new ConsoleLogListener());
      ((GenericService) service).addLogListener(new ConsoleLogListener());
    }
    return service;
  }

  static {
    try {
      if (TIMESTAMP_SER.exists()) {
        if (TIMESTAMP_SER.lastModified() < System.currentTimeMillis() - CONFIG_FILES_TIMESTAMPS_TTL) {
          if (log.isDebugEnabled()) {
            log.debug(String.format("Deleting previous config files timestamps map "
                + "because it's older then %s m/s", CONFIG_FILES_TIMESTAMPS_TTL));
          }
          TIMESTAMP_SER.delete();
        }
        else {
          final ObjectInputStream in = new ObjectInputStream(
              new FileInputStream(TIMESTAMP_SER));
          try {
            CONFIG_FILES_TIMESTAMPS = (Map<String, Long>) in.readObject();
          }
          catch (ClassNotFoundException e) {
            log.error("Error: ", e);
          }
          finally {
            if (in != null) {
              in.close();
            }
          }
        }
      }
      if (CONFIG_FILES_TIMESTAMPS == null) {
        CONFIG_FILES_TIMESTAMPS = new HashMap<String, Long>();
      }
    }
    catch (Exception e) {
      log.error("Error: ", e);
    }
  }
}