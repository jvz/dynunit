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

package atg.tools.dynunit.nucleus;

import atg.nucleus.Nucleus;
import com.mycompany.SimpleComponent;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FirstTest {

    /**
     * Start up a nucleus given a local test "configpath". In that configpath is a
     * .properties file for our TestComponent
     *
     * @throws Exception
     */
    @Test
    public void testComponentStartup()
            throws Exception {
        File configpath = NucleusUtils.getConfigPath(this.getClass(), "config");
        // Put test .properties file in configpath path
        Properties props = new Properties();
        File propFile = NucleusUtils.createProperties(
                "test/SimpleComponentGlobalScope",
                configpath,
                "com.mycompany.SimpleComponent",
                props
        );
        propFile.deleteOnExit();
        List<String> initial = new ArrayList<String>();
        initial.add("/test/SimpleComponentGlobalScope");
        NucleusUtils.createInitial(configpath, initial);
        Nucleus n = NucleusUtils.startNucleus(configpath);
        SimpleComponent testComponent = null;
        try {
            testComponent = (SimpleComponent) n.resolveName("/test/SimpleComponentGlobalScope");
            assertNotNull("Could not resolve test component", testComponent);
            assertTrue(
                    "Test component did not start up cleanly.", testComponent.isCleanStart
            );

        } finally {
            n.stopService();
            assertNotNull(testComponent);
            assertFalse(
                    "Test component did not shut down cleanly.", testComponent.isCleanStart
            );
            testComponent = null;
        }

    }

}
