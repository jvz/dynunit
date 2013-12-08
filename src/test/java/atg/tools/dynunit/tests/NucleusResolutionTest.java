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

package atg.tools.dynunit.tests;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;
import atg.tools.dynunit.nucleus.NucleusTestUtils;
import atg.tools.dynunit.servlet.ServletTestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author msicker
 * @version 1.0.0
 */
@RunWith(Parameterized.class)
public class NucleusResolutionTest {

    private Nucleus nucleus;

    private DynamoHttpServletRequest request;

    private DynamoHttpServletRequest saved;

    private static final ServletTestUtils UTILS = new ServletTestUtils();

    private final Logger log = LogManager.getLogger();

    @NotNull
    @Parameterized.Parameters
    public static Iterable<String> data() {
        return Arrays.asList(
                "/RequestComponent", "/SessionComponent", "/GlobalComponent", "/WindowComponent"
        );
    }

    @Parameterized.Parameter
    public /* not private */ String componentPath;

    @BeforeClass
    public void setUpClass()
            throws ServletException {
        nucleus = NucleusTestUtils.startNucleusWithModules(
                new String[]{ "DAS", "DafEar.base" }, this.getClass(), "/atg/dynamo/MyComponent"
        );
        assertThat(nucleus, is(notNullValue()));
        request = UTILS.createDynamoHttpServletRequestForSession(
                nucleus, "mySessionId", "new"
        );
        assertThat(request, is(notNullValue()));
        log.debug("Window ID = {}", DynamoHttpServletRequest.WINDOW_ID_PARAM_NAME);
    }

    @AfterClass
    public void tearDownClass()
            throws IOException, ServiceException {
        if (nucleus != null) {
            NucleusTestUtils.shutdownNucleus(nucleus);
        }
    }

    @Before
    public void setUp() {
        saved = ServletUtil.setCurrentRequest(request);
    }

    @After
    public void tearDown() {
        ServletUtil.setCurrentRequest(saved);
    }

    @Test
    public void test() {
        Object component = request.resolveName(componentPath);
        assertThat(component, is(notNullValue()));
        log.debug("Resolved component {}", nucleus.getAbsoluteNameOf(component));
    }
}
