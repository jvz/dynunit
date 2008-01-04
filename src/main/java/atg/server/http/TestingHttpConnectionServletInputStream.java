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
package atg.server.http;

import java.io.InputStream;

// -------------------------------------
/**
 * A version of HttpConnectionServletInputStream that allows one to specify the
 * content length parameter. This is useful for writing unit tests since the
 * base class does not allow the content length to be set by callers outside of
 * its package.
 * 
 * @author Adam Belmont
 * @version $Id: $
 * @see HttpConnectionServletInputStream
 * 
 */
public class TestingHttpConnectionServletInputStream extends
    HttpConnectionServletInputStream {
  
  public static final String CLASS_VERSION = "$Id:$";
  // -------------------------------------
  /*
   * (non-Javadoc)
   * 
   * @see atg.server.http.HttpConnectionServletInputStream#getRequestContentLength()
   */
  @Override
  public int getRequestContentLength() {
    // TODO Auto-generated method stub
    return super.getRequestContentLength();
  }

  // -------------------------------------
  /*
   * (non-Javadoc)
   * 
   * @see atg.server.http.HttpConnectionServletInputStream#setRequestContentLength(int)
   */
  @Override
  public void setRequestContentLength(int pRequestContentLength) {
    // TODO Auto-generated method stub
    super.setRequestContentLength(pRequestContentLength);
  }

  // -------------------------------------
  /**
   * Constructs a new HttpConnectionServletInputStream that gets its input from
   * the specified InputStream
   * 
   * @param pIn
   *          the InputStream this will use to read its input
   * @param pContentLength
   *          The "content length" size typically sent by an HttpClient
   */
  public TestingHttpConnectionServletInputStream(InputStream pIn,
      int pContentLength) {
    super(pIn);
    setRequestContentLength(pContentLength);
    mIn = pIn;
    // reset the total bytes read by the application from the underlying stream
    mTotalBytesRead = 0;
  }
}
