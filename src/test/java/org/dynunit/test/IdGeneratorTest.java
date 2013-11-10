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

package org.dynunit.test;

import atg.nucleus.Nucleus;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;
import atg.service.idgen.IdGenerator;
import atg.service.idgen.InitializingSQLIdGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;

/**
 * @author msicker
 * @version 1.0.0
 */
@RunWith(JUnit4.class)
public class IdGeneratorTest {

    private Nucleus nucleus;

    private static final String ID_GENERATOR = "/atg/dynamo/service/IdGenerator";

    @Before
    public void setUp() {
        try {
            nucleus = NucleusTestUtils.startNucleusWithModules(
                    new String[] { "DAS" }, this.getClass(), this.getClass().getName(), ID_GENERATOR
            );
        } catch ( ServletException e ) {
            assumeNoException(e);
        }
    }

    @After
    public void tearDown() {
        assertThat("What happened to the Nucleus?", nucleus, is(notNullValue()));
        try {
            NucleusTestUtils.shutdownNucleus(nucleus);
        } catch ( ServiceException e ) {
            assumeNoException(e);
        } catch ( IOException e ) {
            assumeNoException(e);
        }
    }

    @Test
    public void testSqlIdGenerator()
            throws SQLException {
        Object generator = nucleus.resolveName(ID_GENERATOR);
        assertThat(
                generator, allOf(
                notNullValue(),
                instanceOf(IdGenerator.class),
                instanceOf(InitializingSQLIdGenerator.class)
        )
        );
        InitializingSQLIdGenerator idGenerator = (InitializingSQLIdGenerator) generator;

        DataSource dataSource = idGenerator.getDataSource();
        assertThat(dataSource, is(notNullValue()));

        Connection connection = dataSource.getConnection();
        assertThat(connection, is(notNullValue()));

        Statement statement = connection.createStatement();
        assertThat(statement, is(notNullValue()));

        ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM " + idGenerator.getTableName()
        );
        assertThat(
                "Couldn't query table " + idGenerator.getTableName(), resultSet, is(notNullValue())
        );

        resultSet.close();
        connection.close();
    }

}
