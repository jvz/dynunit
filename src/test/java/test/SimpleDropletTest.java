/**
 *
 */
package test;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.servlet.MockDynamoHttpServletRequest;
import atg.servlet.MockDynamoHttpServletResponse;
import atg.test.AtgDustCase;

import com.mycompany.SimpleDroplet;

/**
 * 
 * Class that hopefully illustrates droplet junit testing.
 * 
 * <br/><br/>Based on {@link AtgDustCase}
 * 
 * 
 * @author robert
 */
public class SimpleDropletTest extends AtgDustCase {

  private SimpleDroplet simpleDroplet;

  protected final Log log = LogFactory.getLog(SimpleDropletTest.class);

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    copyConfigurationFiles(new String[] { "src/test/resources/config".replace(
        "/", File.separator) }, "target/test-classes/config".replace("/",
        File.separator), ".svn");

    simpleDroplet = (SimpleDroplet) resolveNucleusComponent("/test/SimpleDroplet");

  }

  /*
   * (non-Javadoc)
   * 
   * @see atg.test.AtgDustCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testService() {
    assertNotNull(simpleDroplet);

    final MockDynamoHttpServletRequest mockRequest = new MockDynamoHttpServletRequest();
    final MockDynamoHttpServletResponse mockResponse = new MockDynamoHttpServletResponse();

    try {
      simpleDroplet.service(mockRequest, mockResponse);
      assertTrue(mockRequest.getServiceParameters().contains("some value"));
    }
    catch (ServletException e) {
      fail("droplets service method threw unexpected ServletException");
    }
    catch (IOException e) {
      fail("droplets service method threw unexpected IOException");
    }
  }

}
