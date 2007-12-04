package atg.test.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.
 * 
 * <p>
 * TODO: re-enable versioned repositories
 * </p>
 * 
 * @author robert
 * 
 */
public class RepositoryManager {

  private static Logger log = Logger.getLogger(RepositoryManager.class);

  private boolean isDefaultInMemoryDb;

  private final BasicDataSource dataSource = new BasicDataSource();

  /**
   * 
   * @param configRoot
   * @param repositoryPath
   *          The the repository to be tested, specified as nucleus component
   *          path.
   * @param settings
   * 
   * A {@link Properties} instance with the following values (in this example
   * the properties are geared towards an mysql database):
   * 
   * <pre>
   * final Properties properties = new Properties();
   * properties.put(&quot;driver&quot;, &quot;com.mysql.jdbc.Driver&quot;);
   * properties.put(&quot;url&quot;, &quot;jdbc:mysql://localhost:3306/someDb&quot;);
   * properties.put(&quot;user&quot;, &quot;someUserName&quot;);
   * properties.put(&quot;password&quot;, &quot;somePassword&quot;);
   * </pre>
   * 
   * @param dropTables
   *          If <code>true</code> then existing tables will be dropped and
   *          re-created, if set to <code>false</code> the existing tables
   *          will be used.
   * @param isDebug
   *          Enables or disables debugging.
   * @param definitionFiles
   *          One or more needed repository definition files.
   * @throws SQLException
   * @throws IOException
   */
  public void initializeMinimalRepositoryConfiguration(File configRoot,
      String repositoryPath, Map<String, String> settings,
      final boolean dropTables, final boolean isDebug,
      String... definitionFiles) throws SQLException, IOException {

    dataSource.setDriverClassName(settings.get("driver"));
    dataSource.setUsername(settings.get("user"));
    dataSource.setPassword(settings.get("password"));
    dataSource.setUrl(settings.get("url"));

    log.info(String.format("Connected to '%s' using driver '%s'", dataSource
        .getUrl(), dataSource.getDriverClassName()));

    if (dropTables) {
      createIdGeneratorTables();
    }
    else {
      log.info("Existing tables will be used.");
    }

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

}
