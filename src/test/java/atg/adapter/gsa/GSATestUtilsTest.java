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

package atg.adapter.gsa;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Properties;

/**
 * Unit Test for the GSATestUtils class.
 *
 * @author adamb
 * @version $Revision: #4 $
 */
public class GSATestUtilsTest
        extends TestCase {

    private static final Logger log = LogManager.getLogger(GSATestUtilsTest.class);

    public static void main(String[] args) {
        junit.textui.TestRunner.run(GSATestUtilsTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp()
            throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown()
            throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for GSATestUtilsTest.
     *
     * @param arg0
     */
    public GSATestUtilsTest(String arg0) {
        super(arg0);
    }

    public void testInitializeMinimalConfigpath()
            throws Exception {
        String[] defFiles = {
                "atg/adapter/gsa/rep1.xml", "atg/adapter/gsa/rep2.xml"
        };

        File root = new File("target/test-classes/".replace("/", File.separator));
        File propFile = new File(
                root.getAbsolutePath() + "/foo/bar/Repository.properties"
        );
        if ( propFile.exists() ) {
            propFile.delete();
        }
        assertFalse(propFile.exists());
        Properties props = new Properties();
        props.put("driver", "org.hsqldb.jdbcDriver");
        props.put("URL", "jdbc:hsqldb:mem:testdb");
        props.put("user", "sa");
        props.put("password", "");
        GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(
                root, "/foo/bar/Repository", defFiles, props, null, null, null
        );
        assertTrue(propFile.exists());
        log.info(propFile.getAbsolutePath());
        GSATestUtils.getGSATestUtils().cleanup();

    }

}
