package atg.test;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author robert
 * 
 */
public class AtgDustTestCaseTest extends TestCase {

  private final AtgDustTestCase atgDustTestCase = new AtgDustTestCase();

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    atgDustTestCase.setConfigPath("target/test-classes/config/");
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
   * Test method for
   * {@link atg.test.AtgDustTestCase#createPropertyFile(java.lang.String, java.lang.Class)}.
   */
  public void testCreatePropertyFile() {
    try {
      atgDustTestCase.createPropertyFile(System.getProperty("java.io.tmpdir")
          + File.separator + "atgdust", new Object().getClass());
    }
    catch (IOException e) {
      fail();
    }
    // assertTrue(new File(
    // "target/test-classes/atg/test/data/target/test-classes/config/tmp/test.properties")
    // .exists());
  }

  // /**
  // * Test method for
  // * {@link atg.test.AtgDustTestCase#createPropertyFiles(java.util.Map)}.
  // */
  // public void testCreatePropertyFiles() {
  // fail("Not yet implemented");
  // }

  // /**
  // * Test method for
  // * {@link atg.test.AtgDustTestCase#prepareDefaultGsaTest(java.io.File,
  // java.lang.String[], java.lang.String)}.
  // */
  // public void testPrepareDefaultGsaTest() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // * {@link atg.test.AtgDustTestCase#prepareGsaTest(java.io.File,
  // java.lang.String[], java.lang.String, java.lang.String, java.lang.String,
  // java.lang.String, java.lang.String, java.lang.String,
  // atg.test.AtgDustTestCase.DbVendor, boolean)}.
  // */
  // public void testPrepareGsaTest() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for
  // * {@link atg.test.AtgDustTestCase#setConfigPath(java.lang.String)}.
  // */
  // public void testSetConfigPath() {
  // fail("Not yet implemented");
  // }
  //
  // /**
  // * Test method for {@link atg.test.AtgDustTestCase#startNucleus()}.
  // */
  // public void testStartNucleus() {
  // fail("Not yet implemented");
  // }

  // /**
  // * Test method for
  // * {@link
  // atg.test.AtgDustTestCase#useExistingPropertyFiles(java.lang.String,
  // java.lang.String[])}.
  // */
  // public void testUseExistingPropertyFiles() {
  // fail("Not yet implemented");
  // }

  /**
   * Test method for {@link atg.test.AtgDustTestCase#tearDown()}.
   */
  public void testTearDown() {
    try {
      atgDustTestCase.tearDown();
    }
    catch (Exception e) {
      fail();
    }
  }

  /**
   * Test method for
   * {@link atg.test.AtgDustTestCase#getService(java.lang.String)}.
   */
  public void testGetService() {
    try {
      atgDustTestCase.createPropertyFile("/tmp/test", new Object().getClass());
    }
    catch (IOException e) {
      fail();
    }
    assertTrue(new File(
        "target/test-classes/atg/test/data/target/test-classes/config/tmp/test.properties")
        .exists());
    assertNotNull(atgDustTestCase.getService("/tmp/test"));

  }
}
