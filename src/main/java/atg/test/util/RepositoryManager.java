package atg.test.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is a merger of atg.test.util.DBUtils, atg.nucleus.NucleusTestUtils
 * and atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that
 * just has the bare minimums needed for testing against an existing and/or
 * in-memory database.
 * 
 * @author robert
 * 
 */
public class RepositoryManager {

  private static final Log log = LogFactory.getLog(RepositoryManager.class);

  /**
   * Currently versioning is disabled by default
   */
  private boolean versioned, isDefaultInMemoryDb;

  private ConfigurationManager configurationManager;

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

    log.info(String.format("Connected to using driver: %s", dataSource
        .getDriverClassName()));

    configurationManager = new ConfigurationManager(versioned, dataSource,
        isDebug);

    isDefaultInMemoryDb = settings.get("url")
        .contains("jdbc:hsqldb:mem:testDb");

    settings = addHack_UrlPropToUpperCase(settings);

    if (dropTables) {
      log.info("Re-creating (drop and create) DAS_ID_GENERATOR");
      configurationManager.createIdGeneratorTables();
    }

    configurationManager.createPropertiesByConfigRoot(configRoot);
    configurationManager.createFakeXADataSource(configRoot, settings);
    configurationManager.createRepositoryConfiguration(configRoot,
        repositoryPath, definitionFiles, dropTables);

  }

  /**
   * TODO: HACK No 01, so I can use a lower case "url" property name
   * 
   * @param settings
   * @return
   */
  private Map<String, String> addHack_UrlPropToUpperCase(
      Map<String, String> settings) {
    settings.put("URL", settings.get("url"));
    return settings;
  }

  public void shutdownInMemoryDbAndCloseConnections() throws SQLException {
    if (isDefaultInMemoryDb) {
      dataSource.getConnection().createStatement().execute("SHUTDOWN");
    }
    dataSource.close();
  }
}
