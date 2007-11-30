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
  private AtgDustCase atgCase = new AtgDustCase();

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();

    atgCase.setConfigurationLocation("src/test/resources/config");
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

      atgCase.createPropertyFile("target/test-classes/config",
          "/some/component/impl", Object.class);

    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testGetMoreCoverage() {
    try {
      atgCase.tearDown();
    }
    catch (Exception e) {
      fail("Previous call should not have triggerd an Exception");
    }

    try {
      atgCase.resolveNucleusComponent("bla");
      atgCase.resolveNucleusComponent("bla");

      // TODO: there must be a way to really test this
      atgCase.setDebug(true);
    }
    catch (Exception e) {
      fail("Previous call should not have triggerd an Exception");
    }

  }
}
