/**
 * 
 */
package atg.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.
 * 
 * @author robert
 * 
 */
public final class ConfigurationManager {

  // TODO-1 []: re-add versioned repository support?
  // TODO-2 []: better/more uniform way of handling properties file creation

  private String isDebug = Boolean.FALSE.toString();

  // private static final Log log =
  // LogFactory.getLog(ConfigurationManager.class);

  private final Map<String, String> settings = new HashMap<String, String>();

  /**
   * 
   * @param isVersioned
   * @param isDebug
   */
  protected ConfigurationManager(final boolean isDebug) {
    this.isDebug = Boolean.toString(isDebug);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  protected void createPropertiesByConfigRoot(final File root)
      throws IOException {
    this.createScreenLog(root);
    this.createTransactionManager(root);
    this.createUserTransaction(root);
    this.createXMLToolsFactory(root);
    this.createIdGenerator(root);
    this.createClientLockManager(root);
    this.createGlobal(root);
    this.createIdSpaces(root);
    this.createSQLRepositoryEventServer(root);
    this.createInitialServices(root);
    this.createJtdDataSource(root);
  }

  /**
   * 
   * @param root
   * @param jdbcSettings
   * @throws IOException
   */
  protected void createFakeXADataSource(final File root,
      Map<String, String> jdbcSettings) throws IOException {

    jdbcSettings.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");

    FileUtil.createPropertyFile("FakeXADataSource", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.FakeXADataSource",
        jdbcSettings);

  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  protected void createJtdDataSource(final File root) throws IOException {
    this.settings.clear();
    settings.put("dataSource", "/atg/dynamo/service/jdbc/FakeXADataSource");
    settings.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    settings.put("min", "10");
    settings.put("max", "20");
    settings.put("blocking", "true");
    settings.put("loggingSQLWarning", isDebug);
    settings.put("loggingSQLInfo", isDebug);
    settings.put("loggingSQLDebug", isDebug);

    FileUtil.createPropertyFile("JTDataSource", new File(root.getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.MonitoredDataSource",
        settings);
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
   * @param root
   * @throws IOException
   */
  private void createIdGenerator(final File root) throws IOException {
    this.settings.clear();
    settings.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    settings.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    settings.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    FileUtil
        .createPropertyFile("IdGenerator", new File(root.getAbsolutePath()
            + "/atg/dynamo/service/"), "atg.service.idgen.SQLIdGenerator",
            settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createIdSpaces(final File root) throws IOException {
    final String idspaces = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE id-spaces SYSTEM \"http://www.atg.com/dtds/idgen/idgenerator_1.0.dtd\"><id-spaces><id-space name=\"__default__\" seed=\"1\" batch-size=\"100000\"/><id-space name=\"jms_msg_ids\" seed=\"0\" batch-size=\"10000\"/><id-space name=\"layer\" seed=\"0\" batch-size=\"100\"/></id-spaces>";
    final File idspacesFile = new File(root.getAbsolutePath()
        + "/atg/dynamo/service/idspaces.xml");

    idspacesFile.delete();
    idspacesFile.getParentFile().mkdirs();
    idspacesFile.createNewFile();
    FileWriter out = new FileWriter(idspacesFile);
    out.write(idspaces);
    out.write("\n");
    out.flush();
    out.close();
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
   * @param repositoryPath
   * @param definitionFiles
   * @param droptables
   * @throws IOException
   */
  protected void createRepositoryConfiguration(final File root,
      String repositoryPath, String[] definitionFiles, final boolean droptables)
      throws IOException {

    this.settings.clear();

    final StringBuilder defFiles = new StringBuilder();
    for (int i = 0; i < definitionFiles.length; i++) {
      defFiles.append("/" + definitionFiles[i]);
    }
    settings.put("definitionFiles", defFiles.toString());

    settings.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    settings.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    settings.put("idGenerator", "/atg/dynamo/service/IdGenerator");
    settings.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");

    settings.put("lockManager", "/atg/dynamo/service/ClientLockManager");
    settings.put("idspaces", "/atg/dynamo/service/idspaces.xml");
    settings.put("groupContainerPath", "/atg/registry/RepositoryGroups");
    settings.put("restartingAfterTableCreation", "false");
    settings.put("createTables", "true");
    settings.put("loggingError", "true");
    settings.put("loggingDebug", isDebug);
    settings.put("loggingCreateTables", isDebug);
    settings.put("debugLevel", "7");

    // InitializingGSA specific properties
    settings.put("dropTablesIfExist", Boolean.toString(droptables));
    settings.put("dropTablesAtShutdown", Boolean.toString(droptables));
    settings.put("stripReferences", "true");
    int endIndex = repositoryPath.lastIndexOf("/");
    String repositoryDir = repositoryPath.substring(0, endIndex);
    String repositoryName = repositoryPath.substring(endIndex + 1,
        repositoryPath.length());
    File newRoot = new File(root, repositoryDir);
    newRoot.mkdirs();
    FileUtil.createPropertyFile(repositoryName, newRoot,
        "atg.adapter.gsa.InitializingGSA", settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  protected void createScreenLog(final File root) throws IOException {

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
  private void createSQLRepositoryEventServer(final File root)
      throws IOException {
    this.settings.clear();
    settings.put("handlerCount", "0");
    FileUtil.createPropertyFile("SQLRepositoryEventServer", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/server"), "atg.adapter.gsa.event.GSAEventServer",
        settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createTransactionManager(final File root) throws IOException {
    this.settings.clear();
    settings.put("loggingDebug", isDebug);
    File newRoot = new File(root, "/atg/dynamo/transaction");
    newRoot.mkdirs();
    FileUtil.createPropertyFile("TransactionDemarcationLogging", newRoot,
        "atg.dtm.TransactionDemarcationLogging", settings);
    FileUtil.createPropertyFile("TransactionManager", newRoot,
        "atg.dtm.TransactionManagerImpl", settings);
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  private void createUserTransaction(final File root) throws IOException {
    this.settings.clear();
    settings.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    FileUtil.createPropertyFile("UserTransaction", new File(root,
        "/atg/dynamo/transaction"), "atg.dtm.UserTransactionImpl", settings);
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
