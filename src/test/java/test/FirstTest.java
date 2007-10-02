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

package test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;

import com.mycompany.ToBeTested;

public class FirstTest extends TestCase {
  
  /**
   * Start up a nucleus given a local test "configpath".
   * In that configpath is a .properties file for our TestComponent
   * @throws Exception
   */
  public void testComponentStartup() throws Exception {
    File configpath = NucleusTestUtils.getConfigpath(this.getClass(),"config");
    // Put test .properties file in configpath path
    Properties props = new Properties();
    File propFile = NucleusTestUtils.createProperties("test/TestComponent", configpath, "com.mycompany.ToBeTested", props);
    propFile.deleteOnExit();
    List<String> initial = new ArrayList<String>();
    initial.add("/test/TestComponent");
    NucleusTestUtils.createInitial(configpath, initial);
    Nucleus n = NucleusTestUtils.startNucleus(configpath);
    ToBeTested testComponent = null;
    try {
      testComponent = (ToBeTested) n.resolveName("/test/TestComponent");
      assertNotNull("Could not resolve test componet",testComponent);
      assertTrue("Test component did not start up cleanly.",testComponent.mCleanStart);
      
    } finally {
      n.stopService();
      assertNotNull(testComponent);
      assertFalse("Test component did not shut down cleanly.",testComponent.mCleanStart);
      testComponent = null;
    }
    
  }

}
