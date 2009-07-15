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

import javax.sql.DataSource;

/**
 * This is a helper interface implemented by InitializingIdGenerators.
 * It allows the IdGeneratorInitializer to "callback" and get the SQL
 * statement required to initialize a given IdGenerator.
 * @author adamb
 */
public interface InitializingIdGenerator {
  /**
   * Returns the create table statement required for this IdGenerator. 
   */
  public String getCreateStatement();
  
  /**
   * Returns the drop table statement for this IdGenerator
   */
  public String getTableName();
  
  /**
   * Returns the data source used by this IdGenerator
   */
  public DataSource getDataSource();
}
