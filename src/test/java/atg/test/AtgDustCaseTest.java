/**
 * 
 */
package atg.test;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author robert
 * 
 */
public class AtgDustCaseTest extends TestCase {

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for
   * {@link atg.test.AtgDustCase#createPropertyFile(java.lang.String, java.lang.String, java.lang.Class)}.
   */
  public void testCreatePropertyFile() {
    try {
      AtgDustCase atgCase = new AtgDustCase();
      atgCase.setConfigurationLocation("src/test/resources/config");

      atgCase.createPropertyFile("target/test-classes/config",
          "/some/component/impl", Object.class);

    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
