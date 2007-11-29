package atg.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.NucleusTestUtils;

public class GsaUtil {

  private static final Log log = LogFactory.getLog(GsaUtil.class);

  public static File createFakeXADataSource(File root,
      Properties jdbcProperties, String name) throws IOException {
    if (name == null) {
      name = "FakeXADataSource";
    }
    jdbcProperties.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    return NucleusTestUtils.createProperties(name, new File(root
        .getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.FakeXADataSource",
        jdbcProperties);
  }

  public static File createJTDataSource(File pRoot, String pName,
      String pFakeXAName) throws IOException {
    String name = pName;
    if (name == null) {
      name = "JTDataSource";
    }

    String fakeXAName = pFakeXAName;
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
    props.put("loggingSQLWarning", "false");
    props.put("loggingSQLInfo", "false");
    props.put("loggingSQLDebug", "false");

    return NucleusTestUtils.createProperties(name, new File(pRoot
        .getAbsolutePath()
        + "/atg/dynamo/service/jdbc"), "atg.service.jdbc.MonitoredDataSource",
        props);
  }

  private final Connection connection;

  // from GSATestUtils
  private boolean versioned = false;

  /**
   * 
   * @param properties
   * @throws ClassNotFoundException
   * @throws SQLException
   * @throws Exception
   */
  public GsaUtil(Properties properties) throws ClassNotFoundException,
      SQLException {
    Class.forName(properties.getProperty("driver"));
    connection = DriverManager.getConnection(properties.getProperty("url"),
        properties.getProperty("user"), properties.getProperty("password"));
    log.info("Connected to "
        + connection.getMetaData().getDatabaseProductName() + " Version: "
        + connection.getMetaData().getDatabaseProductVersion());
    createIDGeneratorTables();
  }

  /**
   * @param pRoot
   * @throws IOException
   */
  public File createClientLockManager(File pRoot) throws IOException {
    Properties props = new Properties();
    props.put("lockServerAddress", "localhost");
    props.put("lockServerPort", "9010");
    props.put("useLockServer", "false");
    return NucleusTestUtils.createProperties("ClientLockManager", new File(
        pRoot.getAbsolutePath() + "/atg/dynamo/service"),
        "atg.service.lockmanager.ClientLockManager", props);
  }

  /**
   * Creates a GLOBAL.properties
   * 
   * @param pRoot
   * @param pJDBCProperties
   * @return
   * @throws IOException
   */
  public File createGlobal(File pRoot) throws IOException {
    Properties prop = new Properties();
    prop.put("logListeners", "atg/dynamo/service/logging/ScreenLog");
    prop.put("loggingDebug", "false");
    return NucleusTestUtils.createProperties("GLOBAL", new File(pRoot
        .getAbsolutePath()
        + "/"), null, prop);

  }

  /**
   * @param pRoot
   * @throws IOException
   */
  public File createIdGenerator(File pRoot) throws IOException {
    Properties props = new Properties();
    props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    // props.put("initialIdSpaces", "/atg/dynamo/service/idspaces.xml ");
    return NucleusTestUtils.createProperties("IdGenerator", new File(pRoot
        .getAbsolutePath()
        + "/atg/dynamo/service/"), "atg.service.idgen.SQLIdGenerator", props);
  }

  /**
   * Creates the das_id_generator tables using the given database
   * 
   * @param db
   * @throws SQLException
   */
  public void createIDGeneratorTables() throws SQLException {

    final String dbQuery = "CREATE TABLE das_id_generator(id_space_name VARCHAR(60) NOT NULL, seed NUMERIC(19, 0) NOT NULL, batch_size INTEGER NOT NULL,   prefix VARCHAR(10) DEFAULT NULL, suffix VARCHAR(10) DEFAULT NULL, PRIMARY KEY(id_space_name))";

    try {
      final Statement st = connection.createStatement();
      st.executeUpdate("drop table das_id_generator");
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
  public File createIdSpaces(File pRoot) throws IOException {
    String idspaces = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE id-spaces SYSTEM \"http://www.atg.com/dtds/idgen/idgenerator_1.0.dtd\"><id-spaces><id-space name=\"__default__\" seed=\"1\" batch-size=\"100000\"/><id-space name=\"jms_msg_ids\" seed=\"0\" batch-size=\"10000\"/><id-space name=\"layer\" seed=\"0\" batch-size=\"100\"/></id-spaces>";
    File idspacesFile = new File(pRoot.getAbsolutePath()
        + "/atg/dynamo/service/idspaces.xml");
    if (idspacesFile.exists())
      idspacesFile.delete();
    idspacesFile.getParentFile().mkdirs();
    idspacesFile.createNewFile();
    FileWriter out = new FileWriter(idspacesFile);
    try {
      out.write(idspaces);
      out.write("\n");
    }
    catch (IOException e) {
      e.printStackTrace();

    }
    finally {
      out.flush();
      out.close();
    }
    return idspacesFile;
  }

  /**
   * Creates Nucleus' Nucleus.properties
   * 
   */
  public File createNucleusProperties(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("initialServiceName", "/Initial");
    return NucleusTestUtils.createProperties("Nucleus", root,
        "atg.nucleus.Nucleus", prop);
  }

  // ---------------------------------
  /**
   * Creates a .properties file for the given repository. The actual repository
   * implementation is a <code>atg.adapter.gsa.InitializingGSA</code> class.
   * This implementation is used instead because it has the ability to create
   * tables and import data before the repository starts.
   * 
   * @param pRoot
   * @param pRepositoryPath
   * @param pDefinitionFiles
   * @param pSQLAbsolutePath
   * @param pDropSQLAbsolutePath
   * @param pImportFiles
   * @throws IOException
   */
  public File createRepositoryPropertiesFile(File root, String pRepositoryPath,
      String[] pDefinitionFiles, String pCreateSQLAbsolutePath,
      String pDropSQLAbsolutePath, String[] pImportFiles,
      String pJTDataSourceName) throws IOException {

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
        if (i < (pImportFiles.length - 1))
          importFiles.append(",");
      }
      props.put("importFiles", importFiles.toString());
      props.put("importEveryStartup", "true");
    }
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("idGenerator", "/atg/dynamo/service/IdGenerator");
    if (pJTDataSourceName == null)
      props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    else
      props.put("dataSource", "/atg/dynamo/service/jdbc/" + pJTDataSourceName);
    props.put("lockManager", "/atg/dynamo/service/ClientLockManager");
    props.put("idspaces", "/atg/dynamo/service/idspaces.xml");
    props.put("groupContainerPath", "/atg/registry/RepositoryGroups");
    props.put("restartingAfterTableCreation", "false");
    props.put("createTables", "true");
    props.put("loggingError", "true");
    if (pCreateSQLAbsolutePath != null)
      props.put("sqlCreateFiles", "default=" + pCreateSQLAbsolutePath);

    if (pDropSQLAbsolutePath != null)
      props.put("sqlDropFiles", "default=" + pDropSQLAbsolutePath);
    props.put("loggingDebug", "false");
    props.put("loggingCreateTables", "false");
    // props.put("debugLevel", "7");

    // InitializingGSA specific properties
    props.put("dropTablesIfExist", "true");
    props.put("dropTablesAtShutdown", "false");
    props.put("stripReferences", "true");
    int endIndex = pRepositoryPath.lastIndexOf("/");
    String repositoryDir = pRepositoryPath.substring(0, endIndex);
    String repositoryName = pRepositoryPath.substring(endIndex + 1,
        pRepositoryPath.length());
    File newRoot = new File(root, repositoryDir);
    newRoot.mkdirs();

    return NucleusTestUtils.createProperties(repositoryName, newRoot, clazz,
        props);
  }

  /**
   * Creates a ScreenLog component
   * 
   * @param pRoot
   * @param pLogging
   *          TODO
   * @return
   * @throws IOException
   */
  public File createScreenLog(File pRoot, boolean pLogging) throws IOException {
    Properties prop = new Properties();
    prop.put("cropStackTrace", "false");
    prop.put("loggingEnabled", String.valueOf(pLogging));
    return NucleusTestUtils.createProperties("ScreenLog", new File(pRoot
        .getAbsolutePath()
        + "/atg/dynamo/service/logging"),
        "atg.nucleus.logging.PrintStreamLogger", prop);
  }

  /**
   * Creates a SQLRepositoryEventServer
   * 
   * @param pRoot
   * @return
   */
  public File createSQLRepositoryEventServer(File pRoot) throws IOException {

    Properties prop = new Properties();
    prop.put("handlerCount", "0");
    return NucleusTestUtils.createProperties("SQLRepositoryEventServer",
        new File(pRoot.getAbsolutePath() + "/atg/dynamo/server"),
        "atg.adapter.gsa.event.GSAEventServer", prop);
  }

  /**
   * @param pRoot
   * @throws IOException
   */
  public File createTransactionManager(File pRoot) throws IOException {
    Properties props = new Properties();
    props.put("loggingDebug", "false");
    File root = new File(pRoot, "/atg/dynamo/transaction");
    root.mkdirs();
    NucleusTestUtils.createProperties("TransactionDemarcationLogging", root,
        "atg.dtm.TransactionDemarcationLogging", props);

    return NucleusTestUtils.createProperties("TransactionManager", root,
        "atg.dtm.TransactionManagerImpl", props);
  }

  /**
   * Creates the UserTransaction component
   */
  public File createUserTransaction(File pRoot) throws IOException {
    Properties props = new Properties();
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    return NucleusTestUtils.createProperties("UserTransaction", new File(pRoot,
        "/atg/dynamo/transaction"), "atg.dtm.UserTransactionImpl", props);
  }

  /**
   * @param pRoot
   * @throws IOException
   */
  public File createXMLToolsFactory(File pRoot) throws IOException {
    File root = new File(pRoot.getAbsolutePath() + "/atg/dynamo/service/xml");
    return NucleusTestUtils.createProperties("XMLToolsFactory", root,
        "atg.xml.tools.apache.ApacheXMLToolsFactory", new Properties());
  }

  public void initializeMinimalConfigpath(File root, String repositoryPath,
      String[] definitionFiles, Properties jdbcProperties,
      String createSqlAbsolutePath, String dropSqlAbsolutePath,
      String[] importedFiles, boolean logging,
      String fakeXaDataSourceComponentName, String jtdDataSourceComponentName)
      throws IOException, Exception {
    if (repositoryPath != null) {
      createRepositoryPropertiesFile(root, repositoryPath, definitionFiles,
          createSqlAbsolutePath, dropSqlAbsolutePath, importedFiles,
          jtdDataSourceComponentName);
    }
    createTransactionManager(root);
    createUserTransaction(root);
    createXMLToolsFactory(root);
    createIdGenerator(root);
    createClientLockManager(root);
    createJTDataSource(root, null, null);

    // TODO: temporarily solution, so I can use a lower case url property name
    jdbcProperties.put("URL", jdbcProperties.getProperty("url"));

    if (jdbcProperties != null) {
      createFakeXADataSource(root, jdbcProperties,
          fakeXaDataSourceComponentName);
    }

    if (fakeXaDataSourceComponentName == null
        && jtdDataSourceComponentName == null) {
      createJTDataSource(root, null, null);
    }
    else {
      DBUtils.createJTDataSource(root, jtdDataSourceComponentName,
          fakeXaDataSourceComponentName);
    }
    createGlobal(root);
    createScreenLog(root, logging);
    createIdSpaces(root);
    if (jdbcProperties != null) {
      createIDGeneratorTables();
    }
    createSQLRepositoryEventServer(root);
    createNucleusProperties(root);

  }

  public void shutdown() throws SQLException {
    if (!connection.isClosed()) {
      if (connection.getMetaData().getDatabaseProductName().startsWith("HSQL"))
        connection.createStatement().execute("SHUTDOWN");
      connection.close();
    }
  }
}