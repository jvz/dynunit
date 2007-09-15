/**
 * Copyright 2007 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package atg.test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import atg.adapter.gsa.GSATestUtils;
import atg.adapter.gsa.SQLFileParser;
import atg.core.util.StringUtils;

/**
 * Utility code for getting a connection to a database.
 * The most common method is getHSQLDBInMemoryDBConnection.
 * This returns a connection to an in-memory HSQL database.
 * @author adamb
 *
 */
public class DBUtils {

  public Connection conn; //our connnection to the db - presist for life of
  private Properties mJDBCProperties;
  
  private static final Log log = LogFactory.getLog(DBUtils.class);
  // ---------------------------
  /**
   * Returns a Properties object preconfigured to create
   * an HSQLDB in memory database connecting with user "sa"
   * password ""
   * @param pTestDBName
   */
  public static Properties getHSQLDBInMemoryDBConnection(String pTestDBName) {
    Properties props = new Properties();
    props.put("driver", "org.hsqldb.jdbcDriver");
    if(pTestDBName != null)
      props.put("URL", "jdbc:hsqldb:mem:" + pTestDBName);
    else 
      props.put("URL", "jdbc:hsqldb:.");
    props.put("user", "sa");
    props.put("password", "");
    return props;
  }
  
  
  /**
   * Returns a Properties object preconfigured to create
   * an HSQLDB in memory database connecting with user "sa"
   * password ""
   * @param pTestDBName
   */
  public static Properties getHSQLDBRegularDBConnection(String pTestDBName, String pHostName, Object pUser, Object pPassword) {
    Properties props = new Properties();
    props.put("driver", "org.hsqldb.jdbcDriver");
    props.put("URL", "jdbc:hsqldb:hsql://" + pHostName+ "/"+pTestDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  
  /**
   * Returns a Properties object preconfigured to create
   * an HSQLDB in memory database connecting with user "sa"
   * password ""
   * @param pTestDBName
   */
  public static Properties getHSQLDBFileDBConnection(String pPath) {
    Properties props = new Properties();
    props.put("driver", "org.hsqldb.jdbcDriver");
    props.put("URL", "jdbc:hsqldb:file:" + pPath);
    props.put("user", "sa");
    props.put("password", "");
    return props;
  }
  
  // ---------------------------
  /**
   * Returns connection properties for MSSQL
   * @param pHostName host name of db server
   * @param pPort port number of db
   * @param pDBName database name
   * @param pUser database username
   * @param pPassword database user's password
   * @return
   */
  
  public static Properties getMSSQLDBConnection(String pHostName, String pPort, String pDBName, String pUser, String pPassword) {
    Properties props = new Properties();
    props.put("driver", "com.inet.tds.TdsDriver");
    props.put("URL", "jdbc:inetdae:" + pHostName + ":" + pPort + "?database=" + pDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  
  // ---------------------------
  /**
   * Returns connection properties for mysql
   * @param pHostName host name of db server
   * @param pPort port number of db
   * @param pDBName database name
   * @param pUser database username
   * @param pPassword database user's password
   * @return
   */
  
  public static Properties getMySQLDBConnection(String pHostName, String pPort, String pDBName, String pUser, String pPassword) {
    if(pPort == null)pPort = "3306";
    Properties props = new Properties();
    props.put("driver", "com.mysql.jdbc.Driver");
    props.put("URL", "jdbc:mysql://" + pHostName + ":" + pPort + "/" + pDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  
  /**
   * @param pString
   * @param pString2
   * @param pString3
   * @param pString4
   * @param pString5
   * @return
   */
  public static Properties getDB2DBConnection(String pHostName, String pPort, String pDBName, String pUser, String pPassword) {
    Properties props = new Properties();
    props.put("driver", "com.ibm.db2.jcc.DB2Driver");
//    props.put("driver", "COM.ibm.db2.jdbc.app.DB2Drive");
    props.put("URL", "jdbc:db2://" + pHostName + ":" + pPort + "/" + pDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }  
  // ---------------------------
  /**
   * Returns connection properties for MSSQL
   * @param pHostName host name of db server
   * @param pPort port number of db
   * @param pDBName database name
   * @param pUser database username
   * @param pPassword database user's password
   * @return
   */
  
  public static Properties getOracleDBConnection(String pHostName, String pPort, String pDBName, String pUser, String pPassword) {
    Properties props = new Properties();
    props = new Properties();  
    String port = pPort;
    if(pPort == null)
      port = "1521";
    props.put("driver", "oracle.jdbc.OracleDriver");
    props.put("URL", "jdbc:oracle:thin:@"+pHostName+":"+port+":"+pDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  // ---------------------------
  /**
   * Returns connection properties for MSSQL
   * @param pHostName host name of db server
   * @param pPort port number of db
   * @param pDBName database name
   * @param pUser database username
   * @param pPassword database user's password
   * @return
   */
  
  public static Properties getSolidDBConnection(String pHostName, String pPort, String pUser, String pPassword) {
    Properties props = new Properties();
    props = new Properties();  
    String port = pPort;
    if(pPort == null)
      port = "1313";
    props.put("driver", "solid.jdbc.SolidDriver");
    props.put("URL", "jdbc:solid://"+pHostName+":"+port);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  
  // ---------------------------
  /**
   * Returns connection properties for MSSQL
   * @param pHostName host name of db server
   * @param pPort port number of db
   * @param pDBName database name
   * @param pUser database username
   * @param pPassword database user's password
   * @return
   */
  
  public static Properties getSybaseDBConnection(String pHostName, String pPort, String pDBName, String pUser, String pPassword) {
    Properties props = new Properties();
    props = new Properties();  
    String port = pPort;
    if(pPort == null)
      port = "5000";
    props.put("driver", "com.sybase.jdbc2.jdbc.SybDriver");
    props.put("URL", " jdbc:sybase:Tds:"+pHostName+":"+port+"/"+pDBName);
    props.put("user", pUser);
    props.put("password", pPassword);
    return props;
  }
  
  /**
   * Returns a Properties object preconfigured to create
   * an HSQLDB in memory database connecting with user "sa"
   * password ""
   * @param pTestDBName
   */
  public static Properties getHSQLDBInMemoryDBConnection() {
    return getHSQLDBInMemoryDBConnection("testdb");
  }
  
  // ---------------------------
  /**
   * Creates a new DBUtils given a Properties object containing connection info
   * Expected keys:
   * URL<BR>
   * driver<BR>
   * user<BR>
   * password<BR>
   * <BR>
   * @param pProps
   * @throws Exception
   */
  public DBUtils(Properties pProps) throws Exception {
    this(pProps.getProperty("URL"),pProps.getProperty("driver"),pProps.getProperty("user"),pProps.getProperty("password"));
  }
  public String mDatabaseType = null;
  private String mDatabaseVersion;
  // ---------------------------
  public DBUtils(String pURL, String pJDBCDriver,
      String pUser, String pPassword) throws Exception {
    
     mJDBCProperties = new Properties();
     mJDBCProperties.put("driver", pJDBCDriver);
     mJDBCProperties.put("URL", pURL);
     mJDBCProperties.put("user", pUser);
     mJDBCProperties.put("password",pPassword);

    //    general
    // exception

    // Load the HSQL Database Engine JDBC driver
    // hsqldb.jar should be in the class path or made part of the current jar
    Class.forName(pJDBCDriver);


    // connect to the database. This will load the db files and start the
    // database if it is not alread running.
    // db_file_name_prefix is used to open or create files that hold the state
    // of the db.
    // It can contain directory names relative to the
    // current working directory
    conn = DriverManager.getConnection(pURL, // filenames
        pUser, // username
        pPassword); // password
    mDatabaseType = conn.getMetaData().getDatabaseProductName();
    mDatabaseVersion = conn.getMetaData().getDatabaseProductVersion();
    log.info("Connected to "
        + mDatabaseType + " Version: "+ mDatabaseVersion);
    executeCreateIdGenerator();
  }

  public void shutdown() throws SQLException {
    if(!conn.isClosed()){
    Statement st = conn.createStatement();

    // db writes out to files and performs clean shuts down
    // otherwise there will be an unclean shutdown
    // when program ends
    if (conn.getMetaData().getDatabaseProductName().startsWith("HSQL"))
      st.execute("SHUTDOWN");
      conn.close(); // if there are no other open connection
    }
  }

  public int getRowCount(String pTable) throws SQLException {
    Statement st = null;
    ResultSet rs = null;
    try {
      st = conn.createStatement(); // statement objects can be reused with

      // repeated calls to execute but we
      // choose to make a new one each time
      rs = st.executeQuery("SELECT COUNT(*) FROM " + pTable); // run the query

      rs.next();
      int count = rs.getInt(1);
      return count;
    }
    finally {
      st.close(); // NOTE!! if you close a statement the associated ResultSet is
    }

  }

  //use for SQL command SELECT
  public synchronized void query(String expression) throws SQLException {

    Statement st = null;
    ResultSet rs = null;

    st = conn.createStatement(); // statement objects can be reused with

    // repeated calls to execute but we
    // choose to make a new one each time
    rs = st.executeQuery(expression); // run the query

    // do something with the result set.
    dump(rs);
    st.close(); // NOTE!! if you close a statement the associated ResultSet is

    // closed too
    // so you should copy the contents to some other object.
    // the result set is invalidated also if you recycle an Statement
    // and try to execute some other query before the result set has been
    // completely examined.
  }

  //use for SQL commands CREATE, DROP, INSERT and UPDATE
  public synchronized void update(String expression) throws SQLException {
    //log.info("DBUtils.update : " + expression);
    Statement st = null;

    st = conn.createStatement(); // statements

    int i = st.executeUpdate(expression); // run the query

    if (i == -1) {
      log.info("db error : " + expression);
    }

    st.close();
  } // void update()

  public void dump(ResultSet rs) throws SQLException {

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
    for (; rs.next();) {
      for (i = 0; i < colmax; ++i) {
        o = rs.getObject(i + 1); // Is SQL the first column is indexed

        // with 1 not 0
        System.out.print(o.toString() + " ");
      }

      log.info(" ");
    }
  } //void dump( ResultSet rs )
  
  /**
   * @param db
   * @throws SQLException
   */
  public void executeCreateIdGenerator() throws SQLException {
    try {
      if(!isDB2())
            update(" create table das_id_generator (id_space_name   varchar(60)     not null,"
                + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                + " suffix  varchar(10)     null, primary key (id_space_name)) ");
        else 
          update(" create table das_id_generator (id_space_name   varchar(60)     not null,"
              + "seed    numeric(19,0)   not null, batch_size      numeric(19) not null, prefix  varchar(10)  default null,"
              + " suffix  varchar(10)   default  null, primary key (id_space_name)) ");
    } catch (SQLException e) {
      // drop and try again
      log.info("DROPPING DAS_ID_GENERATOR");
      try {
        update("drop table das_id_generator");
      } catch (SQLException ex) {

      }
      if(!isDB2())
            update(" create table das_id_generator (id_space_name   varchar(60)     not null,"
                + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
                + " suffix  varchar(10)     null, primary key (id_space_name)) ");
        else 
          update(" create table das_id_generator (id_space_name   varchar(60)     not null,"
              + "seed    numeric(19,0)   not null, batch_size      numeric(19) not null, prefix  varchar(10)  default null,"
              + " suffix  varchar(10)   default  null, primary key (id_space_name)) ");

    }
  }
  
  public void executeSQLFile(File pFile) {
    log.info("Attemping to execute " + pFile);
    SQLFileParser parser = new SQLFileParser();
    Collection<String> c = parser.parseSQLFile(pFile.getAbsolutePath());
    Iterator<String> cmds = c.iterator();
    while (cmds.hasNext()) {
      String cmd =  cmds.next();
      try {
        if ("Oracle".equals(mDatabaseType)) {
          cmd = StringUtils.replace(cmd, "numeric", "NUMBER");
          cmd = StringUtils.replace(cmd, "varchar ", "VARCHAR2 ");
          cmd = StringUtils.replace(cmd, "varchar(", "VARCHAR2(");
          cmd = StringUtils.replace(cmd, "binary", "RAW (250)");
        }
        log.info("Executing " + cmd);
        update(cmd);
      }    
      catch (SQLException e) {
        log.info(e.getMessage());
      }
    }    
  }
  
  public File createFakeXADataSource(File pRoot) throws IOException{
    return GSATestUtils.createFakeXADataSource(pRoot, mJDBCProperties, null);
  }
  
  public File createFakeXADataSource(File pRoot, String pName) throws IOException{
    
    return GSATestUtils.createFakeXADataSource(pRoot, mJDBCProperties, pName);
    
  }
  
  // ---------------------------------
  /**
   * @param pRoot
   * @throws IOException
   */
  public static File createJTDataSource(File pRoot) throws IOException {
    return GSATestUtils.createJTDataSource(pRoot, null,null);
  }

  // ------------------------------------
  /**
   * Creates a new JTDataSource component. The name of the component may
   * be specified by passing in a non null value for pName.
   * Also the name of the FakeXADataSource may be specified by passing in a non null name.
   * Otherwise the defaults are JTDataSource and FakeXADataSource.
   * 
   * @param pRoot
   * @param pName
   * @param pFakeXAName
   * @return
   * @throws IOException
   */
  public static File createJTDataSource(File pRoot, String pName, String pFakeXAName)
      throws IOException {
    return GSATestUtils.createJTDataSource(pRoot, pName, pFakeXAName);
  }

  /**
   * @param pProps
   * @return
   */
  public static boolean isOracle(Properties pProps) {
    return pProps.get("driver").toString().toLowerCase().indexOf("oracle") != -1;
  }

  /**
   * @param pProps
   * @return
   */
  public static boolean isSybase(Properties pProps) {
    return pProps.get("driver").toString().toLowerCase().indexOf("sybase") != -1;    
  }

  /**
   * @param pProps
   * @return
   */
  public static boolean isMSSQLServer(Properties pProps) {
    return pProps.get("driver").equals( "com.inet.tds.TdsDriver");
  }

  /**
   * @param pProps
   * @return
   */
  public static boolean isDB2(Properties pProps) {
    return pProps.get("driver").toString().indexOf("DB2") != -1;
  }
  

  /**
   * @param pProps
   * @return
   */
  public boolean isOracle() {
    return DBUtils.isMSSQLServer(mJDBCProperties);
  }

  /**
   * @param pProps
   * @return
   */
  public boolean isSybase() {
    return DBUtils.isMSSQLServer(mJDBCProperties);
  }

  /**
   * @param pProps
   * @return
   */
  public boolean isMSSQLServer() {
    return DBUtils.isMSSQLServer(mJDBCProperties);
  }

  /**
   * @param pProps
   * @return
   */
  public boolean isDB2() {
    return DBUtils.isDB2(mJDBCProperties);
  }
}