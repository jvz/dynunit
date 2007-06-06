package test;

import java.io.File;
import java.util.Properties;

import atg.adapter.gsa.GSARepository;
import atg.adapter.gsa.GSATest;
import atg.adapter.gsa.GSATestUtils;
import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.nucleus.ConfigurationFileSystems;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryItem;
import atg.test.DBUtils;
import atg.vfs.VirtualFile;
import atg.vfs.VirtualFileSystem;
import atg.vfs.VirtualPath;

/**
 * This test starts a repository, adds an item to that repository, then shuts
 * down. The repository is started up against an in-memory Hypersonic Database.
 */
public class SimpleGSATest extends GSATest {

  public void testSimple() throws Exception {

    // setup the repository
    File configpath = getConfigpath();
    
    // Define the path to our repository definition file called
    // "simpleRepository.xml"
    String[] definitionFiles = { StringUtils.replace(getClass().getPackage()
        .getName(), '.', "/")
        + "/simpleRepository.xml" };
    System.out.println(" definitionFile[0]=" + definitionFiles[0]);
    // Use the DBUtils utility class to get JDBC properties for an in memory
    // HSQL DB called "testdb".
    Properties props = DBUtils.getHSQLDBInMemoryDBConnection("testdb");

    // Start up our database
    DBUtils db = initDB(props);

    boolean rollback = true;

    // Setup our testing configpath
    GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(configpath,
        "/SimpleRepository", definitionFiles, props, null, null, null, true);

    // Start Nucleus
    Nucleus n = startNucleus(configpath);

    TransactionDemarcation td = new TransactionDemarcation();
    MutableRepository r = (MutableRepository) n
        .resolveName("/SimpleRepository");

    try {
      // Start a new transaction
      td.begin(((GSARepository) r).getTransactionManager());
      // Create the item
      MutableRepositoryItem item = r.createItem("simpleItem");
      item.setPropertyValue("name", "simpleName");
      // Persist to the repository
      r.addItem(item);
      // Try to get it back from the repository
      String id = item.getRepositoryId();
      RepositoryItem item2 = r.getItem(id,"simpleItem");
      assertNotNull(
          " We did not get back the item just created from the repository.",
          item2);
      rollback = false;
    } finally {
      // End the transaction, rollback on error
      if (td != null)
        td.end(rollback);
      // shut down Nucleus
      n.stopService();
      // Shut down HSQLDB
      db.shutdown();
    }
  }
}