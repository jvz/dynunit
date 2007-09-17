package atg.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

/**
 * @author robert
 * 
 */
public class AtgDustTestCaseTest extends TestCase {

  private final AtgDustTestCase atgDustTestCase = new AtgDustTestCase();
  private static final Log log = LogFactory.getLog(AtgDustTestCase.class);

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    atgDustTestCase.setConfigPath("target/test-classes/config/".replace("/",
        File.separator));
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
    assertTrue(new File(
        "target/test-classes/atg/test/data/target/test-classes/config/test.properties"
            .replace("/", File.separator)).exists());
    assertNotNull(atgDustTestCase.getService("test"));

  }
}
