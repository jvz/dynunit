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

package atg.nucleus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import atg.core.util.StringUtils;
import atg.nucleus.naming.ComponentName;
import atg.nucleus.servlet.NucleusServlet;

/**
 * NucleusTestUtils
 * 
 * Utilities for creating a Nucleus that can be used by test code.
 * 
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/nucleus/NucleusTestUtils.java#7 $
 * 
 * This class contains some utility methods to make it faster
 * to write a unit test that needs to resolve componants against Nucleus.
 *
 */
public class NucleusTestUtils {
  
  /**
   * Creates an Initial.properties file
   * pRoot The root directory of the configpath
   * pInitialServices A list of initial services
   */
  public static File createInitial(File pRoot,List pInitialServices) throws IOException {
    Properties prop = new Properties();
    Iterator iter = pInitialServices.iterator();
    StringBuffer services = new StringBuffer();
    while (iter.hasNext()) {
      if (services.length() != 0) services.append(",");
      services.append((String)iter.next());
    }
    prop.put("initialServices",services.toString());
    return NucleusTestUtils.createProperties("Initial", new File(pRoot.getAbsolutePath()),"atg.nucleus.InitialService", prop);
  }
  
  /**
   * Allows the absoluteName of the given service to be explicitly defined.
   * Normally this is determined by the object's location in the Nucleus hierarchy.
   * For test items that are not really bound to Nucleus, it's convenient to just give
   * it an absolute name rather than going through the whole configuration and binding process.
   * @param pName
   * @param pService
   */
  public static void setAbsoluteName(String pName, GenericService pService) {
    pService.mAbsoluteName = pName;
  }
  // ---------------------
  /**
   * Adds the given object, pComponent to Nucleus, pNucleus at the path given
   * by pComponentPath.
   * @param pNucleus
   * @param pComponentPath
   * @param pComponent
   */
  public static void addComponent(
    Nucleus pNucleus,
    String pComponentPath,
    Object pComponent) {
    // make sure it's not already there
    if (pNucleus.resolveName(pComponentPath) != null)
      return;
    ComponentName name = ComponentName.getComponentName(pComponentPath);
    ComponentName[] subNames = name.getSubNames();
    GenericContext[] contexts = new GenericContext[subNames.length - 1];
    contexts[0] = pNucleus;
    for (int i = 1; i < subNames.length - 1; i++) {
      contexts[i] = new GenericContext();
      // Make sure it's not there
      GenericContext tmpContext =
        (GenericContext) contexts[i - 1].getElement(subNames[i].getName());
      if (tmpContext == null)
        contexts[i - 1].putElement(subNames[i].getName(), contexts[i]);
      else
        contexts[i] = tmpContext;
    }
    contexts[contexts.length
      - 1].putElement(subNames[subNames.length - 1].getName(), pComponent);
  }
  
  /**
   * Creates a .properties file
   * @param pComponentName
   * @param pConfigDir
   * @param pClass
   * @param pProps
   * @return
   * @throws IOException
   */
  public static File createProperties(String pComponentName, File pConfigDir, String pClass, Properties pProps)
  throws IOException {
  File prop;
  if (pConfigDir == null)
    prop = new File("./" + pComponentName + ".properties");
  else {
    pConfigDir.mkdirs();
    prop = new File(pConfigDir, pComponentName + ".properties");
    new File(prop.getParent()).mkdirs();
  }
  
  if (prop.exists()) prop.delete();
  prop.createNewFile();
  FileWriter fw = new FileWriter(prop);
  String classLine = "$class=" + pClass + "\n";
  try {
    if (pClass != null) fw.write(classLine);
    if (pProps != null) {
      Iterator iter = pProps.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        String thisLine = key + "=" + StringUtils.replace(pProps.getProperty(key),'\\', "\\\\") + "\n";
        fw.write(thisLine);
      }
    }
  }
  finally {
    fw.flush();
    fw.close();
  }
  return prop;
}
  /**
   * Starts Nucleus using the given config directory
   * @param configpath
   * @return
   */
  public static  Nucleus startNucleus(File configpath) {
    System.setProperty("atg.dynamo.license.read", "true");
    System.setProperty("atg.license.read", "true");
    String[] configpathStr = { configpath.getAbsolutePath() };
    NucleusServlet.addNamingFactoriesAndProtocolHandlers();
    Nucleus n = Nucleus.startNucleus(configpathStr);
    return n;
  }
  
  static Map sConfigDir = new HashMap();
  /**
   * A convenience method for returning the configpath for a test.
   * pConfigDirectory is the top level name to be used for the configpath.
   * 
   * @return
   */
  public static File getConfigpath(Class pClass,String pConfigDirectory) {
    if (sConfigDir.get(pConfigDirectory) == null) {
      String configdirname = "config";
      String packageName = StringUtils.replace(pClass.getPackage()
          .getName(), '.', "/");
      if (pConfigDirectory != null)
        configdirname = pConfigDirectory;

      String configFolder = packageName + "/data/" + configdirname;

      URL dataURL = pClass.getClassLoader().getResource(configFolder);
      // Mkdir
      if (dataURL == null) {
        URL root = pClass.getClassLoader().getResource(packageName);

        File f = new File(root.getFile());
        File f2 = new File(f, "/data/" + configdirname);
        f2.mkdirs();
        dataURL = pClass.getClassLoader().getResource(configFolder);
      }

      sConfigDir.put(pConfigDirectory ,new File(dataURL.getFile()));
    }
    System.setProperty("atg.configpath",((File) sConfigDir.get(pConfigDirectory)).getAbsolutePath());
    return (File) sConfigDir.get(pConfigDirectory);
  }
}
