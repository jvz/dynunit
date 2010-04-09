package atg.dms.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Scanner;

import javax.sql.DataSource;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.util.SqlTokenizer;

import atg.core.io.FileUtils;
import atg.nucleus.NucleusTestUtils;
import atg.nucleus.ServiceException;

/**
 * This SQLJmsManager sub class will create its required database schema on
 * startup and optionally drop the schema (tables) on shutdown. By default this
 * class will use the datasource assigned to the
 * <code>/atg/dynamo/service/jdbc/SQLRepository</code>
 * 
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/dms/sql/InitializingSqlJmsManager.java#1 $
 * @author adamb
 */
public class InitializingSqlJmsManager extends SqlJmsManager {
  public String mSQLRepositoryPath = "/atg/dynamo/service/jdbc/SQLRepository";

  boolean mCreatingSchemaOnStartup = true;
  boolean mDropSchemaOnShutdown = false;
  boolean mDropSchemaOnStartup = true;

  // ---------------------
  /**
   * Creates schema and continues with SQLJms startup.
   */
  public void doStartService() throws ServiceException {
    DataSource ds = getDataSource();
    // get the db type
    logInfo("Database = " + getDatabaseName());
    if (isLoggingInfo())
      logInfo("Initializing schema using datasource " + ds);
    if (mCreatingSchemaOnStartup) {

      createSchema();

    }
    super.doStartService();
  }

  /**
   * Creates the required tables for this component.
   * 
   * @throws FileNotFoundException
   */
  public void createSchema() {
    String ddlPath = getDDLPath(getDatabaseName());
    Platform platform = PlatformFactory
        .createNewPlatformInstance(getDataSource());
    try {
      String sqlString = FileUtils.readFileString(new File(ddlPath));
      platform.evaluateBatch(sqlString, true);
    } catch (FileNotFoundException e) {
      if (isLoggingError())
        logError("Can't create schema. ", e);
    } catch (IOException e) {
      if (isLoggingError())
        logError("Can't create schema. ", e);
    }
  }

  /**
   * Returns the path to the DDL file for the given database.
   * 
   * @param pDatabaseName
   *          = DBName returned from DatabaseMetaData
   * @return
   */
  String getDDLPath(String pDatabaseName) {
    if (pDatabaseName == null)
      return null;
    // Get Dynamo Home or Root
    String dynamoRootEnv = System.getenv("DYNAMO_ROOT");
    String dynamoHomeEnv = System.getenv("DYNAMO_HOME");
    String ddlRelativePath = "/DAS/sql/db_components/";
    String root = (dynamoRootEnv != null ? dynamoRootEnv : dynamoHomeEnv
        + File.separatorChar + "..");
    if (pDatabaseName.toLowerCase().startsWith("hsql")) {
      pDatabaseName = "mysql";
      // Not in the build so take it from source tree
      File configDir = NucleusTestUtils.getConfigpath(
          InitializingSqlJmsManager.class, InitializingSqlJmsManager.class
              .getSimpleName(), false);
      if (!configDir.exists()) {
        if (isLoggingError()) {
          logError("Can't locate sql scripts for HSQLDB. Directory "
              + configDir + " does not exist.");
        }
        return null;
      } else {
        return configDir.getAbsolutePath() + "/hsql/create_sql_jms_ddl.sql";
      }
    } else if (pDatabaseName.toLowerCase().startsWith("db2")) {
      pDatabaseName = "db2";
    } else if (pDatabaseName.toLowerCase().startsWith("microsoft")) {
      pDatabaseName = "mssql";
    }
    return root + ddlRelativePath + pDatabaseName.toLowerCase()
        + "/create_sql_jms_ddl.sql";
  }

  /**
   * Returns the name of the database to which this component is connected.
   * 
   * @return
   */
  public String getDatabaseName() {
    if (getDataSource() == null)
      return null;
    Connection c = null;
    String dbName = null;
    try {
      c = getDataSource().getConnection();
      DatabaseMetaData meta = c.getMetaData();
      dbName = meta.getDatabaseProductName();
    } catch (SQLException e) {
      if (isLoggingError())
        logError("Could not get database type name. ", e);
    } finally {
      if (c != null)
        try {
          c.close();
        } catch (SQLException e) {
          ;
        }
    }
    return dbName;
  }

}
