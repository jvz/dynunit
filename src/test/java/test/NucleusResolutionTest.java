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
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletTestUtils;
import atg.servlet.ServletUtil;
import junit.framework.TestCase;

/**
 * Based on {@link TestCase}
 */
public class NucleusResolutionTest
        extends TestCase {

    private Nucleus mNucleus;

    private final ServletTestUtils mServletTestUtils = new ServletTestUtils();

    /**
     * Start up Nucleus. In this case DAS is the base module.
     */
    @Override
    protected void setUp()
            throws Exception {
        super.setUp();

        // start up Nucleus with the DAS module, using MyComponent
        // as its initial service.
        mNucleus = NucleusTestUtils.startNucleusWithModules(
                new String[] { "DAS", "DafEar.base" }, this.getClass(), "/atg/dynamo/MyComponent"
        );
    }


    // test resolutions in various scopes
    public void testResolutions() {
        DynamoHttpServletRequest request = mServletTestUtils.createDynamoHttpServletRequestForSession(
                mNucleus, "mySessionId", "new"
        );
        System.out.println(
                "window id = " + request.getParameter(
                        DynamoHttpServletRequest.WINDOW_ID_PARAM_NAME
                )
        );
        assertNotNull(
                "Request component", resolveWithRequest(request, "/RequestComponent")
        );
        assertNotNull(
                "Session component", resolveWithRequest(request, "/SessionComponent")
        );
        assertNotNull(
                "Global component", resolveWithRequest(request, "/GlobalComponent")
        );
        assertNotNull(
                "Window component", resolveWithRequest(request, "/WindowComponent")
        );
    }


    Object resolveWithRequest(DynamoHttpServletRequest pRequest, String pNucleusPath) {
        DynamoHttpServletRequest requestOld = ServletUtil.setCurrentRequest(pRequest);
        try {
            Object objResult = pRequest.resolveName(pNucleusPath);

            System.out.println(
                    "Resolved component " + ((objResult
                                              == null) ? "null" : mNucleus.getAbsoluteNameOf(
                            objResult
                    ))
            );
            return objResult;
        } finally {
            ServletUtil.setCurrentRequest(requestOld);
        }

    }

    /**
     * Shutdown Nucleus.
     */
    @Override
    protected void tearDown()
            throws Exception {
        super.tearDown();
        if ( mNucleus != null ) {
            NucleusTestUtils.shutdownNucleus(mNucleus);
            mNucleus = null;
        }
    }


}
