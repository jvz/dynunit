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

package atg.service.idgen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/*
 * IdGeneratorInitializer contains logic used to create and drop the schema used
 * for IdGenerators.
 */
public class IdGeneratorInitializer {

  private static final String     SELECT_COUNT_FROM_TEMPLATE = "select count(*) from das_id_generator";

  private static final String     DROP_TABLE_TEMPLATE        = "DROP TABLE";

  private InitializingIdGenerator mGenerator;
  Logger                          log                        = Logger
                                                                 .getLogger(IdGeneratorInitializer.class);

  /**
   * Creates a new IdGeneratorInitializer used for the given generator,
   * pGenerator.
   * 
   * @param pGenerator
   */
  public IdGeneratorInitializer(InitializingIdGenerator pGenerator) {
    mGenerator = pGenerator;
  }

  /**
   * Creates a new schema for the current generator. If the schema exists, it's
   * dropped and a new one is created.
   * @throws SQLException 
   */
  public void initialize() throws SQLException {
    if (tablesExist()) {
      dropTables();
    }
    initializeTables();
  }

// --------------------------
  /**
   * Drops the tables required for this component
   * @throws SQLException 
   */
  void dropTables() throws SQLException {
    executeUpdateStatement(DROP_TABLE_TEMPLATE + " "
        + mGenerator.getTableName());
  }

  // --------------------------
  /**
   * Returns true if the tables required for this component exist
   * 
   * @return
   */
  boolean tablesExist() {
    boolean exists = false;
    Statement st = null;
    try {
      ResultSet rs = null;
      st = mGenerator.getDataSource().getConnection().createStatement();
      rs = st.executeQuery(SELECT_COUNT_FROM_TEMPLATE + " "
          + mGenerator.getTableName());
      exists = true;
    } catch (SQLException e) {
      ; // eat it. table isn't there.
    } finally {
      try {
        st.close();
      } catch (SQLException e) {
        // eat this too
      }
    }
    return exists;
  }

  // --------------------------
  /**
   * Creates the table required for this component
   * @throws SQLException 
   */
  void initializeTables() throws SQLException {
    String statement = mGenerator.getCreateStatement();
    log.info("Creating IdGenerator tables : " + statement);
    executeUpdateStatement(statement);
  }

  // --------------------------
  /**
   * @return TODO
   * @throws SQLException 
   */
  private boolean executeUpdateStatement(String pStatement) throws SQLException {
    boolean success = false;
    Statement st = null;
    try {
      st = mGenerator.getDataSource().getConnection().createStatement(); // statements
      int i = st.executeUpdate(pStatement); // run the query
      if (i == -1) {
        log.error("Error creating tables with statement" + pStatement);
      }
      success = true;
    } finally {
      try {
        st.close();
      } catch (SQLException e) {
        ; // eat it
      }
    }
    return success;
  }
}
