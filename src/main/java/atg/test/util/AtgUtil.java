package atg.test.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.Nucleus;
import atg.nucleus.servlet.NucleusServlet;

/**
 * This class is a merger of atg.test.util.DBUtils, atg.nucleus.NucleusTestUtils
 * and atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that
 * just has the bare minimums needed for testing against an existing and/or
 * in-memory database.
 * 
 * @author robert
 * 
 */
public class AtgUtil {

  private static final Log log = LogFactory.getLog(AtgUtil.class);

  private final Connection connection;

  private boolean versioned = false, isDefaultInMemoryDb = false;

  private final ConfigurationManager configurationManager;

  /**
   * 
   * @param properties
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public AtgUtil(Properties properties, final boolean isDebug)
      throws ClassNotFoundException, SQLException {

    Class.forName(properties.getProperty("driver"));
    connection = DriverManager.getConnection(properties.getProperty("url"),
        properties.getProperty("user"), properties.getProperty("password"));

    log.info("Connected to "
        + connection.getMetaData().getDatabaseProductName() + " Version: "
        + connection.getMetaData().getDatabaseProductVersion());

    configurationManager = new ConfigurationManager(versioned, connection,
        isDebug);

    // cleaning up just before exit if not using the in-memory db
    isDefaultInMemoryDb = properties.getProperty("url").contains(
        "jdbc:hsqldb:mem:testDb");
    if (isDefaultInMemoryDb) {
      log
          .info("No need for rigourus cleaning up, default in-memory db was in use");
    }
    else {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            if (connection != null) {
              connection.close();
              log.info("ShutdownHook: Connection closed!");
            }
            else {
              log.info("ShutdownHook: Connection was already null");
            }
          }
          catch (SQLException e) {
            log.error("Error: ", e);
          }
        }
      });
    }
  }

  public void initializeMinimalRepositoryConfiguration(File root,
      String repositoryPath, String[] definitionFiles,
      Properties jdbcProperties, String createSqlAbsolutePath,
      String dropSqlAbsolutePath, String[] importedFiles,
      String fakeXaDataSourceComponentName, String jtdDataSourceComponentName,
      final boolean dropTables) throws IOException, Exception {

    // TODO: temporarily solution, so I can use a lower case url property name
    jdbcProperties.put("URL", jdbcProperties.getProperty("url"));

    log.info("Re-creating (drop and create) DAS_ID_GENERATOR = " + dropTables);

    if (dropTables) {
      configurationManager.createIdGeneratorTables();
    }

    configurationManager.createPropertiesByFileRoot(root);
    configurationManager.createJtdDataSource(root, null, null);
    configurationManager.createFakeXADataSource(root, jdbcProperties,
        fakeXaDataSourceComponentName);

    if (fakeXaDataSourceComponentName == null
        && jtdDataSourceComponentName == null) {
      configurationManager.createJtdDataSource(root, null, null);
    }
    else {
      configurationManager.createJtdDataSource(root,
          jtdDataSourceComponentName, fakeXaDataSourceComponentName);
    }
    if (repositoryPath != null) {
      configurationManager.createRepositoryConfigFile(root, repositoryPath,
          definitionFiles, createSqlAbsolutePath, dropSqlAbsolutePath,
          importedFiles, jtdDataSourceComponentName, dropTables);
    }

  }

  public void shutdownInMemoryDbAndCloseConnections() throws SQLException {
    if (isDefaultInMemoryDb) {
      connection.createStatement().execute("SHUTDOWN");
    }
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }

  }

  public static Nucleus startNucleus(File configpath) {
    System.setProperty("atg.dynamo.license.read", "true");
    System.setProperty("atg.license.read", "true");
    NucleusServlet.addNamingFactoriesAndProtocolHandlers();
    return Nucleus.startNucleus(new String[] { configpath.getAbsolutePath() });
  }
}