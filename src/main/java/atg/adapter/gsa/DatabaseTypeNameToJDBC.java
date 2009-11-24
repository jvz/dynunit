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

package atg.adapter.gsa;

/**
 * The purpose of this class is to map from a the database specific type name
 * for a column to the <code>java.sql.Types</code> for that column. Given a
 * DatabaseTableInfo for a given database it will return the jdbc type for the
 * supported GSA types.
 * 
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/DatabaseTypeNameToJDBC.java#2 $
 * @author adamb
 * @see java.sql.Types
 */
public class DatabaseTypeNameToJDBC {

  public DatabaseTableInfo mTableInfo = null;
  public static final int UNKNOWN = -9999;

  // -------------------------------
  /**
   * Creates a new instance of this class initialized with the given
   * DatabaseTableInfo object.
   * 
   * @param pTableInfo
   */
  public DatabaseTypeNameToJDBC(DatabaseTableInfo pTableInfo) {
    mTableInfo = pTableInfo;
  }

// -------------------------------
  /**
   * Given a database specific type name, returns the matching
   * <code>java.sql.Types</code> constant. If there is no suitable match this
   * method returns the constant <code>UNKNOWN</code>.
   * 
   * @param pTypeName
   * @return
   */
  public int databaseTypeNametoJDBCType(String pTypeName) {
    // Walk the DatabaseTableInfo and do a comparison.
    if (mTableInfo.mVarcharType.equals(pTypeName)) {
      return java.sql.Types.VARCHAR;
    } else if (mTableInfo.mIntType.equals(pTypeName)) {
      // Fix for MS SQLServer
      if("NUMERIC".equals(pTypeName)){
        return java.sql.Types.NUMERIC;
      }else{
        return java.sql.Types.INTEGER;
      }
    } else if (mTableInfo.mBinaryType.equals(pTypeName)) {
      return java.sql.Types.BLOB;
    } else if (mTableInfo.mLongVarcharType.equals(pTypeName)) {
      return java.sql.Types.LONGVARCHAR;
    } else if (mTableInfo.mTimestampType.equals(pTypeName)) {
      return java.sql.Types.TIMESTAMP;
    } else if (mTableInfo.mCharType.equals(pTypeName)) {
      return java.sql.Types.CHAR;
    } else if (mTableInfo.mDateType.equals(pTypeName)) {
      return java.sql.Types.DATE;
    } else if (mTableInfo.mDecimalType.equals(pTypeName)) {
      return java.sql.Types.DECIMAL;
    } else
      return UNKNOWN;
  }
}
