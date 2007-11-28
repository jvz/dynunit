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
 * {@link AtgDustCase#copyDir(String, String, String[])}</b>.</li>
 * <li>Tell {@link AtgDustCase} class where the configuration location is by
 * using <b>{@link AtgDustCase#setConfigurationLocation(String)}</b>. BTW you
 * always want to point it to some temp location, because this framework will
 * gerenated needed property files and if you point it to some location in your
 * existing source tree, you will polute it with these gerenerate files.</li>
 * <li><b><i>Optional</i></b> if you want to create repository based tests
 * use <b>{@link AtgDustCase#prepareRepositoryTest(String[], String)}</b> or
 * <b>{@link AtgDustCase#prepareRepositoryTest(String[], String, Properties, boolean)}</b>.</li>
 * </ol>
 * Example usage can be found in test.SongRepositoryNewTest.
 * 
 * @author robert
 */
public class AtgDustCase extends TestCase {

  private static final Log log = LogFactory.getLog(AtgDustCase.class);

  private File configurationLocation;

  private DBUtils dbUtils;

  private transient Nucleus nucleus;

  private static boolean COPIED_CONFIGS = false;

  private static String lastConfigSrcDir, lastConfigDstDir;

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
   * @return the current configured configuration location path
   * @throws FileNotFoundException
   */
  private File getConfigurationLocation() throws FileNotFoundException {
    if (configurationLocation == null || !configurationLocation.exists()) {
      throw new FileNotFoundException("");
    }
    return configurationLocation;
  }

  // default repository test against an default in-memory hsql db
  protected void prepareRepositoryTest(final String[] definitionFiles,
      final String repoPath) throws Exception {

    final Properties props = new Properties();
    props.put("driver", "org.hsqldb.jdbcDriver");
    props.put("URL", "jdbc:hsqldb:mem:testDb");
    props.put("user", "sa");
    props.put("password", "");

    prepareRepositoryTest(definitionFiles, repoPath, props, false);

  }

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
    this.configurationLocation = new File(configurationLocation);
    log.info("Using configurationLocation: "
        + this.configurationLocation.getPath());
  }

  protected void startNucleus() throws IOException {
    if (nucleus == null || !nucleus.isRunning()) {
      // force the scope to global
      nucleus = NucleusTestUtils.startNucleus(getConfigurationLocation());
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (dbUtils != null) {
      dbUtils.shutdown();
    }
    nucleus.doStopService();
    nucleus.stopService();
    nucleus.destroy();

    // FileUtils.deleteDir(getConfigurationLocation());
    NucleusTestUtils.emptyConfigDirMap();
  }

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

  protected void copyDir(final String srcDir, final String dstDir,
      final String[] excludes) throws IOException {
    if (COPIED_CONFIGS && lastConfigDstDir.equalsIgnoreCase(dstDir)
        && lastConfigSrcDir.equalsIgnoreCase(srcDir)) {
      log.info("No config copy, because they are still the same");
      return;
    }
    log.info("Config copy and forcing global scope");
    FileUtils.deleteDir(configurationLocation);
    configurationLocation = new File(dstDir);
    FileUtils.copyDir(srcDir, dstDir, Arrays.asList(excludes));
    forceGlobalScopeOnAllConfigs(configurationLocation);
    COPIED_CONFIGS = true;
    lastConfigDstDir = dstDir;
    lastConfigSrcDir = srcDir;

  }
}
