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
import atg.nucleus.ServiceException;
import atg.repository.MutableRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.FileNotFoundException;
import java.io.IOException;

import static atg.tools.dynunit.nucleus.NucleusUtils.startNucleusWithModules;
import static atg.tools.dynunit.nucleus.NucleusUtils.stopNucleus;
import static org.junit.Assert.assertNotNull;

/**
 * This test is an example showing how to startup a Nucleus instance which resolves its
 * configuration path (CONFIGPATH)
 * from a set of modules within an ATG installation.
 * This saves the test author from having to maintain separate copies of files which exist in the
 * CONFIGPATH of a given module.
 * The location of DYNAMO_HOME is determined by checking the following items in order for the first
 * non-null value:
 * <ul>
 * <li>System Property "atg.dynamo.root"
 * <li>Environment Variable "DYNAMO_ROOT"
 * <li>System Property "atg.dynamo.home"
 * <li>Environment Variable "DYNAMO_HOME"
 * </ul>
 *
 * @author adamb
 */
public class StartWithModulesTest {

    private static final Logger logger = LogManager.getLogger();

    @Nullable
    private Nucleus mNucleus = null;

    // ------------------------------------

    /**
     * Starts Nucleus. Fails the test if there is a problem starting Nucleus.
     */
    @Before
    public void setUp()
            throws ServletException, FileNotFoundException {
        logger.info("Starting Nucleus.");
        System.setProperty("derby.locks.deadlockTrace", "true");
        mNucleus = startNucleusWithModules(
                new String[]{ "DAF.Deployment", "DPS" },
                this.getClass(),
                this.getClass().getName(),
                "/atg/deployment/DeploymentRepository"
        );
    }

    // ------------------------------------

    /**
     * If there is a running Nucleus, this method shuts it down.
     * The test will fail if there is an error while shutting down Nucleus.
     */
    @After
    public void tearDown()
            throws IOException, ServiceException {
        logger.info("Stopping Nucleus.");
        if (mNucleus != null) {
            stopNucleus(mNucleus);
        }
    }

    // ----------------------------------------

    /**
     * Resolves a component that is defined within the ATG platform and is not specifically
     * part of this test's configpath.
     * Confirms that Nucleus can start given a set of modules and a properly set DYNAMO_HOME
     * environment variable. (ex: DYNAMO_HOME=/home/user/ATG/ATG9.0/home)
     */
    @Test
    public void testResolveComponentWithNucleus() {
        assertNotNull(mNucleus);
        MutableRepository catalog = (MutableRepository) mNucleus.resolveName(
                "/atg/deployment/DeploymentRepository"
        );
        assertNotNull("DeploymentRepository should not be null.", catalog);
        MutableRepository profile = (MutableRepository) mNucleus.resolveName(
                "/atg/userprofiling/ProfileAdapterRepository"
        );
        // Good enough for this test.
        // Don't want to disturb any data that might be in this repository.
    }

}
