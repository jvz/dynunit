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
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;
import atg.tools.dynunit.droplet.DropletInvoker.DropletResult;
import atg.tools.dynunit.droplet.DropletInvoker.RenderedOutputParameter;
import atg.tools.dynunit.nucleus.NucleusUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DropletInvokerTest {

    private Nucleus mNucleus;

    @Before
    public void setUp()
            throws Exception {
        mNucleus = NucleusUtils.startNucleusWithModules(
                new String[]{ "DAS", "DafEar.Tomcat" },
                this.getClass(),
                "/atg/dynamo/droplet/Switch"
        );
    }

    @After
    public void tearDown()
            throws Exception {
        ServletUtil.setCurrentRequest(null);
        if (mNucleus != null) {
            NucleusUtils.stopNucleus(mNucleus);
            mNucleus = null;
        }
    }


    @Test
    public void testDropletInvoker()
            throws ServletException, IOException {
        doTestSwitch();
        doTestForEach();
    }

    void doTestSwitch()
            throws ServletException, IOException {
        DropletInvoker invoker = new DropletInvoker(mNucleus);
        DynamoHttpServletRequest request = invoker.getRequest();

        assertNotNull("Request must not be null.", request);
        assertNotNull("Request session must not be null.", request.getSession());
        // test unset if value isn't set

        DropletResult result = invoker.invokeDroplet("/atg/dynamo/droplet/Switch");
        assertNotNull(
                "Make sure that unset got rendered", result.getRenderedOutputParameter("unset")
        );

        // test that foo gets rendered if value is foo
        request.setParameter("value", "foo");

        result = invoker.invokeDroplet("/atg/dynamo/droplet/Switch");

        assertNotNull(
                "Make sure that foo got rendered", result.getRenderedOutputParameter("foo")
        );


        // test that unset gets rendered if value is unset by blocking
        // the rendering of foo
        invoker.setOparamExistsOverride("foo", false);
        result = invoker.invokeDroplet("/atg/dynamo/droplet/Switch");

        assertNotNull(
                "Make sure that default got rendered", result.getRenderedOutputParameter("default")
        );

        // now let's reset our request, and make sure the unset
        // gets rendered, this time
        invoker.resetRequestResponse();
        result = invoker.invokeDroplet("/atg/dynamo/droplet/Switch");
        assertNotNull(
                "Make sure that unset got rendered", result.getRenderedOutputParameter("unset")
        );
    }


    void doTestForEach()
            throws ServletException, IOException {
        DropletInvoker invoker = new DropletInvoker(mNucleus);

        String[] strings = new String[]{ "one", "two", "three", "four", "five" };

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("array", strings);
        // alternatively: do invoker.getRequest().setParameter("array", strings);

        DropletResult result = invoker.invokeDroplet("/atg/dynamo/droplet/ForEach", params);

        assertNotNull(
                "Make sure output got rendered at least once",
                result.getRenderedOutputParameter("output", false)
        );

        List<RenderedOutputParameter> listOutputs = result.getRenderedOutputParametersByName(
                "output"
        );

        assertEquals(
                "Make sure output was rendered 5 times", 5, listOutputs.size()
        );

        // use an old-fashioned for, because we want to test numeric
        // look-ups, too.
        for (int i = 0; i < strings.length; i++) {
            RenderedOutputParameter oparam = listOutputs.get(i);

            assertEquals(
                    "Should be the same as from the list ",
                    oparam,
                    result.getRenderedOutputParameter("output", i)
            );
            assertEquals(
                    "Element should be our string",
                    strings[i],
                    (String) oparam.getFrameParameter("element")
            );
            assertEquals(
                    "Index should be equal to i", i, oparam.getFrameParameter("index")
            );
            // make sure we can get at things through
            // getFrameParameterOfRenderedParameter
            assertEquals(
                    "Count should be i + 1", i + 1, result.getFrameParameterOfRenderedParameter(
                    "count", "output", i
            )
            );
        }
    }


}
