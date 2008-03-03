/**
 * 
 */
package atg.test;

import java.io.File;
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
      e.printStackTrace();
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
      atgCase.copyConfigurationFiles(new String[] { "src/test/resources/config"
          .replace("/", File.separator) }, "target/test-classes/config"
          .replace("/", File.separator), ".svn");

      // starts nucleus and resolves component (which can't be resolved because
      // it's non existing)
      assertNull(atgCase.resolveNucleusComponent("bla"));

      // just resolves the component (which can't be resolved because
      // it's non existing)
      assertNull(atgCase.resolveNucleusComponent("bla"));

      // TODO: there must be a way to really test this
      atgCase.setDebug(true);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("Previous call should not have triggerd an Exception");
    }

  }

  public void testCopyConfigurationFiles() throws Exception {
    atgCase.copyConfigurationFiles(
        new String[] { "/Users/robert/tmp/back_ex/atgdust-lots-of-props",  "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2" },
        "/tmp/atgdust", "excludes");
    atgCase.copyConfigurationFiles(
        new String[] { "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp1",  "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2" },
        "/tmp/atgdust", "excludes");
    atgCase.copyConfigurationFiles(
        new String[] { "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2",  "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2" },
        "/tmp/atgdust", "excludes");
  }
}
