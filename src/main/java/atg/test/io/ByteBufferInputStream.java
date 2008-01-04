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
package atg.test.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An InputStream which reads from a ByteBuffer
 * 
 * @author Adam Belmont
 * @version $Id:$
 * @see ByteBuffer
 * 
 */
public class ByteBufferInputStream extends InputStream {
//-------------------------------------
  /** Class version string */
  
  public static String CLASS_VERSION =
  "$Id:$";
  
  private ByteBuffer mBuffer = null;

  public ByteBufferInputStream(ByteBuffer pBuffer) {
    mBuffer = pBuffer;
  }

  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }

  public synchronized int read() throws IOException {
    if (!mBuffer.hasRemaining()) {
      return -1;
    }
    return mBuffer.get();
  }

  public synchronized int read(byte[] bytes, int off, int len)
      throws IOException {
    len = Math.min(len, mBuffer.remaining());
    mBuffer.get(bytes, off, len);
    return len > 0 ? len : -1;
  }
}
