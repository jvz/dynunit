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
import atg.tools.dynunit.nucleus.NucleusTestUtils;
import atg.tools.dynunit.service.idgen.InitializingSQLIdGenerator;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class IdGeneratorTest
        extends TestCase {

    @Nullable
    private Nucleus mNucleus = null;

    protected void setUp()
            throws Exception {
        super.setUp();
        try {
            mNucleus = NucleusTestUtils.startNucleusWithModules(
                    new String[]{ "DAS" },
                    this.getClass(),
                    this.getClass().getName(),
                    "/atg/dynamo/service/IdGenerator"
            );
        } catch (ServletException e) {
            fail(e.getMessage());
        }
    }

    public void testSQLIdGenerator()
            throws Exception {
        // first make sure we get the "initializing" version
        InitializingSQLIdGenerator idgen = (InitializingSQLIdGenerator) mNucleus.resolveName(
                "/atg/dynamo/service/IdGenerator"
        );
        assertNotNull(idgen);
        DataSource dataSource = idgen.getDataSource();
        assertNotNull(dataSource);
        Connection c = null;
        ResultSet result = null;
        try {
            c = dataSource.getConnection();
            Statement statement = c.createStatement();
            result = statement.executeQuery(
                    "SELECT COUNT(*) from " + idgen.getTableName()
            );
            assertNotNull("no results, table not created " + idgen.getTableName(), result);
        } finally {
            if (result != null) {
                result.close();
            }
            if (c != null) {
                c.close();
            }
        }
    }

    public void testObfuscatedSQLIdGenerator() {

    }

    protected void tearDown()
            throws Exception {
        super.tearDown();
        if (mNucleus != null) {
            try {
                NucleusTestUtils.shutdownNucleus(mNucleus);
            } catch (ServiceException e) {
                fail(e.getMessage());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

}
