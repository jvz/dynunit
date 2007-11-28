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
 * Replacement base class for {@link AtgDustTestCase} comparable to Junit's
 * TestCase. Extend this class and use the following 'pattern' whenever you want
 * to junit test some atg components:
 * <ol>
 * <li>Copy all config and repository mapping files to a location that will be
 * promoted to the configuration location using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String, String, String[])}</b>.
 * The destination directory will automatically be used as the configuration
 * directory.</li>
 * <li><b><i>Optional: </i></b>Tell {@link AtgDustCase} class where the
 * configuration location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>.
 * This is most of the time not needed because the location has already been set
 * at step 1. BTW if you use this method always point it to some temp location,
 * because this framework will gerenated needed property files and if you point
 * it to some location in your existing source tree, you will polute it with
 * these gerenerate files.</li>
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

  private static boolean COPIED_CONFIGS = false;

  private static String lastConfigSrcDir, lastConfigDstDir;

  private static final Log log = LogFactory.getLog(AtgDustCase.class);

  private DBUtils dbUtils;

  private transient Nucleus nucleus;

  /**
   * 
   * @param srcDir
   * @param dstDir
   * @param excludes
   * @throws IOException
   */
  protected void copyConfigurationFiles(final String srcDir,
      final String dstDir, final String[] excludes) throws IOException {
    if (COPIED_CONFIGS && lastConfigDstDir.equalsIgnoreCase(dstDir)
        && lastConfigSrcDir.equalsIgnoreCase(srcDir)) {
      log.info("No need to copy configuration files or "
          + "force global scope on all configs, "
          + "because they are still the same.");
      return;
    }
    log.info("Coping configuration files and "
        + "forcing global scope on all configs");
    FileUtils.deleteDir(configurationLocation);
    setConfigurationLocation(dstDir);
    FileUtils.copyDir(srcDir, dstDir, Arrays.asList(excludes));
    forceGlobalScopeOnAllConfigs(configurationLocation);
    COPIED_CONFIGS = true;
    lastConfigDstDir = dstDir;
    lastConfigSrcDir = srcDir;

  }

  /**
   * @param nucleusComponentPath
   *          where to store the created property file for the given service,
   *          relative to the {@link AtgDustTestCase#configDir} directory, set
   *          by {@link #AtgDuster()#setConfigPath(String)}
   * @param nucleusComponentClass
   *          a class that will be resolved by
   *          {@link AtgDustTestCase#getService(String)} in a later step
   * @throws IOException
   */
  protected final void createPropertyFile(final String nucleusComponentPath,
      final Class<?> nucleusComponentClass) throws IOException {
    NucleusTestUtils.createProperties(nucleusComponentPath,
        getConfigurationLocation(), nucleusComponentClass.getName(),
        new Properties()).deleteOnExit();
  }

  /**
   * 
   * @param configurationDirectory
   * @throws IOException
   */
  protected void forceGlobalScopeOnAllConfigs(final File configurationDirectory)
      throws IOException {

    // find all .properties in config path
    for (final File f : getFileListing(configurationDirectory)) {
      if (f.getPath().contains(".properties")) {
        // find scope other than global and replace with global
        FileUtils.searchAndReplace("$scope=", "$scope=global\n", f);
      }
    }
  }

  /**
   * 
   * @return the current configured configuration location path
   * @throws FileNotFoundException
   */
  private File getConfigurationLocation() throws FileNotFoundException {
    if (configurationLocation == null || !configurationLocation.exists()) {
      throw new FileNotFoundException("");
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
   * @return
   * @throws IOException
   */
  protected Object resolveNucleusComponent(final String nucleusComponentPath)
      throws IOException {
    startNucleus();
    return nucleus.resolveName(nucleusComponentPath);
  }

  /**
   * @param configurationLocation
   *          the configurationLocation to set
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
  protected void startNucleus() throws IOException {
    if (nucleus == null || !nucleus.isRunning()) {
      nucleus = NucleusTestUtils.startNucleus(getConfigurationLocation());
    }
  }

  /**
   * 
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

    // FileUtils.deleteDir(getConfigurationLocation());
    NucleusTestUtils.emptyConfigDirMap();
  }
}
