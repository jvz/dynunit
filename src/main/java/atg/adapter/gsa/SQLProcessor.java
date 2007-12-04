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

package atg.adapter.gsa ;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.log4j.Logger;

import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.GenericService;
import atg.service.jdbc.BasicDataSource;

/** A generic class to execute SQL actions against a database.
 *
 * Parts copied from atg.service.idgen.?? by mgk
 *
 * @author mf
 * @version 1.0
 **/
public
class SQLProcessor
{
  // =============== MEMBER VARIABLES =================
  
  private static Logger log = Logger.getLogger(SQLProcessor.class);

  DataSource mDataSource;
  /** sets the DataSource from which to get DB connections
   **/
  public void setDataSource(DataSource pDataSource) {
	mDataSource = pDataSource; }
  /** returns the DataSource from which db connections are obtained */
  public DataSource getDataSource() {
	return mDataSource;}

  TransactionManager mTxManager;
  /** sets the TransactionManager that should be used to monitor transactions */
  public void setTransactionManager( TransactionManager pManager ) {
      mTxManager = pManager; }
  /** returns the TransactionManager that should be used to monitor transaction */
  public TransactionManager getTransactionManager() {
      return mTxManager; }

  String mDetermineTableExistsSQL = "SELECT * from ";
  /** sets String executed to determine whether a table exists. The table
   * name is appended to the end of the string before execution occurs.
   */
  public void setDetermineTableExistsSQL( String pStr ) {
	  mDetermineTableExistsSQL = pStr; }
  /** returns String executed to determine whether a table exists. The table
   * name is appended to the end of the string before execution occurs.
   */
  public String getDetermineTableExistsSQL() {
	  return mDetermineTableExistsSQL; }

  String mDropTableSQL = "DROP TABLE ";
  /** sets String executed to drop a table.  The table name is appended to the
   * end of the string before execution
   */
  public void setDropTableSQL( String pStr ) {
	  mDropTableSQL = pStr; }
  /** returns String executed to drop a table.  The table name is appended to the
   * end of the string before execution
   */
  public String getDropTableSQL() {
	  return mDropTableSQL; }

  /** String delimiter used to separate a large String passed to
   * createTables() into an array of individual Create Table statements
   * default value is "CREATE TABLE"
   * This delimiter _will_ be included in the final create statements
   */
  String mCreateTableBeginDelimiter = "CREATE TABLE ";
  public void setCreateTableBeginDelimiter( String pStr ) {
	  mCreateTableBeginDelimiter = pStr; }
  public String getCreateTableBeginDelimiter() {
	  return mCreateTableBeginDelimiter; }

  /** String delimiter used to separate a large String passed to
   * createTables() into an array of individual Create Table statements
   * default value is ";"
   * This delimiter _will not_ be included in the final create statements
   */
  String mCreateTableEndDelimiter = ";";
  public void setCreateTableEndDelimiter( String pStr ) {
	  mCreateTableEndDelimiter = pStr; }
  public String getCreateTableEndDelimiter() {
	  return mCreateTableEndDelimiter; }

   /** an optional GenericService component whose logging services should be used by
    *  this component.
    */
   private GenericService mLogger;
   public void setLoggingManager( GenericService pLogger ) {
      mLogger = pLogger; }
   public GenericService getLoggingManager() {
      return mLogger; }

  // indicates whether to set autoCommit(true) on connections
  private boolean mAutoCommit = false;
  /** if set to true, then autoCommit will be set to true on all connections used to
   *  execute SQL.  otherwise, autoCommit will not be altered from what is set by the
   *  DataSource.
   */
  public void setAutoCommit( boolean pCommit ) {
      mAutoCommit = pCommit; }
  /** returns true if autoCommit should be set to true on all connections used to execute
   *  SQL.
   */
  public boolean isSetAutoCommit() {
      return mAutoCommit; }

  /* =========== CONSTRUCTORS ============= */

  /** Construct with specified DataSource
   *
   *  @param TransactionManager manager - the TransactionManager to use to monitor transactions
   *  @param DataSource dataSource - the DataSource to use for db connections
   **/
  public SQLProcessor( TransactionManager pTxManager, DataSource pDataSource )
  {
      setDataSource( pDataSource );
      setTransactionManager( pTxManager );
  }

  /** Constructor with specified user/password/driver/URL.  specified parameters are used
   *  to create a DataSource connection to the database.
   *
   *  @param TransactionManager manager - the TransactionManager to use to monitor transactions
   *  @param String username - name of user to connect to db
   *  @param String password - pwd to connectc to db
   *  @param String driver - driver specification to connect to db
   *  @param String url - url to connect to db
   *  @exception SQLException if an error occurs creating the DataSource
   */
  public SQLProcessor( TransactionManager pTxManager, String pUsername, String pPassword, String pDriver, String pURL )
      throws SQLException
  {
      setDataSource( createBasicDataSource( pUsername, pPassword, pDriver, pURL ) );
      setTransactionManager( pTxManager );
  }

  // ==================== PUBLIC METHODS ===========================

  /** creates and returns a DataSource based on the user/pwd/driver/url info
   *  supplied.
   */
   public static DataSource createBasicDataSource( String pUsername,
                                              String pPassword,
                                              String pDriver,
                                              String pURL )
   {
        BasicDataSource datasource = new BasicDataSource();
        datasource.setUser( pUsername );
        datasource.setPassword( pPassword );
        datasource.setDriver( pDriver );
        datasource.setURL( pURL );

        return datasource;
   }

  /**
   * Perform the specified SQL statement in a new transaction which is commited.  Autocommit
   * on the connection is set to true if isSetAutoCommit() is true.
   *
   * @param pSQL SQL to execute
   *
   * @exception SQLException if there is DB problem
   * @exception TransactionDemarcationException if there is a tx problem
   **/
    public void executeSQL(String pSQL)
      throws SQLException, TransactionDemarcationException
    {
      TransactionDemarcation td = new TransactionDemarcation();
      try
        {
          td.begin ( getTransactionManager(), TransactionDemarcation.REQUIRES_NEW);
          Connection c = null;
          Statement s = null;
          try
            {
              // get DB connection
              c = getConnection();
              if ( isSetAutoCommit() )
                c.setAutoCommit( true );

              //most of this method is annoying try/catch/finally blocks
              //inflicted on us by JTA. the real work is here.
              s = c.createStatement();
              debug("Executing SQL [" + pSQL + "]");
              s.execute(pSQL);
            }
           finally
            {
              close(s);
              close(c);
            }
        }
       finally
        {
              td.end();
        }
    }

    /** executes the specified query and returns a List of values for the specified column name.
     *  for example, executeQuery( "select * from user", "first_name" ) would return a List of
     *  the first names of all entries in the user table.
     *
     *  @return List of Object values
     *  @exception SQLException if a sql error occurs
     *  @exception TransactionDemarcationException if a tx error occurs
     */
     public List<?> executeQuery( String pQuery, String pColumnName )
        throws SQLException, TransactionDemarcationException
     {
        List<Object> results = new LinkedList<Object>();
        TransactionDemarcation td = new TransactionDemarcation();
        //int rows = 0;
        try
          {
            td.begin ( getTransactionManager(), TransactionDemarcation.REQUIRES_NEW);
            Connection c = null;
            Statement s = null;
            ResultSet rs = null;
            try
              {
                // get DB connection
                c = getConnection();

                //most of this method is annoying try/catch/finally blocks
                //inflicted on us by JTA. the real work is here.
                s = c.createStatement();
                debug("Executing query [" + pQuery + "]");
                rs = s.executeQuery( pQuery );

                while ( rs.next() ) {
                    results.add( rs.getObject( pColumnName) );
                }
              }
             finally
              {
                close(rs);
                close(s);
                close(c);
              }
          }
         finally
          {
                td.end();
          }
          return results;
     }

    /** Method that iteratively attempts to drop tables.  An iterative
     * effort is utilized in case references exist between tables.
     *
     * ASSUMPTION: references only exist between tables specified in the
     * List.  If references exist from tables outside the List, then some
     * tables may not be able to be dropped and this method will throw a
     * SQLException
     *
     * @param Collection of names of tables to be dropped
     * @param boolean cascadeConstraints.  true if 'CASCADE CONSTRAINTS' should be used in
     * drop statement.
     * @param boolean preview. if true then iterative behavior is disabled and method simply
     * prints one drop statement that would be executed for each table.  iterative behavior has
     * to be disabled since it doesn't make sense if drops are not being executed.
     * @exception SQLException thrown if all tables can not be dropped
     */
    public void dropTables( Collection<String> pNames, boolean pCascadeConstraints, boolean pPreview )
            throws SQLException, TransactionDemarcationException
    {
      // just show drops once if preview is true
      if ( pPreview ) {
          Iterator<String> tables = pNames.iterator();
          while ( tables.hasNext() ) {
              dropTable(tables.next(), pCascadeConstraints, pPreview );
          }
          return;
      }

      // assuming only one table can be dropped each time, this should take
      // at most n iterations where n is the nbr of tables being dropped
      int maxIterations = pNames.size();

      // every table is tried at least once
      Collection<String> tablesToDrop = pNames;

      List<String> remainingTables;
      int attempt = 0;
      do {
              remainingTables = new ArrayList<String>();
              Iterator<String> tables = tablesToDrop.iterator();
              while ( tables.hasNext() ) {
                      String table =  tables.next();
                      if ( tableExists( table ) ) {
                              try {
                                      dropTable( table, pCascadeConstraints, pPreview );
                                      debug( "Dropped table: " + table);
                              } catch ( SQLException se ) {
                                      // if this is the last iteration, throw an exception
                                      if ( attempt+1 >= maxIterations )
                                              throw se;

                                      // otherwise track this table for the next try
                                      remainingTables.add( table );
                              }
                      }
              }
              tablesToDrop = remainingTables;

      } while ( ( attempt++ < maxIterations )
                       && ( ! remainingTables.isEmpty() ) );
    }

    // ====================== PRIVATE METHODS ==========================

      /**
       * Get a DB connection
       * @return the connection
       * @exception SQLProcessorException if there is DB trouble or
       * DataSource trouble
       **/
      Connection getConnection()
        throws SQLException
      {
            if (getDataSource() == null)
                            throw new SQLException("DataSource is null.");

            return getDataSource().getConnection();
      }

      /**
       * Close a DB connection. It is okay to pass a null connection here
       *
       * @param pConnection connection to close, may be null
       * @exception SQLException if an error occurs trying to close a non-null connection
       **/
      private final void close(Connection pConnection)
          throws SQLException
      {
        if (pConnection != null)
            pConnection.close();
      }

      /**
       * Close a result set. It is okay to pass a null here
       *
       * @param pResultSet result set to close, may be null
       * @exception SQLException if an error occurs closing a non-null ResultSet
       **/
      private final void close(ResultSet pResultSet)
          throws SQLException
      {
        if (pResultSet != null)
            pResultSet.close();
      }

      /**
       * Close a statement. It is okay to pass a null here.
       *
       * @param pStatement statement to close, may be null
       * @exception SQLException if an error occurs closing a non-null Statement
       **/
      private final void close(Statement pStatement)
          throws SQLException
      {
        if (pStatement != null)
            pStatement.close();
      }

      /** This method is used to execute a 'Drop Table' call. The
       * method creates the drop table statement by appending the name
       * passed as a method with the SQL that has been set as the dropTableSQL
       * property.  By default, this property is set to "Drop table"
       *
       * @param String - the name of the table to drop
       * @param boolean cascadeConstraints.  true if 'CASCADE CONSTRAINTS' should be used in
       * drop statement.
       * @exception SQLException thrown if an error occurs trying
       * to drop the table
       */
        private void dropTable( String pName, boolean pCascadeConstraints, boolean pPreview )
            throws SQLException, TransactionDemarcationException
        {
          String sql = getDropTableSQL() + " " + pName;
          if ( pCascadeConstraints ) sql = sql + " CASCADE CONSTRAINTS";

          if ( pPreview ) log.info( sql );
          else executeSQL( sql );
       }

      /**
       * Method to determine whether a table already exists in the
       * database.  The method operates by appending the name passed
       * as a parameter to the String that has been set in the
       * determineTableExistsSQL property
       *
       * @param String - name of table to check for existence of
       * @return boolean - true if table exists; false otherwise
       * @exception TransactionDemarcationException if a tx error occurs
       */
      private boolean tableExists( String pTableName )
          throws TransactionDemarcationException
      {
              // don't bother with query if name is invalid
              if ( pTableName == null ||
                       pTableName.length() == 0 )
                      return false;

              // create sql
              String sql = getDetermineTableExistsSQL() + " " + pTableName;

              // execute and check for an exception
              try {
                      executeSQL( sql );
              } catch ( SQLException spe ) {
                    // we should only get an exception here if the table doesn't exist
                    // so just return false
                    return false;
              }

              return true;
      }

      /** a utility method to assist with logging */
      private void debug( Object pMsg ) {
          if ( getLoggingManager() != null && getLoggingManager().isLoggingDebug() )
              getLoggingManager().logDebug("SQLProcessor: " + pMsg.toString());
      }
}
