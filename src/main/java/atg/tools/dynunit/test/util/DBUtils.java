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

package atg.tools.dynunit.test.util;

import atg.tools.dynunit.adapter.gsa.GSATestUtils;
import atg.tools.dynunit.adapter.gsa.SQLFileParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;

/**
 * Utility code for getting a connection to a database.
 * The most common method is getHSQLDBInMemoryDBConnection.
 * This returns a connection to an in-memory HSQL database.
 *
 * @author adamb
 */
public class DBUtils {

    private static final Logger logger = LogManager.getLogger();
    @NotNull
    private final Properties connectionProperties;
    private final Connection connection;
    private String databaseType;
    private String databaseVersion;

    /**
     * Creates a new DBUtils given a Properties object containing connection info. Expected properties:
     * <ul>
     *     <li>URL</li>
     *     <li>driver</li>
     *     <li>user</li>
     *     <li>password</li>
     * </ul>
     *
     * @param connectionProperties database configuration.
     *
     * @throws Exception
     */
    public DBUtils(@NotNull Properties connectionProperties)
            throws Exception {
        this(
                connectionProperties.getProperty("URL"),
                connectionProperties.getProperty("driver"),
                connectionProperties.getProperty("user"),
                connectionProperties.getProperty("password")
        );
    }

    public DBUtils(String connectionURL, String driver, String user, String password)
            throws Exception {

        connectionProperties = new Properties();
        connectionProperties.put("driver", driver);
        connectionProperties.put("URL", connectionURL);
        connectionProperties.put("user", user);
        connectionProperties.put("password", password);

        //    general
        // exception

        // Load the HSQL Database Engine JDBC driver
        // hsqldb.jar should be in the class path or made part of the current jar
        Class.forName(driver);


        // connect to the database. This will load the db files and start the
        // database if it is not already running.
        // db_file_name_prefix is used to open or create files that hold the state
        // of the db.
        // It can contain directory names relative to the
        // current working directory
        connection = DriverManager.getConnection(
                connectionURL, // file names
                user, // username
                password
        ); // password
        databaseType = connection.getMetaData().getDatabaseProductName();
        databaseVersion = connection.getMetaData().getDatabaseProductVersion();
        logger.info("Connected to {} version {}", databaseType, databaseVersion);
        executeCreateIdGenerator();
    }

    // ---------------------------

    /**
     * Returns a Properties object pre-configured to create
     * an HSQLDB in memory database connecting with user "sa"
     * password "".
     *
     * @param databaseName name of database or {@code null} to use the default in-memory database.
     * @return connection properties for initializing this database.
     */
    @NotNull
    public static Properties getHSQLDBInMemoryDBConnection(@Nullable String databaseName) {
        Properties props = new Properties();
        props.put("driver", "org.hsqldb.jdbcDriver");
        if (databaseName != null) {
            props.put("URL", "jdbc:hsqldb:mem:" + databaseName);
        }
        else {
            props.put("URL", "jdbc:hsqldb:.");
        }
        props.put("user", "sa");
        props.put("password", "");
        return props;
    }

    /**
     * Returns a Properties object pre-configured to create
     * an HSQLDB in memory database connecting with user "sa"
     * password ""
     *
     * @param databaseName
     */
    @NotNull
    public static Properties getHSQLDBRegularDBConnection(String databaseName,
                                                          String hostName,
                                                          Object user,
                                                          Object password) {
        Properties props = new Properties();
        props.put("driver", "org.hsqldb.jdbcDriver");
        props.put("URL", "jdbc:hsqldb:hsql://" + hostName + "/" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }
    // ---------------------------

    /**
     * Returns a Properties object pre-configured to create
     * an HSQLDB in memory database connecting with user "sa"
     * password ""
     *
     * @param databaseFileName
     */
    @NotNull
    public static Properties getHSQLDBFileDBConnection(String databaseFileName) {
        Properties props = new Properties();
        props.put("driver", "org.hsqldb.jdbcDriver");
        props.put("URL", "jdbc:hsqldb:file:" + databaseFileName);
        props.put("user", "sa");
        props.put("password", "");
        return props;
    }
    // ---------------------------

    /**
     * Returns connection properties for MSSQL
     *
     * @param hostName
     *         host name of db server
     * @param port
     *         port number of db
     * @param databaseName
     *         database name
     * @param user
     *         database username
     * @param password
     *         database user's password
     *
     * @return
     */

    @NotNull
    public static Properties getMSSQLDBConnection(String hostName,
                                                  String port,
                                                  String databaseName,
                                                  String user,
                                                  String password) {
        Properties props = new Properties();
        props.put("driver", "com.inet.tds.TdsDriver");
        props.put("URL", "jdbc:inetdae:" + hostName + ":" + port + "?database=" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    // ---------------------------

    /**
     * Returns connection properties for mysql
     *
     * @param hostName
     *         host name of db server
     * @param port
     *         port number of db (or null to use default of 3306)
     * @param databaseName
     *         database name
     * @param user
     *         database username
     * @param password
     *         database user's password
     *
     * @return
     */

    @NotNull
    public static Properties getMySQLDBConnection(@NotNull String hostName,
                                                  @Nullable String port,
                                                  @NotNull String databaseName,
                                                  @NotNull String user,
                                                  @NotNull String password) {
        if (port == null) {
            port = "3306";
        }
        Properties props = new Properties();
        props.put("driver", "com.mysql.jdbc.Driver");
        props.put("URL", "jdbc:mysql://" + hostName + ":" + port + "/" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    @NotNull
    public static Properties getDB2DBConnection(String hostName,
                                                String port,
                                                String databaseName,
                                                String user,
                                                String password) {
        Properties props = new Properties();
        props.put("driver", "com.ibm.db2.jcc.DB2Driver");
        //    props.put("driver", "COM.ibm.db2.jdbc.app.DB2Drive");
        props.put("URL", "jdbc:db2://" + hostName + ":" + port + "/" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    // ---------------------------

    /**
     * Returns connection properties for MSSQL
     *
     * @param hostName
     *         host name of db server
     * @param port
     *         port number of db or {@code null} to use the default port 1521
     * @param databaseName
     *         database name
     * @param user
     *         database username
     * @param password
     *         database user's password
     *
     * @return
     */

    @NotNull
    public static Properties getOracleDBConnection(String hostName,
                                                   @Nullable String port,
                                                   String databaseName,
                                                   String user,
                                                   String password) {
        Properties props = new Properties();
        props = new Properties();
        if (port == null) {
            port = "1521";
        }
        props.put("driver", "oracle.jdbc.OracleDriver");
        props.put("URL", "jdbc:oracle:thin:@" + hostName + ":" + port + ":" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    /**
     * Returns connection properties for MSSQL
     *
     * @param hostName
     *         host name of db server
     * @param port
     *         port number of db
     * @param user
     *         database username
     * @param password
     *         database user's password
     *
     * @return
     */

    @NotNull
    public static Properties getSolidDBConnection(String hostName,
                                                  @Nullable String port,
                                                  String user,
                                                  String password) {
        Properties props = new Properties();
        if (port == null) {
            port = "1313";
        }
        props.put("driver", "solid.jdbc.SolidDriver");
        props.put("URL", "jdbc:solid://" + hostName + ":" + port);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    /**
     * Returns connection properties for MSSQL
     *
     * @param hostName
     *         host name of db server
     * @param port
     *         port number of db
     * @param databaseName
     *         database name
     * @param user
     *         database username
     * @param password
     *         database user's password
     *
     * @return
     */

    @NotNull
    public static Properties getSybaseDBConnection(String hostName,
                                                   @Nullable String port,
                                                   String databaseName,
                                                   String user,
                                                   String password) {
        Properties props = new Properties();
        if (port == null) {
            port = "5000";
        }
        props.put("driver", "com.sybase.jdbc2.jdbc.SybDriver");
        props.put("URL", " jdbc:sybase:Tds:" + hostName + ":" + port + "/" + databaseName);
        props.put("user", user);
        props.put("password", password);
        return props;
    }

    /**
     * Returns a Properties object pre-configured to create
     * an HSQLDB in memory database connecting with user "sa"
     * password ""
     */
    @NotNull
    public static Properties getHSQLDBInMemoryDBConnection() {
        return getHSQLDBInMemoryDBConnection("testdb");
    }

    public static File createJTDataSource(File root)
            throws IOException {
        return createJTDataSource(root, null, null);
    }

    /**
     * Creates a new JTDataSource component. The name of the component may
     * be specified by passing in a non null value for jtdsName.
     * Also the name of the FakeXADataSource may be specified by passing in a non null name.
     * Otherwise the defaults are JTDataSource and FakeXADataSource.
     *
     * @param root
     * @param jtdsName
     * @param xaName
     *
     * @return
     *
     * @throws IOException
     */
    public static File createJTDataSource(File root, String jtdsName, String xaName)
            throws IOException {
        return GSATestUtils.createJTDataSource(root, jtdsName, xaName);
    }

    /**
     * @param pProps
     *
     * @return
     */
    public static boolean isOracle(@NotNull Properties pProps) {
        return pProps.get("driver").toString().toLowerCase().contains("oracle");
    }

    /**
     * @param pProps
     *
     * @return
     */
    public static boolean isSybase(@NotNull Properties pProps) {
        return pProps.get("driver").toString().toLowerCase().contains("sybase");
    }

    /**
     * @param pProps
     *
     * @return
     */
    public static boolean isMSSQLServer(@NotNull Properties pProps) {
        return pProps.get("driver").equals("com.inet.tds.TdsDriver");
    }

    /**
     * @param pProps
     *
     * @return
     */
    public static boolean isDB2(@NotNull Properties pProps) {
        return pProps.get("driver").toString().contains("DB2");
    }

    public void shutdown()
            throws SQLException {
        if (!connection.isClosed()) {
            Statement st = connection.createStatement();

            // db writes out to files and performs clean shuts down
            // otherwise there will be an unclean shutdown
            // when program ends
            if (connection.getMetaData().getDatabaseProductName().startsWith("HSQL")) {
                st.execute("SHUTDOWN");
            }
            connection.close(); // if there are no other open connection
        }
    }

    public int getRowCount(String pTable)
            throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = connection.createStatement(); // statement objects can be reused with

            // repeated calls to execute but we
            // choose to make a new one each time
            rs = st.executeQuery("SELECT COUNT(*) FROM " + pTable); // run the query

            rs.next();
            return rs.getInt(1);
        } finally {
            if (st != null) {
                st.close();
            }
        }

    }

    //use for SQL command SELECT
    public void query(String expression)
            throws SQLException {
        logger.entry(expression);
        Statement statement = null;
        ResultSet resultSet;
        synchronized (connection) {
            try {
                statement = connection.createStatement(); // statement objects can be reused with

                // repeated calls to execute but we
                // choose to make a new one each time
                resultSet = statement.executeQuery(expression); // run the query

                // do something with the result set.
                dump(resultSet);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        // closed too
        // so you should copy the contents to some other object.
        // the result set is invalidated also if you recycle an Statement
        // and try to execute some other query before the result set has been
        // completely examined.
    }

    // ---------------------------------

    //use for SQL commands CREATE, DROP, INSERT and UPDATE
    public void update(String expression)
            throws SQLException {
        logger.entry(expression);
        Statement statement = null;
        synchronized (connection) {
            logger.debug("Synchronizing on connection object.");
            try {
                statement = connection.createStatement();
                int i = statement.executeUpdate(expression);
                if (i == -1) {
                    logger.warn("db error : {}", expression);
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
            logger.debug("Releasing lock on connection object.");
        }
        logger.exit();
    }

    void dump(@NotNull ResultSet rs)
            throws SQLException {

        // the order of the rows in a cursor
        // are implementation dependent unless you use the SQL ORDER statement
        ResultSetMetaData meta = rs.getMetaData();
        int colmax = meta.getColumnCount();
        int i;
        Object o = null;

        // the result set is a cursor into the data. You can only
        // point to one row at a time
        // assume we are pointing to BEFORE the first row
        // rs.next() points to next row and returns true
        // or false if there is no next row, which breaks the loop
        for (; rs.next(); ) {
            for (i = 0; i < colmax; ++i) {
                o = rs.getObject(i + 1); // Is SQL the first column is indexed

                // with 1 not 0
                logger.info(o);
            }
        }
    }

    void executeCreateIdGenerator()
            throws SQLException {
        // TODO: this should use DdlUtils or similar
        try {
            if (!isDB2()) {
                update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                                + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                                + " suffix  varchar(10)     null, primary key (id_space_name)) "
                );
            }
            else {
                update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                                + "seed    numeric(19,0)   not null, batch_size      numeric(19) not null, prefix  varchar(10)  default null,"
                                + " suffix  varchar(10)   default  null, primary key (id_space_name)) "
                );
            }
        } catch (SQLException e) {
            // drop and try again
            logger.info("DROPPING DAS_ID_GENERATOR");
            try {
                update("drop table das_id_generator");
            } catch (SQLException ex) {
                logger.catching(ex);
            }
            if (!isDB2()) {
                update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                                + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                                + " suffix  varchar(10)     null, primary key (id_space_name)) "
                );
            }
            else {
                update(
                        " create table das_id_generator (id_space_name   varchar(60)     not null,"
                                + "seed    numeric(19,0)   not null, batch_size      numeric(19) not null, prefix  varchar(10)  default null,"
                                + " suffix  varchar(10)   default  null, primary key (id_space_name)) "
                );
            }

        }
    }

    public void executeSQLFile(@NotNull File pFile) {
        logger.info("Attempting to execute {}", pFile);
        SQLFileParser parser = new SQLFileParser();
        Collection<String> c = parser.parseSQLFile(pFile.getAbsolutePath());
        for (String cmd : c) {
            try {
                if ("Oracle".equals(databaseType)) {
                    cmd = StringUtils.replace(cmd, "numeric", "NUMBER");
                    cmd = StringUtils.replace(cmd, "varchar ", "VARCHAR2 ");
                    cmd = StringUtils.replace(cmd, "varchar(", "VARCHAR2(");
                    cmd = StringUtils.replace(cmd, "binary", "RAW (250)");
                }
                logger.info("Executing {}", cmd);
                update(cmd);
            } catch (SQLException e) {
                logger.catching(e);
            }
        }
    }

    public File createFakeXADataSource(File root)
            throws IOException {
        return createFakeXADataSource(root, null);
    }

    public File createFakeXADataSource(File root, String componentName)
            throws IOException {
        return GSATestUtils.createFakeXADataSource(root, connectionProperties, componentName);
    }

    public boolean isOracle() {
        return DBUtils.isMSSQLServer(connectionProperties);
    }

    public boolean isSybase() {
        return DBUtils.isMSSQLServer(connectionProperties);
    }

    public boolean isMSSQLServer() {
        return DBUtils.isMSSQLServer(connectionProperties);
    }

    public boolean isDB2() {
        return DBUtils.isDB2(connectionProperties);
    }
}