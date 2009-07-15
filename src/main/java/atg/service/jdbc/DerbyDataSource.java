/**
 * Copyright 2009 ATG DUST Project Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package atg.service.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import atg.nucleus.ServiceException;

/**
 * <b>Experimental since Apache Derby is not supported by ATG 9.0.</b>
 * 
 * This datasource is used for testing. It starts up a Derby in memory instance
 * on localhost automatically. The database will be named "testdb" by default.
 * If you need to name it something else set the "databaseName" property on this
 * component. You may want to change the name if your test requires running two
 * databases at the same time.
 * 
 * @author adamb
 * @version $Id:$
 */

public class DerbyDataSource extends InitializingDataSourceBase {

  static Logger sLog = Logger.getLogger(DerbyDataSource.class);
  
  private String  framework          = "embedded";
  private String  driver             = "org.apache.derby.jdbc.EmbeddedDriver";
  private String  protocol           = "jdbc:derby:";
  private boolean mAddedShutdownHook = false;

  /**
   * Sets Derby JDBC properties to be used when the first client asks for a
   * connection.
   */
  @Override
  public void doStartService() throws ServiceException {
    logInfo("Starting DerbyDataSource.");
    loadDriver();
    this.setURL(protocol + getDatabaseName() + ";create=true");
    this.setDriver(driver);
    this.setUser("user1");
    this.setPassword("user1");
  }

  /**
   * Cleans up for dynamo shutdown
   */
  @Override
  public void doStopService() throws ServiceException {
    // Add a shutdown hook to shut down derby.
    // We can't shutdown now because not all dynamo services
    // that depend on us are guaranteed to be stopped when this method is
    // invoked.
    if (!mAddedShutdownHook)
      addShutdownHook(getDatabaseName());
  }

  /**
   * Adds a shutdown hook to shutdown Derby when the JVM exits.
   * 
   * @param pDBName
   *          
   */
  private void addShutdownHook(String pDBName) {
    final String name = pDBName;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        DerbyDataSource.shutdown(name);
      }
    });
  }

  /**
   * Shuts down derby
   * 
   * @param name
   */
  private static void shutdown(String name) {
    try {
      // the shutdown=true attribute shuts down Derby
      DriverManager.getConnection("jdbc:derby:" + name + ";shutdown=true");

      // To shut down a specific database only, but keeep the
      // engine running (for example for connecting to other
      // databases), specify a database in the connection URL:
      // DriverManager.getConnection("jdbc:derby:" + dbName +
      // ";shutdown=true");
    } catch (SQLException se) {

      if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
        // we got the expected exception        
        sLog.info("Derby shut down normally");
        // Note that for single database shutdown, the expected
        // SQL state is "08006", and the error code is 45000.
      } else if ((se.getErrorCode() == 45000)
          && ("08006".equals(se.getSQLState()))) {
        // database is already shutdown
      } else {
        // if the error code or SQLState is different, we have
        // an unexpected exception (shutdown failed)
        sLog.error("Derby did not shut down normally", se);
        printSQLException(se);
      }
    }

  }

  /**
   * Prints details of an SQLException chain to <code>System.err</code>. Details
   * included are SQL State, Error code, Exception message.
   * 
   * @param e
   *          the SQLException from which to print details.
   */
  public static void printSQLException(SQLException e) {
    // Unwraps the entire exception chain to unveil the real cause of the
    // Exception.
    while (e != null) {
      System.err.println("\n----- SQLException -----");
      System.err.println("  SQL State:  " + e.getSQLState());
      System.err.println("  Error Code: " + e.getErrorCode());
      System.err.println("  Message:    " + e.getMessage());
      // for stack traces, refer to derby.log or uncomment this:
      // e.printStackTrace(System.err);
      e = e.getNextException();
    }
  }

  /**
   * Loads the appropriate JDBC driver for this environment/framework. For
   * example, if we are in an embedded environment, we load Derby's embedded
   * Driver, <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
   */
  private void loadDriver() {
    /*
     * The JDBC driver is loaded by loading its class. If you are using JDBC 4.0
     * (Java SE 6) or newer, JDBC drivers may be automatically loaded, making
     * this code optional.
     * 
     * In an embedded environment, this will also start up the Derby engine
     * (though not any databases), since it is not already running. In a client
     * environment, the Derby engine is being run by the network server
     * framework.
     * 
     * In an embedded environment, any static Derby system properties must be
     * set before loading the driver to take effect.
     */
    try {
      Class.forName(driver).newInstance();
    } catch (ClassNotFoundException cnfe) {
      sLog.error("\nUnable to load the JDBC driver " + driver);
      sLog.error("Please check your CLASSPATH.");
      cnfe.printStackTrace(System.err);
    } catch (InstantiationException ie) {
      sLog.error("\nUnable to instantiate the JDBC driver " + driver);
      ie.printStackTrace(System.err);
    } catch (IllegalAccessException iae) {
      sLog.error("\nNot allowed to access the JDBC driver " + driver);
      iae.printStackTrace(System.err);
    }
  }

}
