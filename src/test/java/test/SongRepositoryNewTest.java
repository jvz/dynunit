package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.adapter.gsa.GSARepository;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.test.AtgDustCase;

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
public class SongRepositoryNewTest extends AtgDustCase {

  @SuppressWarnings("unused")
  private static final Log log = LogFactory.getLog(SongRepositoryNewTest.class);

  @Override
  public void setUp() throws Exception {
    super.setUp();

    // make sure all needed files are at the config location.
    // "target/test-classes/config" is then prompoted to the configuration
    // directory.
    copyConfigurationFiles(new String[] { "src/test/resources/config".replace(
        "/", File.separator) }, "target/test-classes/config".replace("/",
        File.separator), new String[] { ".svn" });

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

    // The actual test is quite generic. The only difference is the way the
    // repository is prepared by the prepareRepositoryTest method

    prepareRepositoryTest(new String[] { "/GettingStarted/songs.xml" },
        "/GettingStarted/SongsRepository");

    songsRepositoryTest();
  }

  /**
   * Example test with existing Database. This test is disabled by default (set
   * to false/or not set in the env.properties) because the MySQL JDBC drivers
   * (and the env.properties is configured to use mysql) are not included in the
   * atg dust package.
   * 
   * To make use of this test, install a mysql-connector-java (mysql jdbc
   * driver) into your .m2/repository, un-comment the mysql dependency in the
   * pom.xml. Test data can be found in
   * src/test/resources/config/GettingStarted/songs-data.xml.
   * 
   * 
   * @throws Exception
   */
  public void testWithExistingDb() throws Exception {

    Properties properties = new Properties();
    properties.load(new FileInputStream("src/test/resources/env.properties"));

    // a mechanism to disbale/enable the repository test against an existing
    // database
    if (properties.getProperty("enabled") == null
        || properties.getProperty("enabled").equalsIgnoreCase("false")) {
      return;
    }

    // The actual test is quite generic. The only difference is the way the
    // repository is prepared by the prepareRepositoryTest method

    prepareRepositoryTest(new String[] { "/GettingStarted/songs.xml" },
        "/GettingStarted/SongsRepository", properties, false);

    songsRepositoryTest();
  }

  private void songsRepositoryTest() throws TransactionDemarcationException,
      RepositoryException, IOException {
    GSARepository songsRepository = (GSARepository) resolveNucleusComponent("/GettingStarted/SongsRepository");
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
      td.end(false);
    }
  }

}