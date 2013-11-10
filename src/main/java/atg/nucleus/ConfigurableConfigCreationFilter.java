/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.nucleus;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Implementation of ConfigCreationFilter interface that can be configured to prevent
 * Nucleus from starting up specified components or all components in specified config subtrees.
 *
 * @author Clare Rabinow
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/nucleus/ConfigurableConfigCreationFilter.java#3
 *          $$Change: 557551 $
 */
public class ConfigurableConfigCreationFilter
        extends GenericService
        implements ConfigCreationFilter {
    //-------------------------------------

    /**
     * Class version string
     */

    public static String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/nucleus/ConfigurableConfigCreationFilter.java#3 $$Change: 557551 $";

    //-------------------------------------
    // property: droppedComponents
    //-------------------------------------

    /**
     * Our ordered set of dropped components. Saved as a set
     * to be more efficient for lookups.
     */
    private Set<String> mDroppedComponents = new LinkedHashSet<String>();

    /**
     * Get list of component paths to filter out.
     */
    public String[] getDroppedComponents() {
        return mDroppedComponents.toArray(new String[0]);
    }

    /**
     * Set the list of component paths to filter out.
     */
    public void setDroppedComponents(String[] pDroppedComponents) {
        mDroppedComponents.clear();
        if ( pDroppedComponents != null ) {
            mDroppedComponents.addAll(Arrays.asList(pDroppedComponents));
        }
    }

    //-------------------------------------
    // property: droppedPaths
    //-------------------------------------
    private String[] mDroppedPaths;

    /**
     * Get the list of component prefixes to filter out.
     */
    public String[] getDroppedPaths() {
        return mDroppedPaths;
    }

    /**
     * Set the list of component prefixes to filter out.
     */
    public void setDroppedPaths(String[] pDroppedPaths) {
        mDroppedPaths = pDroppedPaths;
    }

    //-------------------------------------
    // property: nucleus (part of ConfigCreationFilter interface)
    //-------------------------------------
    public void setNucleus(Nucleus pNucleus) {
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
    public boolean shouldCreateComponent(Configuration pConfig) {

        if ( mDroppedPaths != null ) {
            for ( String strDroppedPath : mDroppedPaths ) {
                if ( pConfig.getServiceName().startsWith(strDroppedPath) ) {
                    vlogDebug(
                            "shouldCreateComponent({0}) returning {1}",
                            pConfig.getServiceName(),
                            false
                    );
                    return false;
                }
            }
        }

        if ( mDroppedComponents.contains(pConfig.getServiceName()) ) {
            vlogDebug(
                    "shouldCreateComponent({0}) returning {1}", pConfig.getServiceName(), false
            );
            return false;
        }
        vlogDebug(
                "shouldCreateComponent({0}) returning {1}", pConfig.getServiceName(), true
        );
        return true;
    }
}
