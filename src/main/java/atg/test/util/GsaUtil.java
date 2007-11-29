package atg.test.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GsaUtil {

  private static final Log log = LogFactory.getLog(GsaUtil.class);

  private final Connection connection;

  private boolean versioned = false;

  private final ConfigurationManager configManager;

  /**
   * 
   * @param properties
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public GsaUtil(Properties properties) throws ClassNotFoundException,
      SQLException {

    Class.forName(properties.getProperty("driver"));
    connection = DriverManager.getConnection(properties.getProperty("url"),
        properties.getProperty("user"), properties.getProperty("password"));

    log.info("Connected to "
        + connection.getMetaData().getDatabaseProductName() + " Version: "
        + connection.getMetaData().getDatabaseProductVersion());

    configManager = new ConfigurationManager(versioned, connection);

    // only create the id tables when using the default in memory hsql db
    if (properties.getProperty("url").contains("jdbc:hsqldb:mem:testDb")) {
      configManager.createIdGeneratorTables();
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          if (connection != null) {
            connection.close();
            log.info("Connection closed!");
          }
          else {
            log.info("Connection was already null");
          }
        }
        catch (SQLException e) {
          log.error("Error: ", e);
        }
      }
    });

  }

  public void initializeMinimalConfigpath(File root, String repositoryPath,
      String[] definitionFiles, Properties jdbcProperties,
      String createSqlAbsolutePath, String dropSqlAbsolutePath,
      String[] importedFiles, String fakeXaDataSourceComponentName,
      String jtdDataSourceComponentName, final boolean dropTables)
      throws IOException, Exception {

    // TODO: temporarily solution, so I can use a lower case url property name
    jdbcProperties.put("URL", jdbcProperties.getProperty("url"));

    configManager.createPropertiesByFileRoot(root);
    configManager.createJtdDataSource(root, null, null);
    configManager.createFakeXADataSource(root, jdbcProperties,
        fakeXaDataSourceComponentName);

    if (fakeXaDataSourceComponentName == null
        && jtdDataSourceComponentName == null) {
      configManager.createJtdDataSource(root, null, null);
    }
    else {
      configManager.createJtdDataSource(root, jtdDataSourceComponentName,
          fakeXaDataSourceComponentName);
    }
    if (repositoryPath != null) {
      configManager.createRepositoryConfigFile(root, repositoryPath,
          definitionFiles, createSqlAbsolutePath, dropSqlAbsolutePath,
          importedFiles, jtdDataSourceComponentName, dropTables);
    }

  }

  public void shutdown() throws SQLException {
    if (!connection.isClosed()) {
      if (connection.getMetaData().getDatabaseProductName().startsWith("HSQL")) {
        connection.createStatement().execute("SHUTDOWN");
      }
      connection.close();
    }
  }
}