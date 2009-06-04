/**
 * Copyright 2009 ATG DUST Project
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

package atg.service.idgen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import atg.nucleus.ServiceException;

/**
 * This IDGenerator is intended to be used by unit tests. It will manage it's
 * own database schema rather than assuming the tables already exist. Otherwise
 * it's the same functionality as the SQLIdGenerator.
 * 
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/service/idgen/InitializingSQLIdGenerator.java#1 $
 */
public class InitializingSQLIdGenerator extends SQLIdGenerator {
  /**
   * 
   */
  private static final String SELECT_COUNT_FROM_DAS_ID_GENERATOR = "select count(*) from das_id_generator";
  /**
   * 
   */
  private static final String DROP_TABLE_DAS_ID_GENERATOR = "DROP TABLE das_id_generator";

  /**
   * Ensures that the required tables for this id generator exist.
   */
  @Override
  public void doStartService() throws ServiceException {
    if (tablesExist()) {
      dropTables();
    }
    initializeTables();
  }

  // --------------------------
  /**
   * Drops the tables required for this component
   */
  private void dropTables() {
    executeUpdateStatement(DROP_TABLE_DAS_ID_GENERATOR);
  }

  // --------------------------
  /**
   * Returns true if the tables required for this component exist
   * 
   * @return
   */
  private boolean tablesExist() {
    boolean exists = false;
    Statement st = null;
    try {
      ResultSet rs = null;
      st = getDataSource().getConnection().createStatement();      
      rs = st.executeQuery(SELECT_COUNT_FROM_DAS_ID_GENERATOR);
      exists = true;
    }
    catch (SQLException e) {
      ; // eat it. table isn't there.
    }
    finally {
      try {
        st.close();
      }
      catch (SQLException e) {
        // eat this too
      }
    }
    return exists;
  }

  // --------------------------
  /**
   * Creates the table required for this component
   */
  private void initializeTables() {
    String statement = getCreateStatement();
    if (isLoggingInfo())
      logInfo("Creating IdGenerator tables : " + statement);
    executeUpdateStatement(statement);
  }

  // --------------------------
  /**
   */
  private void executeUpdateStatement(String pStatement) {
    Statement st = null;
    try {
      st = getDataSource().getConnection().createStatement(); // statements
      int i = st.executeUpdate(pStatement); // run the query
      if (i == -1) {
        if (isLoggingError())
          logError("Error creating tables with statement" + pStatement);
      }
    }
    catch (SQLException e) {
      if (isLoggingError()) logError(e);
    }
    finally {
      try {
        st.close();
      }
      catch (SQLException e) {
        ; // eat it
      }
    }
  }

  /**
   * Returns the create statement appropriate for the current database
   */
  public String getCreateStatement() {
    // TODO Add DBCheck and return DB2 syntax
    return " create table das_id_generator (id_space_name   varchar(60)     not null,"
        + "seed    numeric(19,0)   not null, batch_size      integer not null, prefix  varchar(10)     null,"
        + " suffix  varchar(10)     null, primary key (id_space_name)) ";
  }
}
