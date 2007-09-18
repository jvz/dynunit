package test;

import static atg.test.AtgDustTestCase.DbVendor.MySQLDBConnection;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import atg.adapter.gsa.GSARepository;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.test.AtgDustTestCase;

/**
 * 
 * Example test case to illustrate the usage of AtgDustTestCase built-in
 * database functionalities. Before running this test from your ide do a mvn
 * resources:testResources to copy all needed file to the expected locations.
 * 
 * 
 * @author robert
 * 
 */
public class SongRepositoryTest extends AtgDustTestCase {

  private String userName;
  private String password;
  private String host;
  private String port;
  private String dbName;
  private String enabled;
  private final Properties properties = new Properties();

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Runs a test against an in-memory HSQL database
   * 
   * @throws Exception
   */
  public void testWithInMemoryDb() throws Exception {
    prepareRepositoryTest(new File("target/test-classes/config/"),
        new String[] { "GettingStarted/songs.xml" },
        "/GettingStarted/SongsRepository");

    // The actual test is quite generic. The only difference is the way the
    // repository is prepared by the prepareRepositoryTest method
    songsRepositoryTest();
  }

  /**
   * Example test with MySQL Database. This test is disabled by default (set to
   * false/or not set in the env.properties) because the MySQL JDBC drivers are
   * not included in the atg dust package.
   * 
   * To make use of this test, install a mysql-connector-java (mysql jdbc
   * driver) into your .m2/repository, un-comment the mysql dependency in the
   * pom.xml. Test data can be found in
   * src/test/resources/config/GettingStarted/songs-data.xml.
   * 
   * 
   * @throws Exception
   */
  public void testWithExistingMysqlDb() throws Exception {

    properties.load(new FileInputStream("src/test/resources/env.properties"));
    userName = properties.getProperty("userName");
    password = properties.getProperty("password");
    host = properties.getProperty("host");
    port = properties.getProperty("port");
    dbName = properties.getProperty("dbName");
    enabled = properties.getProperty("enabled");

    if (enabled == null || enabled.equalsIgnoreCase("false")) {
      return;
    }

    prepareRepositoryTest(new File("target/test-classes/config/"),
        new String[] { "GettingStarted/songs.xml" },
        "/GettingStarted/SongsRepository", userName, password, host, port,
        dbName, MySQLDBConnection, false);

    // this was a small test to fix some configuration related bug I was having
    assertNotNull(getService("/test/TestComponent"));

    // The actual test is quite generic. The only difference is the way the
    // repository is prepared by the prepareRepositoryTest method
    songsRepositoryTest();
  }

  private void songsRepositoryTest() throws TransactionDemarcationException,
      RepositoryException {
    GSARepository songsRepository = (GSARepository) getService("/GettingStarted/SongsRepository");
    assertNotNull(songsRepository);

    final TransactionDemarcation td = new TransactionDemarcation();
    assertNotNull(td);

    try {
      // Start a new transaction
      td.begin(songsRepository.getTransactionManager());
      // Create a new artist
      MutableRepositoryItem artist = songsRepository.createItem("artist");
      artist.setPropertyValue("name", "joe");
      // Persist to the repository
      songsRepository.addItem(artist);
      // Try to get it back from the repository
      String id = artist.getRepositoryId();
      RepositoryItem retrievedArtist = songsRepository.getItem(id, "artist");

      assertEquals(artist, retrievedArtist);
    }
    finally {
      // End the transaction, roll-back to restore original database state
      td.end(true);
    }
  }
}