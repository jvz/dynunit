/*<ATGCOPYRIGHT>
 * Copyright (C) 1997-2000 Art Technology Group, Inc.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Art Technology Group.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES 
 * ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, 
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * "Dynamo" is a trademark of Art Technology Group, Inc.
 </ATGCOPYRIGHT>*/

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
