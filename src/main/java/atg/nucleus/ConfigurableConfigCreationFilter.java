/*<ATGCOPYRIGHT>
 * Copyright (C) 1997-2009 Art Technology Group, Inc.
 * All Rights Reserved.  No use, copying or distribution ofthis
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
 * Dynamo is a trademark of Art Technology Group, Inc.
 </ATGCOPYRIGHT>*/

package atg.nucleus;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * Implementation of ConfigCreationFilter interface that can be configured to prevent
 * Nucleus from starting up specified components or all components in specified config subtrees.
 *
 * @author Clare Rabinow
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/nucleus/ConfigurableConfigCreationFilter.java#3 $$Change: 557551 $
 * @updated $DateTime: 2009/10/20 16:51:57 $$Author: cmore $
 */
public class ConfigurableConfigCreationFilter extends GenericService implements ConfigCreationFilter
{
  //-------------------------------------
  /** Class version string */

  public static String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/nucleus/ConfigurableConfigCreationFilter.java#3 $$Change: 557551 $";

  //-------------------------------------
  // property: droppedComponents
  //-------------------------------------

  /** Our ordered set of dropped components. Saved as a set
   * to be more efficient for lookups.
   */
  protected Set<String> mDroppedComponents =
    new LinkedHashSet<String>();
  
  /** Get list of component paths to filter out. */
  public String[] getDroppedComponents()
  {
    return mDroppedComponents.toArray(new String[0]);
  }

  /** Set the list of component paths to filter out. */  
  public void setDroppedComponents(String[] pDroppedComponents)
  {
    mDroppedComponents.clear();
    if (pDroppedComponents != null) {
      mDroppedComponents.addAll(Arrays.asList(pDroppedComponents));
    }
  }

  //-------------------------------------
  // property: droppedPaths
  //-------------------------------------
  protected String [] mDroppedPaths;

  /** Get the list of component prefixes to filter out. */
  public String[] getDroppedPaths() {
    return mDroppedPaths;
  }

  /** Set the list of component prefixes to filter out. */  
  public void setDroppedPaths(String[] pDroppedPaths) {
    mDroppedPaths = pDroppedPaths;
  }

  //-------------------------------------
  // property: nucleus (part of ConfigCreationFilter interface)
  //-------------------------------------
  public void setNucleus(Nucleus pNucleus)
  {
    // not needed for anything
  }

  //-------------------------------------
  /**
   * Determine whether the particular component
   * configuration with pConfig should be created.
   * Returns false if the configuration's service name starts with one
   * of the droppedPaths or if the name appears in droppedComponents.
   * 
   * @return whether the component should be created.
   */
  public boolean shouldCreateComponent(Configuration pConfig)
  {

    if (mDroppedPaths != null) {
      for (String strDroppedPath : mDroppedPaths) {
        if (pConfig.getServiceName().startsWith(strDroppedPath)) {
          vlogDebug("shouldCreateComponent({0}) returning {1}",
                    pConfig.getServiceName(), false);
          return false;
        }
      }
    }
    
    if (mDroppedComponents.contains(pConfig.getServiceName())) {
      vlogDebug("shouldCreateComponent({0}) returning {1}",
                pConfig.getServiceName(), false);
      return false;
    }
    vlogDebug("shouldCreateComponent({0}) returning {1}",
              pConfig.getServiceName(), true);
    return true;
  }
}
