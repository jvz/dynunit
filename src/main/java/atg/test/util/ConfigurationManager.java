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
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.core.util.StringUtils;

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
  // TODO-2 []: better way of handling property creation
  // TODO-3 []: other way for debug enabling

  private final Connection connection;

  private String isDebug = Boolean.FALSE.toString();

  private static final Log log = LogFactory.getLog(AtgUtil.class);

  protected ConfigurationManager(final boolean isVersioned,
      final Connection connection, final boolean isDebug) {

    if (isVersioned) {
      throw new UnsupportedOperationException(
          "Versioned Repositories are currently not supported");
    }
    this.connection = connection;
    this.isDebug = Boolean.toString(isDebug);
  }

  protected void createPropertiesByFileRoot(final File root)
      throws IOException, SQLException {
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
  }

  protected void createFakeXADataSource(File root, Properties jdbcProperties,
      String name) throws IOException {

    jdbcProperties.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    createProperties(name == null ? name = "FakeXADataSource" : name, new File(
        root.getAbsolutePath() + "/atg/dynamo/service/jdbc"),
        "atg.service.jdbc.FakeXADataSource", jdbcProperties);
  }

  protected void createJtdDataSource(File root, String name, String fakeXaName)
      throws IOException {

    if (fakeXaName == null) {
      fakeXaName = "FakeXADataSource";
    }

    Properties props = new Properties();
    props.put("dataSource", "/atg/dynamo/service/jdbc/" + fakeXaName);
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("min", "10");
    props.put("max", "20");
    props.put("blocking", "true");
    props.put("loggingSQLWarning", isDebug);
    props.put("loggingSQLInfo", isDebug);
    props.put("loggingSQLDebug", isDebug);

    createProperties(name == null ? name = "JTDataSource" : name, new File(root
        .getAbsolutePath()
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
    createProperties("ClientLockManager", new File(root.getAbsolutePath()
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
    prop.put("logListeners", "/atg/dynamo/service/logging/ScreenLog");
    prop.put("loggingDebug", isDebug);
    createProperties("GLOBAL", new File(root.getAbsolutePath() + "/"), null,
        prop);

  }

  private void createIdGenerator(File root) throws IOException {
    Properties props = new Properties();
    props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    createProperties("IdGenerator", new File(root.getAbsolutePath()
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
   * Creates initial services properties like Initial, AppServerConfig, Nucleus,
   * etc, etc.
   * 
   */
  private void createInitialServices(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("initialServiceName", "/Initial");
    createProperties("Nucleus", root, "atg.nucleus.Nucleus", prop);
  }

  /**
   * 
   * @param root
   * @param repositoryPath
   * @param definitionFiles
   * @param createSQLAbsolutePath
   * @param dropSQLAbsolutePath
   * @param importFiles
   * @param jtdDataSourceName
   * @return
   * @throws IOException
   */
  protected void createRepositoryConfigFile(File root, String repositoryPath,
      String[] definitionFiles, String createSQLAbsolutePath,
      String dropSQLAbsolutePath, String[] importFiles,
      String jtdDataSourceName, final boolean droptables) throws IOException {

    final String clazz = "atg.adapter.gsa.InitializingGSA";

    final Properties props = new Properties();

    final StringBuilder defFiles = new StringBuilder();
    for (int i = 0; i < definitionFiles.length; i++) {
      defFiles.append("/" + definitionFiles[i]);
      if (i < (definitionFiles.length - 1))
        defFiles.append(",");
    }
    props.put("definitionFiles", defFiles.toString());
    if (importFiles != null) {
      final StringBuilder impFiles = new StringBuilder();
      for (int i = 0; i < importFiles.length; i++) {
        impFiles.append(new File(root, importFiles[i]).getAbsolutePath());
        if (i < (importFiles.length - 1)) {
          impFiles.append(",");
        }
      }
      props.put("importFiles", impFiles.toString());
      props.put("importEveryStartup", "true");
    }
    props.put("XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory");
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    props.put("idGenerator", "/atg/dynamo/service/IdGenerator");
    if (jtdDataSourceName == null) {
      props.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
    }
    else {
      props.put("dataSource", "/atg/dynamo/service/jdbc/" + jtdDataSourceName);
    }
    props.put("lockManager", "/atg/dynamo/service/ClientLockManager");
    props.put("idspaces", "/atg/dynamo/service/idspaces.xml");
    props.put("groupContainerPath", "/atg/registry/RepositoryGroups");
    props.put("restartingAfterTableCreation", "false");
    props.put("createTables", "true");
    props.put("loggingError", "true");
    if (createSQLAbsolutePath != null) {
      props.put("sqlCreateFiles", "default=" + createSQLAbsolutePath);
    }

    if (dropSQLAbsolutePath != null) {
      props.put("sqlDropFiles", "default=" + dropSQLAbsolutePath);
    }
    props.put("loggingDebug", isDebug);
    props.put("loggingCreateTables", isDebug);
    props.put("debugLevel", "7");

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
    createProperties(repositoryName, newRoot, clazz, props);
  }

  /**
   * Creates a ScreenLog component
   * 
   * @param root
   * @param isLogging
   * @return
   * @throws IOException
   */
  protected void createScreenLog(File root) throws IOException {
    Properties prop = new Properties();
    prop.put("cropStackTrace", "false");
    prop.put("loggingEnabled", isDebug);
    createProperties("ScreenLog", new File(root.getAbsolutePath()
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
    createProperties("SQLRepositoryEventServer", new File(root
        .getAbsolutePath()
        + "/atg/dynamo/server"), "atg.adapter.gsa.event.GSAEventServer", prop);
  }

  /**
   * @param root
   * @throws IOException
   */
  private void createTransactionManager(File root) throws IOException {
    Properties props = new Properties();
    props.put("loggingDebug", isDebug);
    File newRoot = new File(root, "/atg/dynamo/transaction");
    newRoot.mkdirs();
    createProperties("TransactionDemarcationLogging", newRoot,
        "atg.dtm.TransactionDemarcationLogging", props);
    createProperties("TransactionManager", newRoot,
        "atg.dtm.TransactionManagerImpl", props);
  }

  /**
   * Creates the UserTransaction component
   */
  private void createUserTransaction(File root) throws IOException {
    Properties props = new Properties();
    props.put("transactionManager",
        "/atg/dynamo/transaction/TransactionManager");
    createProperties("UserTransaction", new File(root,
        "/atg/dynamo/transaction"), "atg.dtm.UserTransactionImpl", props);
  }

  /**
   * @param root
   * @throws IOException
   */
  private void createXMLToolsFactory(File root) throws IOException {
    createProperties("XMLToolsFactory", new File(root.getAbsolutePath()
        + "/atg/dynamo/service/xml"),
        "atg.xml.tools.apache.ApacheXMLToolsFactory", new Properties());
  }

  // from NucleusTestUtils

  /**
   * Creates a .properties file
   * 
   * @param componentName
   * @param configurationLocation
   * @param clazz
   * @param props
   * @return
   * @throws IOException
   */
  public static File createProperties(String componentName,
      File configurationLocation, String clazz, Properties props)
      throws IOException {
    File prop;
    if (configurationLocation == null)
      prop = new File("./" + componentName + ".properties");
    else {
      configurationLocation.mkdirs();
      prop = new File(configurationLocation, componentName + ".properties");
      new File(prop.getParent()).mkdirs();
    }

    if (prop.exists()) {
      prop.delete();
    }
    prop.createNewFile();
    final FileWriter fw = new FileWriter(prop);
    final String classLine = "$class=" + clazz + "\n";
    try {
      if (clazz != null) {
        fw.write(classLine);
      }
      if (props != null) {
        Iterator<?> iter = props.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String) iter.next();
          String thisLine = key + "="
              + StringUtils.replace(props.getProperty(key), '\\', "\\\\")
              + "\n";
          fw.write(thisLine);
        }
      }
    }
    finally {
      fw.flush();
      fw.close();
    }
    return prop;
  }

}
