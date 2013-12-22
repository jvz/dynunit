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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author msicker
 * @version 1.0.0
 */
public class NucleusFactoryTest {

    private NucleusFactory factory;
    private Nucleus nucleus;
    private static final Logger logger = LogManager.getLogger();

    @Before
    public void setUp()
            throws Exception {
        factory = NucleusFactory.getFactory();
    }

    @After
    public void tearDown()
            throws Exception {
        if (nucleus != null) {
            if (nucleus.isRunning()) {
                nucleus.stopService();
                assertThat(nucleus.isRunning(), is(false));
            }
        }
    }

    @Test
    public void testGetFactory()
            throws Exception {
        final NucleusFactory anotherFactory = NucleusFactory.getFactory();
        assertThat(factory, is(equalTo(anotherFactory)));
    }

    @Test
    public void testCreateNucleus()
            throws Exception {
        nucleus = factory.createNucleus(new File("src/test/resources/config/simple"));
        assertThat(nucleus, is(notNullValue()));
        assertThat(nucleus.isRunning(), is(true));
        final Object instanceFactory = nucleus.resolveName("/atg/dynamo/nucleus/ParameterConstructorInstanceFactory");
        assertThat(instanceFactory, is(notNullValue()));
        logger.debug("Found instanceFactory: {}", instanceFactory);
        final String testComponent = (String) nucleus.resolveName("/test/TestComponent");
        assertThat(testComponent, is(notNullValue()));
        logger.debug("Value of TestComponent: {}", testComponent);
        assertThat(testComponent, is(equalTo("Hello")));
    }
}
