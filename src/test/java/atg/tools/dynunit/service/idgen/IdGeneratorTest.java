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

package atg.tools.dynunit.service.idgen;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.service.idgen.IdGenerator;
import atg.tools.dynunit.nucleus.NucleusUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author msicker
 * @version 1.0.0
 */
@RunWith(JUnit4.class)
public class IdGeneratorTest {

    private static Nucleus nucleus;

    private InitializingSQLIdGenerator idGenerator;

    private Connection idGeneratorConnection;

    private ResultSet results;

    private static final String ID_GENERATOR = "/atg/dynamo/service/IdGenerator";

    @BeforeClass
    public static void startUp()
            throws ServletException, FileNotFoundException {
        nucleus = NucleusUtils.startNucleusWithModules(
                new String[]{ "DAS" },
                IdGeneratorTest.class,
                IdGeneratorTest.class.getName(),
                ID_GENERATOR
        );
    }

    @AfterClass
    public static void shutDown()
            throws IOException, ServiceException {
        assertThat(nucleus, is(notNullValue()));
        NucleusUtils.stopNucleus(nucleus);
    }

    @Before
    public void setUp()
            throws SQLException {
        setUpIdGenerator();
        setUpConnection();
    }

    @After
    public void tearDown()
            throws SQLException {
        if (results != null) {
            results.close();
        }
        if (idGeneratorConnection != null) {
            idGeneratorConnection.close();
        }
    }

    private void setUpIdGenerator() {
        final Object generator = nucleus.resolveName(ID_GENERATOR);
        assertThat(generator, is(notNullValue()));
        assertThat(generator, is(instanceOf(IdGenerator.class)));
        assertThat(generator, is(instanceOf(InitializingSQLIdGenerator.class)));
        idGenerator = (InitializingSQLIdGenerator) generator;
    }

    private void setUpConnection()
            throws SQLException {
        final DataSource idGeneratorDataSource = idGenerator.getDataSource();
        assertThat(idGeneratorDataSource, is(notNullValue()));
        idGeneratorConnection = idGeneratorDataSource.getConnection();
        assertThat(idGeneratorConnection, is(notNullValue()));
    }

    @Test
    public void testSqlIdGenerator()
            throws SQLException {
        Statement statement = idGeneratorConnection.createStatement();
        assertThat(statement, is(notNullValue()));

        results = statement.executeQuery(
                "SELECT COUNT(*) FROM " + idGenerator.getTableName()
        );
        assertThat(
                "Couldn't query table " + idGenerator.getTableName(), results, is(notNullValue())
        );

    }

}
