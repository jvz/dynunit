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

package com.mycompany;

import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.nucleus.logging.ApplicationLogging;

/**
 * This is an example class created for the purpose of demonstrating how to
 * write test code with DUST.
 * 
 * Original file name src/main/java/com/mycompany/ToBeTested.java. Renamed it so
 * we get consisting test class names.
 * 
 * 
 * 
 * @author adamb
 * @author robert
 * 
 */
public class SimpleComponent extends GenericService {

  public boolean isCleanStart = false;

  // -------------------------
  /**
   * Called when this component is started.
   */
  public void doStartService() throws ServiceException {
    prepare();
  }

  // -------------------------
  /**
   * Called when this component is shut down.
   */
  public void doStopService() throws ServiceException {
    isCleanStart = false;
  }

  // -------------------------
  /**
   * Checks that Nucleus is running, if its not, then throw a ServiceException.
   * If Nucleus is running, the log an info message.
   * 
   * @throws ServiceException
   */
  public void prepare() throws ServiceException {
    Nucleus n = Nucleus.getGlobalNucleus();
    if (n == null) {
      throw new ServiceException("Nucleus is not running.");
    }
    else {
      ((ApplicationLogging) n).logInfo("Prepared.");
    }
    isCleanStart = true;
  }
}
