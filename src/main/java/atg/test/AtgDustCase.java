package atg.test;

import static atg.test.AtgDustTestCase.AtgDustSystemProperties.ATG_DUST_DROP_TABLES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <ol>
 * <li><b>Always</b> copy all config and repository mapping files to a
 * location that will be promoted to the configuration location using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String, String, String[])}</b>.
 * The destination directory will automatically be used as the configuration
 * directory. <b>Always</b> point it to some temp location, because this
 * framework will gerenated needed property files and if you point it to some
 * location in your existing source tree, you will polute it with these
 * gerenerate files</li>
 * <li><b><i>Optional: </i></b>Tell {@link AtgDustCase} class where the
 * configuration location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>.
 * This is most of the time not needed because the location has already been set
 * at step 1. </li>
 * <li><b><i>Optional: </i></b> if you want to create repository based tests
 * use <b>{@link AtgDustCase#prepareRepositoryTest(String[], String)}</b> or
 * <b>{@link AtgDustCase#prepareRepositoryTest(String[], String, Properties, boolean)}</b>.</li>
 * </ol>
 * Example usage can be found in test.SongRepositoryNewTest.
 * 
 * @author robert
 */
public class AtgDustCase extends TestCase {

  private static File configurationLocation;

  // Start: needed for optimalizations
  private static boolean COPIED_CONFIGS = false;

  private static String lastConfigDstDir;

  private static List<String> lastConfigSrcDirs = new ArrayList<String>();

  private static final Log log = LogFactory.getLog(AtgDustCase.class);

  // Stop: needed for optimalizations

  private transient DBUtils dbUtils;

  private transient Nucleus nucleus;

  /**
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
    if (COPIED_CONFIGS && lastConfigDstDir.equalsIgnoreCase(dstDir)
        && lastConfigSrcDirs.equals(srcsAsList)) {
      log.info("No need to copy configuration files or "
          + "force global scope on all configs, "
          + "because they are still the same.");
      return;
    }
    log.info("Coping configuration files and "
        + "forcing global scope on all configs");
    FileUtils.deleteDir(configurationLocation);

    for (final String srcs : srcDirs) {
      setConfigurationLocation(dstDir);
      FileUtils.copyDir(srcs, dstDir, Arrays.asList(excludes));
    }
    forceGlobalScopeOnAllConfigs(configurationLocation);
    COPIED_CONFIGS = true;
    lastConfigDstDir = dstDir;
    lastConfigSrcDirs = srcsAsList;
  }

  /**
   * @param configurationLocation
   *          The location where the property file will be created. This will
   *          also set the config location directory.
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
   * @param configurationDirectory
   * @throws IOException
   */
  private void forceGlobalScopeOnAllConfigs(final File configurationDirectory)
      throws IOException {
    // find all .properties in config path
    for (final File file : getFileListing(configurationDirectory)) {
      if (file.getPath().contains(".properties")) {
        // find scope other than global and replace with global
        FileUtils.searchAndReplace("$scope=", "$scope=global\n", file);
      }
    }
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
   * 
   * @param aStartingDir
   * @return
   * @throws FileNotFoundException
   */
  private List<File> getFileListing(File aStartingDir)
      throws FileNotFoundException {
    final List<File> result = new ArrayList<File>();

    final File[] filesAndDirs = aStartingDir.listFiles();
    final List<File> filesDirs = Arrays.asList(filesAndDirs);
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
   * properties.put(&quot;&lt;b&gt;URL&lt;/b&gt;&quot;, &quot;jdbc:mysql://localhost:3306/someDb&quot;);
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
