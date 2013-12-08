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

package atg.tools.dynunit.droplet;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.repository.MutableRepository;
import atg.servlet.DynamoHttpServletRequest;
import atg.tools.dynunit.servlet.ServletTestUtils;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;

import static atg.servlet.ServletUtil.setCurrentRequest;
import static atg.tools.dynunit.nucleus.NucleusTestUtils.shutdownNucleus;
import static atg.tools.dynunit.nucleus.NucleusTestUtils.startNucleusWithModules;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests ProfileFormHandler using the config layer from the ATG build.
 *
 * @author adamb
 */
public class ProfileFormHandlerTest {

    private static final String PROFILE_ADAPTER_REPOSITORY_PATH = "/atg/userprofiling/ProfileAdapterRepository";

    private static final Logger logger = getLogger();

    private Nucleus nucleus;

    private DynamoHttpServletRequest previousRequest;

    private final ServletTestUtils servletTestUtils = new ServletTestUtils();

    @Before
    public void setUp()
            throws ServletException {
        logger.info("Starting Nucleus.");

        System.setProperty("derby.locks.deadlockTrace", "true");
        nucleus = startNucleusWithModules(
                new String[]{ "DPS", "DafEar.base" },
                this.getClass(),
                this.getClass().getName(),
                PROFILE_ADAPTER_REPOSITORY_PATH
        );
    }

    @After
    public void tearDown()
            throws IOException, ServiceException {
        if (previousRequest != null) {
            setCurrentRequest(previousRequest);
        }
        logger.info("Stopping Nucleus.");
        if (nucleus != null) {
            shutdownNucleus(nucleus);
        }
    }

    /**
     * TODO: Make it possible to resolve request scoped objects in a test.
     * Ideally we setup a fake request and make all the code "just work".
     *
     * @throws Exception
     */
    @Test
    public void testProfileFormHandler()
            throws Exception {
        MutableRepository par = (MutableRepository) nucleus.resolveName(
                PROFILE_ADAPTER_REPOSITORY_PATH
        );
        assertNotNull(par);
    }

    /**
     * Run a second test to be sure that HSQLDB shuts down
     * properly between tests
     */
    @Test
    public void testProfileFormHandlerAgain()
            throws Exception {
        assertNotNull(nucleus.getCreationFilter());
        Object s = nucleus.resolveName("/atg/scenario/ScenarioManager");
        assertNull(s);

        DynamoHttpServletRequest request = servletTestUtils.createDynamoHttpServletRequestForSession(
                nucleus, "mySessionId", "new"
        );
        previousRequest = setCurrentRequest(request);
        MutableRepository par = (MutableRepository) nucleus.resolveName(
                PROFILE_ADAPTER_REPOSITORY_PATH
        );
        assertNotNull(par);

        assertNotNull(
                "Request component",
                request.resolveName("/atg/userprofiling/ProfileFormHandler")
        );
    }


}
