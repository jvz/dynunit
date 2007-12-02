/**
 * 
 */
package atg.test.configuration;

import java.io.File;
import java.io.FileWriter;
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
 * This class will created all properties files needed for repository based
 * tests.
 * </p>
 * 
 * @author robert
 * 
 */
public final class RepositoryConfiguration {

  // TODO-1 []: re-add versioned repository support?
  // TODO-2 []: better/more uniform way of handling properties file creation

  protected String isDebug = Boolean.FALSE.toString();

  protected final Map<String, String> settings = new HashMap<String, String>();

  protected static final Log log = LogFactory.getLog(BasicConfiguration.class);

  public void setDebug(final boolean isDebug) {
    this.isDebug = Boolean.toString(isDebug);
  }

  public RepositoryConfiguration() {
    super();
  }

  /**
   * 
   * @param root
   * @throws IOException
   */
  public void createPropertiesByConfigurationLocation(final File root)
      throws IOException {
    this.createTransactionManager(root);
    this.createUserTransaction(root);
    this.createIdGenerator(root);
    this.createIdSpaces(root);
    this.createSQLRepositoryEventServer(root);
    this.createJtdDataSource(root);

    log.info("Created basic repository configuration file set");
  }

  /**
   * 
   * @param root
   * @param jdbcSettings
   * @throws IOException
   */
  public void createFakeXADataSource(final File root,
      Map<String, String> jdbcSettings) throws IOException {

    // TODO: Something expects the url property name in upper case... still have
    // to investigate.
    jdbcSettings.put("URL", jdbcSettings.get("url"));
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
  private void createJtdDataSource(final File root) throws IOException {
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
   * @param root
   * @param repositoryPath
   * @param droptables
   * @param definitionFiles
   * @throws IOException
   */
  public void createRepositoryConfiguration(final File root,
      final String repositoryPath, final boolean droptables,
      final String... definitionFiles) throws IOException {

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

}
