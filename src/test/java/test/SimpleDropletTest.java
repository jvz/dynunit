/**
 * Copyright 2008 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletTestUtils;
import atg.servlet.TestingDynamoHttpServletRequest;
import atg.test.AtgDustCase;

import com.mycompany.SimpleDroplet;

/**
 * 
 * Class that hopefully illustrates droplet junit testing. This class uses an
 * instance of atg.servlet.ServletTestUtils to initialize a
 * DynamoHttpServletRequest and DynamoHttpServletResponse pair. These are not
 * fully initialized but they will have working I/O streams and you may set and
 * get parameters.
 * 
 * <br/><br/>Based on {@link AtgDustCase}
 * 
 * 
 * @author robert
 */
public class SimpleDropletTest extends AtgDustCase {
  private SimpleDroplet simpleDroplet;
  @SuppressWarnings("unused")
  private static Logger log = Logger.getLogger(SimpleDropletTest.class);
  ServletTestUtils mServletTestUtils = null;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // We can reuse this instance for all tests
    if (mServletTestUtils == null)
      mServletTestUtils = new ServletTestUtils();
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

  /**
   * Tests droplet executing using ServletTestUtils
   */
  public void testService() {
    assertNotNull(simpleDroplet);
    Map<String, String> params = new HashMap<String, String>();
    // Add some request parameters
    params.put("color", "red");
    params.put(SimpleDroplet.USERNAME,"noonan");
    // let's try 128k, should be more than enough
    int bufferSize = 128 * 1024;
    // Setup request/response pair
    DynamoHttpServletRequest request = mServletTestUtils.createDynamoHttpServletRequest(params, bufferSize, "GET");
    DynamoHttpServletResponse response = mServletTestUtils.createDynamoHttpServletResponse();
    try {
      simpleDroplet.service(request,response);
      // check that the "output" oparam was rendered
      Object outputOparam = request.getLocalParameter("output");
      System.out.println("outputOParam="+outputOparam);
      //assertNotNull(outputOparam);
      // check that the username property on the droplet was set to the incoming http param "username"
      assertEquals("noonan", simpleDroplet.getUsername());
      System.out.println("outputStreamContents="+response.getOutputStream());
    } catch (ServletException e) {
      fail("droplets service method threw unexpected ServletException");
    } catch (IOException e) {
      fail("droplets service method threw unexpected IOException");
    }
  }
  
  /**
   * In this example we execute a POST request to a given DynamoServlet.
   * The servlet reads some data from the client and sets it to a value in a local variable.
   * This demonstrates the use of streams with the testing request and response objects.
   * @throws IOException 
   */
  public void testPostService() throws IOException {
    assertNotNull(simpleDroplet);
    Map<String, String> params = new HashMap<String, String>();
    // Add some request parameters, though they don't get used in this test.
    params.put("color", "red");    
    // let's try 128k, should be more than enough
    int bufferSize = 128 * 1024;
    // Setup request/response pair
    TestingDynamoHttpServletRequest request = mServletTestUtils.createDynamoHttpServletRequest(params, bufferSize, "POST");
    DynamoHttpServletResponse response = mServletTestUtils.createDynamoHttpServletResponse();
    // This time we write the username in the POST data instead
    // We're pretending to be a client such as a browser here.
    OutputStream out = request.getClientOutputStream(false);
    out.write(SimpleDroplet.USERNAME.getBytes());
    out.write("=noonan".getBytes());
    // Ok done writing, get ready for reading.
    request.prepareForRead();
    
    try {
      simpleDroplet.service(request,response);
      // check that the "output" oparam was rendered
      Object outputOparam = request.getLocalParameter("output");
      //assertNotNull(outputOparam);
      // check that the username property on the droplet was set to the incoming http param "username"
      assertEquals("noonan", simpleDroplet.getUsernameFromInputStream());
    } catch (ServletException e) {
      fail("droplets service method threw unexpected ServletException");
    } catch (IOException e) {
      fail("droplets service method threw unexpected IOException");
    }
  }
}
