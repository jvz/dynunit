/**
 * 
 */
package atg.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.NucleusTestUtils;

/**
 * This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully a class that just has
 * the bare minimums needed for testing against an existin/in-memory db.
 * 
 * 
 * @author robert
 * 
 */
public final class ConfigurationManager {

  private final Connection connection;

  private boolean versioned = false;

  private static final String isDebug = Boolean.FALSE.toString();

  private static final Log log = LogFactory.getLog(GsaUtil.class);

  protected ConfigurationManager(final boolean versioned,
      final Connection connection) {
    this.versioned = versioned;
    this.connection = connection;

  }

  protected void createPropertiesByFileRoot(final File root)
      throws IOException, SQLException {
    this.createScreenLog(root);
    this.createIdGeneratorTables();
    this.createTransactionManager(root);
    this.createUserTransaction(root);
    this.createXMLToolsFactory(root);
    this.createIdGenerator(root);
    this.createClientLockManager(root);
    this.createGlobal(root);
    this.createIdSpaces(root);
    this.createSQLRepositoryEventServer(root);
    this.createNucleusProperties(root);
  }

  protected void createFakeXADataSource(File root, Properties jdbcProperties,
      String name) throws IOException {
    if (name == null) {
      name = "FakeXADataSource";
    }
    jdbcProperties.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    NucleusTestUtils.createProperties(name, new File(root.getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.FakeXADataSource",
        jdbcProperties);
  }

  protected void createJtdDataSource(File root, String pName, String fakeXaName)
      throws IOException {
    String name = pName;
    if (name == null) {
      name = "JTDataSource";
    }

    String fakeXAName = fakeXaName;
    if (fakeXAName == null) {
      fakeXAName = "FakeXADataSource";
    }

    Properties props = new Properties();
    props.put("dataSource", "/atg/dynamo/service/jdbc/" + fakeXAName);
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("min", "10");
    props.put("max", "20");
    props.put("blocking", "true");
    props.put("loggingSQLWarning", isDebug);
    props.put("loggingSQLInfo", isDebug);
    props.put("loggingSQLDebug", isDebug);

    NucleusTestUtils.createProperties(name, new File(root.getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.MonitoredDataSource",
        props);
  }

  /**
   * @param root
   * @throws IOException
   */
  private void createClientLockManager(File root) throws IOException {
    Properties props = new Properties();
    props.put("lockServerAddress", "localhost");
    props.put("lockServerPort", "9010");
    props.put("useLockServer", "false");
    NucleusTestUtils.createProperties("ClientLockManager", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service"), "atg.service.lockmanager.ClientLockManager",
        props);
  }

  /**
   * Creates a GLOBAL.properties
   * 
   * @param root
   * @param pJDBCProperties
   * @return
   * @throws IOException
   */
  private void createGlobal(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("logListeners", "atg/dynamo/service/logging/ScreenLog");
    prop.put("loggingDebug", isDebug);
    NucleusTestUtils.createProperties("GLOBAL", new File(root.getAbsolutePath()
        + "/"), null, prop);

  }

  private void createIdGenerator(File root) throws IOException {
    Properties props = new Properties();
    props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    NucleusTestUtils.createProperties("IdGenerator", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service/"), "atg.service.idgen.SQLIdGenerator", props);
  }

  protected void createIdGeneratorTables() throws SQLException {

    final String dbQuery = "CREATE TABLE DAS_ID_GENERATOR(ID_SPACE_NAME VARCHAR(60) NOT NULL, "
        + "SEED NUMERIC(19, 0) NOT NULL, BATCH_SIZE INTEGER NOT NULL,   "
        + "PREFIX VARCHAR(10) DEFAULT NULL, SUFFIX VARCHAR(10) DEFAULT NULL, "
        + "PRIMARY KEY(ID_SPACE_NAME))";

    try {
      final Statement st = connection.createStatement();
      st.executeUpdate("DROP TABLE DAS_ID_GENERATOR");
      st.close();
    }
    catch (Exception e) {
    }

    try {
      final Statement st = connection.createStatement();
      st.executeUpdate(dbQuery);
      st.close();
    }
    catch (SQLException e) {
      log.error("Error: creating das_id_generator", e);
    }
  }

  /**
   * Writes the idspaces.xml file
   */
  private void createIdSpaces(File root) throws IOException {
    String idspaces = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE id-spaces SYSTEM \"http://www.atg.com/dtds/idgen/idgenerator_1.0.dtd\"><id-spaces><id-space name=\"__default__\" seed=\"1\" batch-size=\"100000\"/><id-space name=\"jms_msg_ids\" seed=\"0\" batch-size=\"10000\"/><id-space name=\"layer\" seed=\"0\" batch-size=\"100\"/></id-spaces>";
    File idspacesFile = new File(root.getAbsolutePath()
        + "/atg/dynamo/service/idspaces.xml");
    if (idspacesFile.exists()) {
      idspacesFile.delete();
    }
    idspacesFile.getParentFile().mkdirs();
    idspacesFile.createNewFile();
    FileWriter out = new FileWriter(idspacesFile);
    try {
      out.write(idspaces);
      out.write("\n");
      out.flush();

    }
    catch (IOException e) {
      log.error("Error: ", e);

    }
    finally {
      out.close();

    }
  }

  /**
   * Creates Nucleus' Nucleus.properties
   * 
   */
  private void createNucleusProperties(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("initialServiceName", "/Initial");
    NucleusTestUtils.createProperties("Nucleus", root, "atg.nucleus.Nucleus",
        prop);
  }

  /**
   * 
   * @param root
   * @param repositoryPath
   * @param pDefinitionFiles
   * @param createSQLAbsolutePath
   * @param dropSQLAbsolutePath
   * @param pImportFiles
   * @param jtdDataSourceName
   * @return
   * @throws IOException
   */
  protected void createRepositoryConfigFile(File root, String repositoryPath,
      String[] pDefinitionFiles, String createSQLAbsolutePath,
      String dropSQLAbsolutePath, String[] pImportFiles,
      String jtdDataSourceName, final boolean droptables) throws IOException {

    final String clazz;
    if (versioned) {
      clazz = "atg.adapter.gsa.InitializingVersionRepository";
    }
    else {
      clazz = "atg.adapter.gsa.InitializingGSA";
    }

    Properties props = new Properties();
    props.put("repositoryName", "TestRepository" + System.currentTimeMillis());
    StringBuffer definitionFiles = new StringBuffer();
    for (int i = 0; i < pDefinitionFiles.length; i++) {
      definitionFiles.append("/" + pDefinitionFiles[i]);
      if (i < (pDefinitionFiles.length - 1))
        definitionFiles.append(",");
    }
    props.put("definitionFiles", definitionFiles.toString());
    if (pImportFiles != null) {
      StringBuffer importFiles = new StringBuffer();
      for (int i = 0; i < pImportFiles.length; i++) {
        importFiles.append(new File(root, pImportFiles[i]).getAbsolutePath());
        if (i < (pImportFiles.length - 1)) {
          importFiles.append(",");
        }
      }
      props.put("importFiles", importFiles.toString());
      props.put("importEveryStartup", "true");
    }
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("idGenerator", "/atg/dynamo/service/IdGenerator");
    if (jtdDataSourceName == null)
      props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    else
      props.put("dataSource", "/atg/dynamo/service/jdbc/" + jtdDataSourceName);
    props.put("lockManager", "/atg/dynamo/service/ClientLockManager");
    props.put("idspaces", "/atg/dynamo/service/idspaces.xml");
    props.put("groupContainerPath", "/atg/registry/RepositoryGroups");
    props.put("restartingAfterTableCreation", "false");
    props.put("createTables", "true");
    props.put("loggingError", "true");
    if (createSQLAbsolutePath != null)
      props.put("sqlCreateFiles", "default=" + createSQLAbsolutePath);

    if (dropSQLAbsolutePath != null)
      props.put("sqlDropFiles", "default=" + dropSQLAbsolutePath);
    props.put("loggingDebug", isDebug);
    props.put("loggingCreateTables", isDebug);
    // props.put("debugLevel", "7");

    // InitializingGSA specific properties
    props.put("dropTablesIfExist", Boolean.toString(droptables));
    props.put("dropTablesAtShutdown", Boolean.toString(droptables));
    props.put("stripReferences", "true");
    int endIndex = repositoryPath.lastIndexOf("/");
    String repositoryDir = repositoryPath.substring(0, endIndex);
    String repositoryName = repositoryPath.substring(endIndex + 1,
        repositoryPath.length());
    File newRoot = new File(root, repositoryDir);
    newRoot.mkdirs();
    NucleusTestUtils.createProperties(repositoryName, newRoot, clazz, props);
  }

  /**
   * Creates a ScreenLog component
   * 
   * @param root
   * @param isLogging
   *          TODO
   * @return
   * @throws IOException
   */
  protected void createScreenLog(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("cropStackTrace", "false");
    prop.put("loggingEnabled", isDebug);
    NucleusTestUtils.createProperties("ScreenLog", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service/logging"),
        "atg.nucleus.logging.PrintStreamLogger", prop);
  }

  /**
   * Creates a SQLRepositoryEventServer
   * 
   * @param root
   * @return
   */
  private void createSQLRepositoryEventServer(File root) throws IOException {

    Properties prop = new Properties();
    prop.put("handlerCount", "0");
    NucleusTestUtils.createProperties("SQLRepositoryEventServer", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/server"), "atg.adapter.gsa.event.GSAEventServer", prop);
  }

  /**
   * @param pRoot
   * @throws IOException
   */
  private void createTransactionManager(File pRoot) throws IOException {
    Properties props = new Properties();
    props.put("loggingDebug", isDebug);
    File root = new File(pRoot, "/atg/dynamo/transaction");
    root.mkdirs();
    NucleusTestUtils.createProperties("TransactionDemarcationLogging", root,
        "atg.dtm.TransactionDemarcationLogging", props);
    NucleusTestUtils.createProperties("TransactionManager", root,
        "atg.dtm.TransactionManagerImpl", props);
  }

  /**
   * Creates the UserTransaction component
   */
  private void createUserTransaction(File root) throws IOException {
    Properties props = new Properties();
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    NucleusTestUtils.createProperties("UserTransaction", new File(root,
        "/atg/dynamo/transaction"), "atg.dtm.UserTransactionImpl", props);
  }

  /**
   * @param pRoot
   * @throws IOException
   */
  private void createXMLToolsFactory(File pRoot) throws IOException {
    File root = new File(pRoot.getAbsolutePath() + "/atg/dynamo/service/xml");
    NucleusTestUtils.createProperties("XMLToolsFactory", root,
        "atg.xml.tools.apache.ApacheXMLToolsFactory", new Properties());
  }

}
