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

package atg.tools.dynunit.service.jdbc;


import atg.service.jdbc.FakeXADataSource;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;

/**
 * Base class for InitializingDataSource's common functionality.
 *
 * @author adamb
 * @version $Id:$
 */

public class InitializingDataSourceBase
        extends FakeXADataSource {

    private String databaseName = "testdb";

    /**
     * Returns the name of the database to use with HSQLDB. The default name is
     * "testdb"
     *
     * @return
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the name of the database to be used with HSQLDB
     *
     * @param databaseName The name of the HSQLDB database to be created when this datasource
     *              starts up.
     */
    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Get the driver manager connection used in this data source. This method is provided because the normal method
     * for accessing the database connection is package-local.
     *
     * @return underlying database connection object or {@code null} if it couldn't be found.
     */
    @Nullable
    public Connection getConnection() {
        try {
            return (Connection) MethodUtils.invokeMethod(this, "getDriverManagerConnection", null, null);
        } catch (NoSuchMethodException e) {
            logError(e);
        } catch (IllegalAccessException e) {
            logError(e);
        } catch (InvocationTargetException e) {
            logError(e);
        }
        return null;
    }

}
