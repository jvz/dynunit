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

package atg.servlet;

import java.nio.ByteBuffer;
import java.util.Map;



/**
 * Utility methods for setting up Servlet based tests.
 * 
 * @author Adam Belmont
 * 
 */
public class ServletTestUtils {
  
  public static final String CLASS_VERSION = "$Id:$";
  
  // -----------------------------
  /**
   * Creates a new DynamoHtttpServletRequest object that can be used in a unit
   * test. The request is setup with an InputStream, OutputStream and the given
   * set of request parameters.
   * 
   * @param pParmeters
   *          A set of request parameters that this request should initially be
   *          populated with
   * @param pBufferSize
   *          The size in bytes of the backing buffer holding stream data for
   *          this request
   * @param pMethod
   *          The HTTP method for this request. For example GET,POST,PUT
   */
  public TestingDynamoHttpServletRequest createDynamoHttpServletRequest(
      Map<String, String> pParameters, int pBufferSize, String pMethod) {
    GenericHttpServletRequest greq = new GenericHttpServletRequest();
    DynamoHttpServletRequest request = new DynamoHttpServletRequest();
    request.setRequest(greq);
    ByteBuffer buffer = ByteBuffer.allocate(pBufferSize);
    request.setMethod(pMethod);
    setParametersFromMap(request,pParameters);
    return new TestingDynamoHttpServletRequest(request, buffer);
  }

  // -----------------------------
  /**
   * Converts the keys and values from the given map into request
   * parameters on the given request object
   * @param pRequest
   * @param pParameters
   */
  private void setParametersFromMap(DynamoHttpServletRequest pRequest,
      Map<String, String> pParameters) {
    for (String s : pParameters.keySet()) {
      pRequest.setParameter(s, pParameters.get(s));
    }
  }

  // -----------------------------
  /**
   * Creates a new DynamoHtttpServletResponse object that can be used in a unit
   * test.
   *  
   */
  public DynamoHttpServletResponse createDynamoHttpServletResponse() {
    DynamoHttpServletResponse response = new DynamoHttpServletResponse();
    ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
    response.setOutputStream(out);
    return new TestingDynamoHttpServletResponse(response);
  }
}

