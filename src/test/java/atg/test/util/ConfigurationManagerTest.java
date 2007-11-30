package atg.test.util;

import junit.framework.TestCase;

public class ConfigurationManagerTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConfigurationManager() {
    try {
      new ConfigurationManager(true, null, true);
      fail("Should have thrown an UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

}
