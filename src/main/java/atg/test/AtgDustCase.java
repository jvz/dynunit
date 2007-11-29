package atg.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.Nucleus;
import atg.test.util.AtgUtil;
import atg.test.util.ConfigurationManager;
import atg.test.util.FileUtil;

/**
 * Replacement base class for {@link AtgDustTestCase}. Extend this class and
 * use the following 'pattern' whenever you want to junit test some atg
 * components:
 * <ul>
 * <li><b>Copy</b> all configuration and repository mapping files to a
 * location that will be promoted to the configuration location ({@link AtgDustCase#configurationLocation})
 * using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String[], String, String[])}</b>.
 * The destination directory will automatically be used as the configuration
 * directory.</li>
 * <li><b><i>Or: </i></b>tell {@link AtgDustCase} class where the
 * configuration location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>.
 * </li>
 * </ul>
 * Repository based tests are depended on one of the two steps previously
 * described plus:
 * <ul>
 * <li> <b>{@link AtgDustCase#prepareRepositoryTest(String[], String)}</b> for
 * testing against an default in-memory hsql database or <b>{@link AtgDustCase#prepareRepositoryTest(String[], String, Properties, boolean)}
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

  private AtgUtil atgUtil;

  private Nucleus nucleus;

  private boolean isDebug;

  /**
   * Every .properties file copied using this method will have it's scope (if
   * one is available) set to global.
   * 
   * @param srcDirs
   *          One or more directories containing needed configuration files.
   * @param dstDir
   *          where to copy the above files to. This will also be the
   *          configuration location.
   * @param excludes
   *          Directories not to include during the copy process. Use this one
   *          to speed up the test cycle considerably.
   * @throws IOException
   *           Whenever some file related error's occure.
   */
  protected void copyConfigurationFiles(final String[] srcDirs,
      final String dstDir, final String[] excludes) throws IOException {

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
        FileUtil.copyDir(srcs, dstDir, Arrays.asList(excludes));
      }

      // forcing global scope on all configurations
      for (final File file : FileUtil
          .getFileListing(getConfigurationLocation())) {
        if (file.getPath().endsWith(".properties")) {
          // find scope other than global and replace with global
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
    ConfigurationManager.createProperties(nucleusComponentPath,
        getConfigurationLocation(), nucleusComponentClass.getName(),
        new Properties());
  }

  /**
   * 
   * @return the current configured 'configuration location path'
   * @throws IOException
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
   * @param definitionFiles
   *          an {@link String[]} array with all needed repository definition
   *          files.
   * @param repoPath
   *          the nucleus component path of the repository to be tested.
   * @throws Exception
   *           if something goes wrong
   */
  protected void prepareRepositoryTest(final String[] definitionFiles,
      final String repoPath) throws Exception {

    final Properties properties = new Properties();
    properties.put("driver", "org.hsqldb.jdbcDriver");
    properties.put("url", "jdbc:hsqldb:mem:testDb");
    properties.put("user", "sa");
    properties.put("password", "");

    prepareRepositoryTest(definitionFiles, repoPath, properties, true);

  }

  /**
   * Prepares a test against an existing database.
   * 
   * @param definitionFiles
   *          An {@link String[]} array with all needed repository definition
   *          files.
   * @param repositoryPath
   *          The the repository to be tested specified as nucleus component
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
   * @param dropTable
   *          If <code>true</code> then existing tables will be dropped and
   *          re-created, if set to <code>false</code> the existing tables
   *          will be used.
   * 
   * @throws Exception
   *           if something goes wrong
   */
  protected void prepareRepositoryTest(final String[] definitionFiles,
      final String repositoryPath, final Properties connectionProperties,
      final boolean dropTable) throws Exception {

    atgUtil = new AtgUtil(connectionProperties, isDebug);
    atgUtil.initializeMinimalRepositoryConfiguration(
        getConfigurationLocation(), repositoryPath, definitionFiles,
        connectionProperties, null, null, null, null, null, dropTable);
  }

  /**
   * 
   * @param nucleusComponentPath
   *          Path to a nucleus component (e.g. /Some/Service/Impl).
   * @return Fully injected instance of the component registered under previous
   *         argument or <code>null</code> if there is an error.
   * @throws IOException
   *           If some configuration file related errors occur.
   */
  protected Object resolveNucleusComponent(final String nucleusComponentPath)
      throws IOException {
    startNucleus();
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
   * 
   * @throws IOException
   */
  private void startNucleus() throws IOException {
    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = AtgUtil.startNucleus(getConfigurationLocation());
    }
  }

  /**
   * Always make sure to call this because it will do necessary clean up
   * actions.
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (atgUtil != null) {
      atgUtil.shutdownInMemoryDbAndCloseConnections();
    }
    if (nucleus != null) {
      nucleus.doStopService();
      nucleus.stopService();
      nucleus.destroy();
    }
  }

  /**
   * @param isDebug
   *          the isDebug to set
   */
  public void setDebug(boolean isDebug) {
    this.isDebug = isDebug;
  }
}
