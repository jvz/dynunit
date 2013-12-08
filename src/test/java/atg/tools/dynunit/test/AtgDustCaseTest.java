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

package atg.tools.dynunit.test;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author robert
 */
public class AtgDustCaseTest {

    private AtgDustCase atgCase;

    @Before
    public void setUp()
            throws Exception {
        atgCase = new AtgDustCase();
    }

    @Test
    public void testCreatePropertyFile() {
        try {

            atgCase.createPropertyFile(
                    "target/test-classes/config", "/some/component/impl", Object.class
            );

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetMoreCoverage() {
        try {
            atgCase.tearDown();
        } catch (Exception e) {
            fail("Previous call should not have triggered an Exception");
        }

        try {
            atgCase.copyConfigurationFiles(
                    new String[]{
                            "src/test/resources/config".replace("/", File.separator)
                    }, "target/test-classes/config".replace("/", File.separator), ".svn"
            );

            // starts nucleus and resolves component (which can't be resolved because
            // it's non existing)
            assertNull(atgCase.resolveNucleusComponent("bla"));

            // just resolves the component (which can't be resolved because
            // it's non existing)
            assertNull(atgCase.resolveNucleusComponent("bla"));

            // TODO: there must be a way to really test this
            atgCase.setDebug(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Previous call should not have triggered an Exception");
        }

    }

    public void _testCopyConfigurationFiles()
            throws Exception {
        atgCase.copyConfigurationFiles(
                new String[]{ "/Users/robert/tmp/back_ex/atgdust-lots-of-props" },
                "/tmp/atgdust",
                ".svn"
        );
        atgCase.copyConfigurationFiles(
                new String[]{
                        "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2",
                        "/Users/robert/tmp/back_ex/atgdust-lots-of-props/tmp2"
                }, "/tmp/atgdust", "excludes"
        );
    }
}
