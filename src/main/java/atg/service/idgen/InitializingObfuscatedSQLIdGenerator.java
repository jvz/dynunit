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

import java.sql.SQLException;

import atg.nucleus.ServiceException;

/**
 * This IDGenerator is intended to be used by unit tests. It will manage it's
 * own database schema rather than assuming the tables already exist. Otherwise
 * it's the same functionality as the ObfuscatedSQLIdGenerator.
 * 
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/service/idgen/
 *          InitializingSQLIdGenerator.java#1 $
 */
public class InitializingObfuscatedSQLIdGenerator extends
    ObfuscatedSQLIdGenerator implements InitializingIdGenerator {

  IdGeneratorInitializer     mInitializer;
  String mCreateStatement = null;

  /**
   * The SQL statement required to create the table used by this component.
   */
  public static final String CREATE_STATEMENT = "create table das_secure_id_gen"
                                                  + " (id_space_name   varchar(60)    not null,"
                                                  + "seed    numeric(19,0)    not null,"
                                                  + "batch_size      integer not null,"
                                                  + "ids_per_batch   integer null,"
                                                  + "prefix  varchar(10),"
                                                  + "suffix  varchar(10),"
                                                  + "constraint das_secure_id_ge_p primary key (id_space_name))";
  
  /**
   * Ensures that the required tables for this id generator exist.
   */
  @Override
  public void doStartService() throws ServiceException {
    if (mInitializer == null)
      mInitializer = new IdGeneratorInitializer(this);
    try {
      mInitializer.initialize();
    } catch (SQLException e) {
      throw new ServiceException(e);
    }
  }

  /**
   * Overrides the default create statement
   */
  public void setCreateStatement(String pStatement) {
    mCreateStatement = pStatement;
  }
  /**
   * Returns the create statement appropriate for the current database
   */
  public String getCreateStatement() {
    if (mCreateStatement == null)
      return CREATE_STATEMENT;
    else
      return mCreateStatement;
  }
}
