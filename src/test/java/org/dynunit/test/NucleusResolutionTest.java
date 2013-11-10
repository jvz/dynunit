package org.dynunit.test;

import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletTestUtils;
import atg.servlet.ServletUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    Nucleus nucleus;

    DynamoHttpServletRequest request;

    DynamoHttpServletRequest saved;

    static final ServletTestUtils UTILS = new ServletTestUtils();

    final Logger log = LogManager.getLogger();

    @Parameterized.Parameters
    public static Iterable<String> data() {
        return Arrays.asList(
                "/RequestComponent", "/SessionComponent", "/GlobalComponent", "/WindowComponent"
        );
    }

    @Parameterized.Parameter
    public String componentPath;

    @BeforeClass
    public void setUpClass()
            throws ServletException {
        nucleus = NucleusTestUtils.startNucleusWithModules(
                new String[] { "DAS", "DafEar.base" }, this.getClass(), "/atg/dynamo/MyComponent"
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
        if ( nucleus != null ) {
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
