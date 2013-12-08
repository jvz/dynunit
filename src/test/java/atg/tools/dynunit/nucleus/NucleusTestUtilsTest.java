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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * NucleusTestUtilsTest
 *
 * @author adamb
 */
public class NucleusTestUtilsTest {

    /**
     * Tests the addComponent method of NucleusTestUtils to make sure that when a
     * component is added that exact same one
     */
    @Test
    public void testAddComponent()
            throws Exception {
        Nucleus n = null;
        try {
            n = new Nucleus("");
            Object component = "I'm the test component";
            String path = "/foo/Test";
            Object component2 = "I'm the test component too";
            String path2 = "/foo/Test2";
            NucleusTestUtils.addComponent(n, path, component);
            NucleusTestUtils.addComponent(n, path2, component2);
            Object result = n.resolveName(path);
            Object result2 = n.resolveName(path2);
            // Make sure we get something
            assertNotNull(result);
            assertNotNull(result2);
            // Make sure it's the same component we added
            assertEquals(component, result);
            assertEquals(component2, result2);
        } finally {
            if (n != null) {
                n.stopService();
            }
        }
    }

    /**
     * Tests that NucleusTestUtils is able to find
     * the global testconfig directory from
     * a system property.
     */
    @Test
    public void testGetGlobalTestConfig() {
        NucleusTestUtils ntu = new NucleusTestUtils();
        String testConfig = "/foo/baz/testconfig/";
        System.setProperty(NucleusTestUtils.ATG_DUST_TESTCONFIG, testConfig);
        assertNotNull(testConfig);
        assertEquals(testConfig, NucleusTestUtils.getGlobalTestConfig());
    }
}
