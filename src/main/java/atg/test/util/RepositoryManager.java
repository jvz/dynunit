package atg.test.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.
 * 
 * <p>TODO: re-enable versioned repositories</p>
 * 
 * @author robert
 * 
 */
public class RepositoryManager {

  private static final Log log = LogFactory.getLog(RepositoryManager.class);

  private boolean isDefaultInMemoryDb;

  private final BasicDataSource dataSource = new BasicDataSource();

  /**
   * 
   * @param connectionSettings
   * @throws SQLException
   *           If some SQL related error occurs, like wrong password, unable to
   *           connect to database, unable to find the correct db driver, etc,
   *           etc.
   * @throws IOException
   */
  public void initializeMinimalRepositoryConfiguration(File configRoot,
      String repositoryPath, String[] definitionFiles,
      Map<String, String> settings, final boolean dropTables,
      final boolean isDebug) throws SQLException, IOException {

    dataSource.setDriverClassName(settings.get("driver"));
    dataSource.setUsername(settings.get("user"));
    dataSource.setPassword(settings.get("password"));
    dataSource.setUrl(settings.get("url"));

    log.info(String.format("Connected to '%s' using driver '%s'", dataSource
        .getUrl(), dataSource.getDriverClassName()));

    if (dropTables) {
      createIdGeneratorTables();
    }

    // TODO: fix hack no.01
    hackUrlPropToUpperCase(settings);

    final ConfigurationManager configurationManager = new ConfigurationManager(
        isDebug);
    configurationManager.createPropertiesByConfigRoot(configRoot);
    configurationManager.createFakeXADataSource(configRoot, settings);
    configurationManager.createRepositoryConfiguration(configRoot,
        repositoryPath, definitionFiles, dropTables);

    isDefaultInMemoryDb = settings.get("url")
        .contains("jdbc:hsqldb:mem:testDb");

  }

  /**
   * 
   * @throws SQLException
   */
  public void shutdownInMemoryDbAndCloseConnections() throws SQLException {
    if (isDefaultInMemoryDb) {
      dataSource.getConnection().createStatement().execute("SHUTDOWN");
    }
    dataSource.close();
  }

  /**
   * 
   * @throws SQLException
   */
  private void createIdGeneratorTables() throws SQLException {

    log.info("Re-creating (drop and create) DAS_ID_GENERATOR");

    final Statement statement = dataSource.getConnection().createStatement();
    try {
      statement.executeUpdate("DROP TABLE DAS_ID_GENERATOR");
    }
    catch (SQLException e) {
      // just try drop any existing DAS_ID_GENERATOR if desired
    }
    // create new DAS_ID_GENERATOR
    statement
        .executeUpdate("CREATE TABLE DAS_ID_GENERATOR(ID_SPACE_NAME VARCHAR(60) NOT NULL, "
            + "SEED NUMERIC(19, 0) NOT NULL, BATCH_SIZE INTEGER NOT NULL,   "
            + "PREFIX VARCHAR(10) DEFAULT NULL, SUFFIX VARCHAR(10) DEFAULT NULL, "
            + "PRIMARY KEY(ID_SPACE_NAME))");
    statement.close();
  }

  /**
   * TODO: HACK No 01, so I can use a lower case "url" property name
   * 
   * @param settings
   */
  private void hackUrlPropToUpperCase(Map<String, String> settings) {
    settings.put("URL", settings.get("url"));
  }
}
