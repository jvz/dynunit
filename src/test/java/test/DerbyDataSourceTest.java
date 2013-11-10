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

/**
 * This test starts up an ATG repository using Derby as the database. It tests the DerbyDataSource
 * class.
 * Note that as of ATG 9.0 there appears to be a problem when starting the GSARepository in
 * multithreaded mode on Derby.
 * A deadlock occurs on DatabaseMetaData.getColumns.
 * This test disables multithreaded startup and sets loadColumnInfosAtStartup=false to workaround
 * the problem.
 * See src/test/resources/test/data/test.DerbyDataSourceTest/ for the specific .properties file
 * changes
 * made by this test.
 *
 * @author adamb
 */
public class DerbyDataSourceTest
        extends StartWithModulesTest {

}
