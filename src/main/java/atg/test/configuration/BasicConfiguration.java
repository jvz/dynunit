/**
 * 
 */
package atg.test.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.test.util.FileUtil;

/**
 * <i>This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.</i>
 * <p>
 * This class will created all properties files needed for non repository based
 * tests.
 * </p>
 * 
 * @author robert
 * 
 */
public final class BasicConfiguration {

  protected String isDebug = Boolean.FALSE.toString();

  protected final Map<String, String> settings = new HashMap<String, String>();

  protected static final Log log = LogFactory.getLog(BasicConfiguration.class);

  public void setDebug(final boolean isDebug) {
    this.isDebug = Boolean.toString(isDebug);
  }

  /**
   * 
   * @param isDebug
   */
  public BasicConfiguration() {
    super();
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  public void createPropertiesByConfigurationLocation(final File root)
      throws IOException {

    this.createClientLockManager(root);
    this.createGlobal(root);
    this.createInitialServices(root);
    this.createScreenLog(root);
    this.createXMLToolsFactory(root);

    log.info("Created non repository configuration file set");

  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createClientLockManager(final File root) throws IOException {
    this.settings.clear();
    settings.put("lockServerAddress", "localhost");
    settings.put("lockServerPort", "9010");
    settings.put("useLockServer", "false");
    FileUtil.createPropertyFile("ClientLockManager", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service"), "atg.service.lockmanager.ClientLockManager",
        settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createGlobal(final File root) throws IOException {
    this.settings.clear();
    settings.put("logListeners", "/atg/dynamo/service/logging/ScreenLog");
    settings.put("loggingDebug", isDebug);
    FileUtil.createPropertyFile("GLOBAL",
        new File(root.getAbsolutePath() + "/"), null, settings);

  }

  /**
   * 
   * Creates initial services properties like Initial, AppServerConfig, Nucleus,
   * etc, etc.
   * 
   * @param root
   * @throws IOException
   */
  private void createInitialServices(final File root) throws IOException {
    this.settings.clear();
    settings.put("initialServiceName", "/Initial");
    FileUtil.createPropertyFile("Nucleus", root, "atg.nucleus.Nucleus",
        settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createScreenLog(final File root) throws IOException {

    this.settings.clear();
    settings.put("cropStackTrace", "false");
    settings.put("loggingEnabled", isDebug);
    FileUtil.createPropertyFile("ScreenLog", new File(root.getAbsolutePath()
        + "/atg/dynamo/service/logging"),
        "atg.nucleus.logging.PrintStreamLogger", settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createXMLToolsFactory(final File root) throws IOException {
    FileUtil.createPropertyFile("XMLToolsFactory", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service/xml"),
        "atg.xml.tools.apache.ApacheXMLToolsFactory",
        new HashMap<String, String>());
  }

}
