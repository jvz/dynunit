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


/**
 * Base class for InitializingDataSource's common functionaliy.
 * 
 * @author adamb
 * @version $Id:$
 */

public class InitializingDataSourceBase extends FakeXADataSource {

	public String mDatabaseName = "testdb";

	/**
	   * Returns the name of the database to use with HSQLDB. The defaut name is
	   * "testdb"
	   * 
	   * @return
	   */
	public String getDatabaseName() {
	    return mDatabaseName;
	  }

	/**
	   * Sets the name of the database to be used with HSQLDB
	   * 
	   * @param pName
	   *          The name of the HSQLDB database to be created when this datasource
	   *          starts up.
	   */
	public void setDatabaseName(String pName) {
	    mDatabaseName = pName;
	  }

}
