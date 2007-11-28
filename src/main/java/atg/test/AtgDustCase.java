package atg.test;

import static atg.test.AtgDustTestCase.AtgDustSystemProperties.ATG_DUST_DROP_TABLES;

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

import atg.adapter.gsa.GSATestUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.test.util.DBUtils;
import atg.test.util.FileUtils;

/**
 * Replacement base class for {@link AtgDustTestCase}. Extend this class and
 * use the following 'pattern' whenever you want to junit test some atg
 * components:
 * <ul>
 * <li><b>Copy</b> all config and repository mapping files to a location that
 * will be promoted to the configuration location ({@link AtgDustCase#configurationLocation})
 * using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String[], String, String[])}</b>.
 * The destination directory will automatically be used as the configuration
 * directory.</li>
 * <li><b><i>Or: </i></b>tell {@link AtgDustCase} class where the
 * configuration location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>.
 * </li>
 * </ul>
 * Repository based tests need one of the two steps descriped above plus:
 * <ul>
 * <li> <b>{@link AtgDustCase#prepareRepositoryTest(String[], String)}</b> or
 * <b>{@link AtgDustCase#prepareRepositoryTest(String[], String, Properties, boolean)}</b>.</li>
 * </ul>
 * 
 * If you need to generate some components "on the fly":
 * <ul>
 * <li><b>{@link AtgDustCase#createPropertyFile(String, String, Class)}</b></li>
 * </ul>
 * 
 * 
 * Example usage can be found in test.SongRepositoryNewTest.
 * 
 * @author robert
 */
public class AtgDustCase extends TestCase {

  private static File configurationLocation;

  // Start: needed for optimalizations

  private static String lastConfigDstDir = "";

  private static List<String> lastConfigSrcDirs = new ArrayList<String>();

  // Stop: needed for optimalizations

  private static final Log log = LogFactory.getLog(AtgDustCase.class);

  private DBUtils dbUtils;

  private Nucleus nucleus;

  /**
   * Every .propertie file copied using this method will have it's scope (if one
   * is avaiable) set to global.
   * 
   * @param srcDirs
   *          One or more directories containing needed configuration files.
   * @param dstDir
   *          where to copy the above files to. This will also be the
   *          configuration location.
   * @param excludes
   *          Which files not to include during the copy process.
   * @throws IOException
   *           Whenever some file related error's occure.
   */
  protected void copyConfigurationFiles(final String[] srcDirs,
      final String dstDir, final String[] excludes) throws IOException {

    final List<String> srcsAsList = Arrays.asList(srcDirs);
    if (lastConfigDstDir.equalsIgnoreCase(dstDir)
        && lastConfigSrcDirs.equals(srcsAsList)) {
      log.info("No need to copy configuration files or "
          + "force global scope on all configs, "
          + "because they are still the same.");

    }
    else {
      log.info("Coping configuration files and "
          + "forcing global scope on all configs");
      FileUtils.deleteDir(configurationLocation);

      // copy all files to it's destination
      for (final String srcs : srcDirs) {
        setConfigurationLocation(dstDir);
        FileUtils.copyDir(srcs, dstDir, Arrays.asList(excludes));
      }

      // forcing global scope on all configs
      for (final File file : FileUtils
          .getFileListing(getConfigurationLocation())) {
        if (file.getPath().endsWith(".properties")) {
          // find scope other than global and replace with global
          FileUtils.searchAndReplace("$scope=", "$scope=global\n", file);
        }
      }

      lastConfigDstDir = dstDir;
      lastConfigSrcDirs = srcsAsList;
    }
  }

  /**
   * @param configurationLocation
   *          The location where the property file should be created. This will
   *          also set the {@link AtgDustCase#configurationLocation}.
   * 
   * @param nucleusComponentPath
   *          the nucleus component path (e.g /Some/Service/Impl).
   * 
   * @param nucleusComponentClass
   *          the class implementation of the nucleus component specified in
   *          previous argument.
   * 
   * @throws IOException
   *           if we have some File related errors
   */
  protected final void createPropertyFile(final String configurationLocation,
      final String nucleusComponentPath, final Class<?> nucleusComponentClass)
      throws IOException {
    AtgDustCase.configurationLocation = new File(configurationLocation);
    NucleusTestUtils.createProperties(nucleusComponentPath,
        getConfigurationLocation(), nucleusComponentClass.getName(),
        new Properties()).deleteOnExit();
  }

  /**
   * 
   * @return the current configured configuration location path
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
   * Prepares a test against an default in-memory hsql db.
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
    properties.put("URL", "jdbc:hsqldb:mem:testDb");
    properties.put("user", "sa");
    properties.put("password", "");

    prepareRepositoryTest(definitionFiles, repoPath, properties, false);

  }

  /**
   * Prepares a test against an existing db.
   * 
   * @param definitionFiles
   *          an {@link String[]} array with all needed repository definition
   *          files.
   * @param repositoryPath
   *          the nucleus component path of the repository to be tested.
   * @param connectionProperties
   *          an {@link Properties} instance with the following values (in this
   *          example the properties are geared towards an mysql db):
   * 
   * <pre>
   * final Properties properties = new Properties();
   * properties.put(&quot;driver&quot;, &quot;com.mysql.jdbc.Driver&quot;);
   * properties.put(&quot;URL&quot;, &quot;jdbc:mysql://localhost:3306/someDb&quot;);
   * properties.put(&quot;user&quot;, &quot;someUserName&quot;);
   * properties.put(&quot;password&quot;, &quot;somePassword&quot;);
   * </pre>
   * 
   * @param dropTable
   *          if <code>true</code> then existing tabled will be dropped and
   *          re-created, if set to <code>false</code> the existing tables
   *          will be used.
   * 
   * @throws Exception
   *           if something goes wrong
   */
  protected void prepareRepositoryTest(final String[] definitionFiles,
      final String repositoryPath, final Properties connectionProperties,
      final boolean dropTable) throws Exception {

    // TODO: Still have to come up with something better...
    System.setProperty(ATG_DUST_DROP_TABLES.getPropertyName(), Boolean
        .toString(dropTable));

    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(
        getConfigurationLocation(), repositoryPath, definitionFiles,
        connectionProperties, null, null, null, false);

    dbUtils = new DBUtils(connectionProperties);

  }

  /**
   * 
   * @param nucleusComponentPath
   *          Path to a nucleus component (e.g. /Some/Service/Impl).
   * @return Fully injected instance of the component registered under previous
   *         argument or <code>null</code> if there is an error.
   * @throws IOException
   *           if some config file related errors occure.
   */
  protected Object resolveNucleusComponent(final String nucleusComponentPath)
      throws IOException {
    startNucleus();
    return nucleus.resolveName(nucleusComponentPath);
  }

  /**
   * Call this method to set the config location.
   * 
   * @param configurationLocation
   *          the configurationLocation to set. Most of the time this location
   *          is a directory containg all repository definition files and
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
      nucleus = NucleusTestUtils.startNucleus(getConfigurationLocation());
    }
  }

  /**
   * Always make sure to call this because it will clean up and shutdown the db
   * and the nucleus.
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (dbUtils != null) {
      dbUtils.shutdown();
    }
    if (nucleus != null) {
      nucleus.doStopService();
      nucleus.stopService();
      nucleus.destroy();
    }
    NucleusTestUtils.emptyConfigDirMap();
  }
}
