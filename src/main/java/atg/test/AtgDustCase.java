package atg.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.Nucleus;
import atg.test.configuration.BasicConfiguration;
import atg.test.configuration.RepositoryConfiguration;
import atg.test.util.FileUtil;
import atg.test.util.RepositoryManager;

/**
 * Replacement base class for {@link AtgDustTestCase}. Extend this class and
 * use the following 'pattern' whenever you want to junit test some atg
 * components:
 * <ul>
 * <li><b>Copy</b> all configuration and repository mapping files to a
 * location that will be promoted to the configuration location ({@link AtgDustCase#configurationLocation})
 * using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String[], String, String...)}</b>.
 * The destination directory will automatically be used as the configuration
 * directory. Copying all needed priorities to a location outside of the source
 * tree is the preferred method, because this frameworks creates properties on
 * the fly and that could pollute your curent source tree. </li>
 * <li><b><i>Or: </i></b>tell {@link AtgDustCase} class where the
 * configuration location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>,
 * but be aware that the loction will also be used for properties file
 * generation. </li>
 * </ul>
 * 
 * <p>
 * <b>Rule of thumb:</b> When running repository tests, copy everything outside
 * of your source tree (or when you use maven, use the target directory ). If
 * you run basic component/formhandler tests, pointing it to your existing
 * configuration directory might be sufficient.
 * 
 * </p>
 * 
 * Repository based tests are depended on one of the two steps previously
 * described plus:
 * <ul>
 * <li> <b>{@link AtgDustCase#prepareRepositoryTest(String, String...)}</b>
 * for testing against an default in-memory hsql database or <b>{@link AtgDustCase#prepareRepositoryTest(String, Properties, boolean, String...)}
 * </b> for testing against an existing database.</li>
 * </ul>
 * 
 * If you need to generate some components "on the fly":
 * <ul>
 * <li><b>{@link AtgDustCase#createPropertyFile(String, String, Class)}</b></li>
 * </ul>
 * 
 * <p>
 * Example usage can be found in test.SongRepositoryNewTest.
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
public class AtgDustCase extends TestCase {

  private static File configurationLocation;

  // Start: needed for optimizing

  private static String lastConfigDstDir = "";

  private static List<String> lastConfigSrcDirs = new ArrayList<String>(),
      lastConfigExcludes = new ArrayList<String>();

  // Stop: needed for optimizing

  private static final Log log = LogFactory.getLog(AtgDustCase.class);

  private RepositoryManager repositoryManager = new RepositoryManager();

  private Nucleus nucleus;

  private boolean isDebug;

  private final RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();

  private final BasicConfiguration basicConfiguration = new BasicConfiguration();

  /**
   * Every *.properties file copied using this method will have it's scope (if
   * one is available) set to global. Most of this method is implemented in a
   * '@BeforeClass' Junit4 fashion because copying configuration files around
   * the file system is a rather expensive operation that should only be done
   * once or the moment the file set to copy has changed (but not just blindly
   * during every single setup or test call).
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

    final List<String> srcsAsList = Arrays.asList(srcDirs);
    final List<String> exsAsList = Arrays.asList(srcDirs);
    if (lastConfigDstDir.equalsIgnoreCase(dstDir)
        && lastConfigSrcDirs.equals(srcsAsList)
        && lastConfigExcludes.equals(exsAsList)) {
      log.info("No need to copy configuration files or "
          + "force global scope on all configs, "
          + "because they are still the same.");

    }
    else {
      log.info("Copying configuration files and "
          + "forcing global scope on all configs");
      // copy all files to it's destination
      for (final String srcs : srcDirs) {
        setConfigurationLocation(dstDir);
        FileUtil.copyDir(srcs, dstDir, Arrays
            .asList(excludes == null ? new String[] {} : excludes));
      }

      // forcing global scope on all configurations
      for (final File file : FileUtil
          .getFileListing(getConfigurationLocation())) {
        if (file.getPath().endsWith(".properties")) {
          FileUtil.searchAndReplace("$scope=", "$scope=global\n", file);
        }
      }
      lastConfigDstDir = dstDir;
      lastConfigSrcDirs = srcsAsList;
      lastConfigExcludes = exsAsList;
    }
  }

  /**
   * @param configurationLocation
   *          The location where the property file should be created. This will
   *          also set the {@link AtgDustCase#configurationLocation}.
   * 
   * @param nucleusComponentPath
   *          Nucleus component path (e.g /Some/Service/Impl).
   * 
   * @param nucleusComponentClass
   *          The implementation of the nucleus component specified in previous
   *          argument.
   * 
   * @throws IOException
   *           If we have some File related errors
   */
  protected final void createPropertyFile(final String configurationLocation,
      final String nucleusComponentPath, final Class<?> nucleusComponentClass)
      throws IOException {
    AtgDustCase.configurationLocation = new File(configurationLocation);
    FileUtil.createPropertyFile(nucleusComponentPath,
        getConfigurationLocation(), nucleusComponentClass.getName(),
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
  protected final void prepareRepositoryTest(final String repoPath,
      final String... definitionFiles) throws SQLException, IOException {

    final Properties properties = new Properties();
    properties.put("driver", "org.hsqldb.jdbcDriver");
    properties.put("url", "jdbc:hsqldb:mem:testDb");
    properties.put("user", "sa");
    properties.put("password", "");

    prepareRepositoryTest(repoPath, properties, true, definitionFiles);

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
  protected final void prepareRepositoryTest(final String repositoryPath,
      final Properties connectionProperties, final boolean dropTables,
      final String... definitionFiles) throws SQLException, IOException {

    final Map<String, String> connectionSettings = new HashMap<String, String>();

    for (final Iterator<Entry<Object, Object>> it = connectionProperties
        .entrySet().iterator(); it.hasNext();) {
      final Entry<Object, Object> entry = it.next();
      connectionSettings
          .put((String) entry.getKey(), (String) entry.getValue());

    }

    repositoryConfiguration.setDebug(isDebug);
    repositoryConfiguration.createPropertiesByConfigurationLocation(getConfigurationLocation());
    repositoryConfiguration.createFakeXADataSource(configurationLocation, connectionSettings);
    repositoryConfiguration.createRepositoryConfiguration(configurationLocation, repositoryPath,
        dropTables, definitionFiles);

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
   */
  protected Object resolveNucleusComponent(final String nucleusComponentPath) {
    startNucleus(configurationLocation);
    return nucleus.resolveName(nucleusComponentPath);
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
    AtgDustCase.configurationLocation = new File(configurationLocation);
    log.info("Using configuration location: "
        + AtgDustCase.configurationLocation.getPath());
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
   */
  private Nucleus startNucleus(final File configpath) {
    if (nucleus == null || !nucleus.isRunning()) {

      // TODO: Find a better place for the next call
      try {
        basicConfiguration.setDebug(isDebug);
        basicConfiguration.createPropertiesByConfigurationLocation(configpath);
      }
      catch (IOException e) {
        log.error("Error: ", e);
        return null;
      }

      System.setProperty("atg.dynamo.license.read", "true");
      System.setProperty("atg.license.read", "true");
      // TODO: Can I safely disable this one?
      // NucleusServlet.addNamingFactoriesAndProtocolHandlers();
      nucleus = Nucleus.startNucleus(new String[] { configpath
          .getAbsolutePath() });
    }
    return nucleus;
  }

}
