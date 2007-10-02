/**
 * Copyright 2007 ATG DUST Project
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

package atg.adapter.gsa;

import atg.core.exception.ContainerException;

/**
 * An exception created by the SQLProcessorEngine.
 * Class derived from IDGeneratorException by mgk.
 *
 * @author mrf
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/test/apiauto/util/SQLProcessorException.java#1 $
 **/
public class SQLProcessorException
extends ContainerException
{
  /**
   * Eclipse generated
   */
  private static final long serialVersionUID = 2541234526510136456L;
  //-------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION =
  "$Id: //test/UnitTests/base/main/src/Java/atg/test/apiauto/util/SQLProcessorException.java#1 $";

  //-------------------------------------
  /**
   * Construct an SQLProcessorException
   * @param pMessage message
   **/
  public SQLProcessorException(String pMessage)
  {
    super(pMessage);
  }

  //-------------------------------------
  /**
   * Construct an SQLProcessorException
   * @param pSourceException source exception
   * @param pMessage message
   **/
  public SQLProcessorException(Throwable pSourceException)
  {
    super(pSourceException);
  }

  //-------------------------------------
  /**
   * Construct an SQLProcessorException
   * @param pSourceException source exception
   * @param pMessage message
   **/
  public SQLProcessorException(String pMessage, Throwable pSourceException)
  {
    super(pMessage, pSourceException);
  }

} // end of class SQLProcessorException
