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

import atg.nucleus.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <b>Experimental since Apache Derby is not supported by ATG 9.0.</b>
 * <p/>
 * This datasource is used for testing. It starts up a Derby in memory instance
 * on localhost automatically. The database will be named "testdb" by default.
 * If you need to name it something else set the "databaseName" property on this
 * component. You may want to change the name if your test requires running two
 * databases at the same time.
 *
 * @author adamb
 * @version $Id:$
 */

public class DerbyDataSource
        extends InitializingDataSourceBase {

    private static final Logger logger = LogManager.getLogger();
    private static final String EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String PROTOCOL = "jdbc:derby:";
    private String framework = "embedded";
    private boolean addedShutdownHook = false;

    /**
     * Shuts down derby
     *
     * @param name
     */
    private static void shutdown(String name) {
        try {
            // the shutdown=true attribute shuts down Derby
            DriverManager.getConnection(PROTOCOL + name + ";shutdown=true");

            // To shut down a specific database only, but keep the
            // engine running (for example for connecting to other
            // databases), specify a database in the connection URL:
            // DriverManager.getConnection("jdbc:derby:" + dbName +
            // ";shutdown=true");
        } catch (SQLException se) {

            if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
                // we got the expected exception
                logger.info("Derby shut down normally");
                // Note that for single database shutdown, the expected
                // SQL state is "08006", and the error code is 45000.
            }
            else if ((se.getErrorCode() == 45000) && ("08006".equals(se.getSQLState()))) {
                logger.trace("Derby was already shut down.");
                // database is already shutdown
            }
            else {
                // if the error code or SQLState is different, we have
                // an unexpected exception (shutdown failed)
                logger.error("Derby did not shut down normally", se);
                logSQLException(se);
            }
        }

    }

    /**
     * Logs details of an SQLException chain. Details included are SQL State, Error code, Exception message.
     *
     * @param e
     *         the SQLException from which to print details.
     */
    private static void logSQLException(@Nullable SQLException e) {
        for (; e != null; e = e.getNextException()) {
            logger.error("SQL State: {}. Error Code: {}. Message: {}.", e.getSQLState(),
                    e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Sets Derby JDBC properties to be used when the first client asks for a
     * connection.
     */
    @Override
    public void doStartService()
            throws ServiceException {
        logger.trace("Starting up Derby data source");
        loadDriver();
        this.setURL(PROTOCOL + getDatabaseName() + ";create=true");
        this.setDriver(EMBEDDED_DRIVER);
        this.setUser("user1");
        this.setPassword("user1");
    }

    /**
     * Cleans up for dynamo shutdown
     */
    @Override
    public void doStopService()
            throws ServiceException {
        // Add a shutdown hook to shut down derby.
        // We can't shutdown now because not all dynamo services
        // that depend on us are guaranteed to be stopped when this method is
        // invoked.
        if (!addedShutdownHook) {
            addShutdownHook(getDatabaseName());
        }
    }

    /**
     * Adds a shutdown hook to shutdown Derby when the JVM exits.
     *
     * @param pDBName
     */
    private void addShutdownHook(String pDBName) {
        final String name = pDBName;
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    public void run() {
                        DerbyDataSource.shutdown(name);
                    }
                }
        );
        addedShutdownHook = true;
    }

    /**
     * Loads the appropriate JDBC driver for this environment/framework. For
     * example, if we are in an embedded environment, we load Derby's embedded
     * Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
     * <p/>
     * The JDBC driver is loaded by loading its class. If you are using JDBC 4.0
     * (Java SE 6) or newer, JDBC drivers may be automatically loaded, making
     * this code optional.
     * <p/>
     * In an embedded environment, this will also start up the Derby engine
     * (though not any databases), since it is not already running. In a client
     * environment, the Derby engine is being run by the network server
     * framework.
     * <p/>
     * In an embedded environment, any static Derby system properties must be
     * set before loading the driver to take effect.
     */
    private void loadDriver() {
        try {
            Class.forName(EMBEDDED_DRIVER).newInstance();
        } catch (ClassNotFoundException cnfe) {
            logger.catching(cnfe);
            logger.error("Unable to load the JDBC driver {}", EMBEDDED_DRIVER);
        } catch (InstantiationException ie) {
            logger.catching(ie);
            logger.error("Unable to instantiate the JDBC driver {}", EMBEDDED_DRIVER);
        } catch (IllegalAccessException iae) {
            logger.catching(iae);
            logger.error("Not allowed to access the JDBC driver {}", EMBEDDED_DRIVER);
        }
    }

}
