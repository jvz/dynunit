package atg.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.nucleus.NucleusTestUtils;

import junit.framework.TestCase;

/**
 * @author robert
 * 
 */
public class AtgDustTestCaseTest extends TestCase {

  private final AtgDustTestCase atgDustTestCase = new AtgDustTestCase();
  private static final Log log = LogFactory.getLog(AtgDustTestCase.class);
  private String configPathString = "target/test-classes/config/";

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    atgDustTestCase.setConfigPath(configPathString);
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    atgDustTestCase.tearDown();
  }

  /**
   * Test method for {@link atg.test.AtgDustTestCase#tearDown()}.
   */
  public void testTearDown() {
    try {
      atgDustTestCase.tearDown();
    }
    catch (Exception e) {
      log.error("Error: ", e);
      fail();
    }
  }

  /**
   * Test method for
   * {@link atg.test.AtgDustTestCase#getService(java.lang.String)}.
   */
  public void testGetService() {
    try {
      atgDustTestCase.createPropertyFile("test", new Object().getClass());
    }
    catch (IOException e) {
      fail();
      log.error("Error: ", e);
    }    
    File configpath = NucleusTestUtils.getConfigpath(this.getClass(), configPathString);
    File f = new File(configpath,"test.properties");
    assertTrue("Couldn't find " + f.getAbsolutePath(),
        f.exists());
    assertNotNull(atgDustTestCase.getService("test"));

  }
}
