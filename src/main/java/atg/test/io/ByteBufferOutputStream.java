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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * An output stream which allows writing to a ByteBuffer
 * 
 * @author Adam Belmont
 * @version $Id:$
 * @see ByteBuffer
 */
public class ByteBufferOutputStream extends OutputStream {
  private ByteBuffer mBuffer = null;

  /**
   * Creates a new ByteBufferOutputStream which writes into the given
   * ByteBuffer. Note that you should class ByteBuffer.flip() after writing to
   * this stream in order to make the buffer available for reading with a
   * ByteBufferInputStream.
   * 
   * @param pBuffer
   * @see ByteBufferInputStream
   */
  public ByteBufferOutputStream(ByteBuffer pBuffer) {
    mBuffer = pBuffer;
  }

  @Override
  public synchronized void write(int b) throws IOException {
    mBuffer.put((byte) b);
  }

  @Override
  public synchronized void write(byte[] bytes, int off, int len)
      throws IOException {
    mBuffer.put(bytes, off, len);
  }
}