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

package test;

import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.repository.MutableRepository;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletTestUtils;
import atg.servlet.ServletUtil;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Tests ProfileFormHandler using the config layer from the ATG build.
 *
 * @author adamb
 */
public class ProfileFormHandlerTest
        extends TestCase {

    private static final String PROFILE_ADAPTER_REPOSITORY_PATH = "/atg/userprofiling/ProfileAdapterRepository";

    private static final Logger logger = LogManager.getLogger();

    private Nucleus mNucleus = null;

    private final ServletTestUtils mServletTestUtils = new ServletTestUtils();

    // ------------------------------------

    /**
     * Starts Nucleus. Fails the test if there is a problem starting Nucleus.
     */
    @Override
    public void setUp() {
        logger.info("Starting Nucleus.");
        try {
            System.setProperty("derby.locks.deadlockTrace", "true");
            mNucleus = NucleusTestUtils.startNucleusWithModules(
                    new String[] { "DPS", "DafEar.base" },
                    this.getClass(),
                    this.getClass().getName(),
                    PROFILE_ADAPTER_REPOSITORY_PATH
            );
        } catch ( ServletException e ) {
            logger.catching(e);
            fail(e.getMessage());
        }

    }

    // ------------------------------------

    /**
     * If there is a running Nucleus, this method shuts it down.
     * The test will fail if there is an error while shutting down Nucleus.
     */
    @Override
    public void tearDown() {
        logger.info("Stopping Nucleus.");
        if ( mNucleus != null ) {
            try {
                NucleusTestUtils.shutdownNucleus(mNucleus);
            } catch ( ServiceException e ) {
                logger.catching(e);
                fail(e.getMessage());
            } catch ( IOException e ) {
                logger.catching(e);
                fail(e.getMessage());
            }
        }
    }

    /**
     * TODO: Make it possible to resolve request scoped objects in a test.
     * Ideally we setup a fake request and make all the code "just work".
     *
     * @throws Exception
     */
    public void testProfileFormHandler()
            throws Exception {
        MutableRepository par = (MutableRepository) mNucleus.resolveName(
                PROFILE_ADAPTER_REPOSITORY_PATH
        );
        assertNotNull(par);
    }

    /**
     * Run a second test to be sure that HSQLDB shuts down
     * properly between tests
     */
    public void testProfileFormHandlerAgain()
            throws Exception {
        assertNotNull(mNucleus.getCreationFilter());
        Object s = mNucleus.resolveName("/atg/scenario/ScenarioManager");
        assertNull(s);

        DynamoHttpServletRequest requestOld = null;
        try {
            DynamoHttpServletRequest request = mServletTestUtils.createDynamoHttpServletRequestForSession(
                    mNucleus, "mySessionId", "new"
            );
            requestOld = ServletUtil.setCurrentRequest(request);
            MutableRepository par = (MutableRepository) mNucleus.resolveName(
                    PROFILE_ADAPTER_REPOSITORY_PATH
            );
            assertNotNull(par);

            assertNotNull(
                    "Request component",
                    request.resolveName("/atg/userprofiling/ProfileFormHandler")
            );
        } finally {
            ServletUtil.setCurrentRequest(requestOld);
        }
    }


}
