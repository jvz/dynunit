/**
 * Copyright 2008 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package test;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import atg.servlet.ServletTestUtils;
import atg.servlet.TestingDynamoHttpServletRequest;
import atg.servlet.TestingDynamoHttpServletResponse;
import atg.test.AtgDustCase;

/**
 * @author adamb
 * 
 */
public class SimpleFormHandlerTest extends AtgDustCase {

  @SuppressWarnings("unused")
  private static Logger log = Logger.getLogger(SimpleFormHandlerTest.class);
  private ServletTestUtils mServletTestUtils;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // We can reuse this instance for all tests
    if (mServletTestUtils == null) mServletTestUtils = new ServletTestUtils();
    copyConfigurationFiles(new String[] {
      "src/test/resources/" + this.getClass().getSimpleName() + "/config".replace("/", File.separator)
    }, "target/test-classes/" + this.getClass().getSimpleName() + "/config".replace("/", File.separator),
        ".svn");
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
   * Resolves a form handler and invokes it's handleRedirectMethod.
   * 
   * @throws Exception
   */
  public void testSimpleFormHandler() throws Exception {
    SimpleFormHandler simpleFormHandler = (SimpleFormHandler) resolveNucleusComponent("/atg/test/SimpleFormHandler");
    assertNotNull(simpleFormHandler);
    Map<String, String> params = new HashMap<String, String>();
    // Add a request parameter
    String redirectPage = "/success.jsp";
    params.put(SimpleFormHandler.REDIRECT_URL_PARAM_NAME,redirectPage);
    // let's try 128k, should be more than enough
    int bufferSize = 128 * 1024;
    // Setup request/response pair
    TestingDynamoHttpServletRequest request = mServletTestUtils.createDynamoHttpServletRequest(params, bufferSize, "GET");
    TestingDynamoHttpServletResponse response = mServletTestUtils.createDynamoHttpServletResponse(request);
    request.prepareForRead();
    // invoke handleRedirect
    assertFalse(simpleFormHandler.handleRedirect(request,response));    
  }

}
