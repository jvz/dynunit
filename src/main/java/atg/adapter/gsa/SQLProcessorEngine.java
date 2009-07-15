/**
 * Copyright 2007 ATG DUST Project Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package atg.adapter.gsa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

/**
 * This class is designed to assist with database table manipulation such as
 * adding tables. Parts copied from atg.service.idgen.?? by mgk
 * 
 * @author mf
 * @version 1.0
 **/
public class SQLProcessorEngine extends GenericService {

  // Vendor String for Apache Derby
  public static final String APACHE_DERBY = "Apache Derby";

  /* =========== CONSTRUCTORS ============= */

  /**
   * empty constructor
   */
  public SQLProcessorEngine() {
  };

  /**
   * Construct a generator
   **/
  public SQLProcessorEngine(GSARepository pRep) {
    setRepository(pRep);
    mDataSource = getRepository().getDataSource();
  }

  // ---------- Property: DataSource ----------
  /**
   * DataSource from which to get DB connections this property is NOT a public
   * property because it is extracted from the repository property
   */
  DataSource mDataSource;

  private void setDataSource(DataSource pDataSource) {
    mDataSource = pDataSource;
  }

  private DataSource getDataSource() {
    return mDataSource;
  }

  /**
   * GSARespository from which to get the DataSource and TransactionManager
   */
  GSARepository mRepository;

  public void setRepository(GSARepository pRep) {
    mRepository = pRep;
  }

  public GSARepository getRepository() {
    return mRepository;
  }

  /**
   * String executed to determine whether a table exists. The table name is
   * appended to the end of the string before execution occurs.
   */
  String mDetermineTableExistsSQL = "SELECT count(*) from";

  public void setDetermineTableExistsSQL(String pStr) {
    mDetermineTableExistsSQL = pStr;
  }

  public String getDetermineTableExistsSQL() {
    return mDetermineTableExistsSQL;
  }

  /**
   * String executed to drop a table. The table name is appended to the end of
   * the string before execution
   */
  String mDropTableSQL = "DROP TABLE";

  public void setDropTableSQL(String pStr) {
    mDropTableSQL = pStr;
  }

  public String getDropTableSQL() {
    return mDropTableSQL;
  }

  /**
   * String delimiter used to separate the large String passed to createTables()
   * into an array of individual Create Table statements default value is
   * "CREATE TABLE" This delimiter _will_ be included in the final create
   * statements
   */
  String mCreateTableBeginDelimiter = "CREATE TABLE";

  public void setCreateTableBeginDelimiter(String pStr) {
    mCreateTableBeginDelimiter = pStr;
  }

  public String getCreateTableBeginDelimiter() {
    return mCreateTableBeginDelimiter;
  }

  /**
   * String delimiter used to separate the large String passed to createTables()
   * into an array of individual Create Table statements default value is ";"
   * This delimiter _will not_ be included in the final create statements
   */
  String mCreateTableEndDelimiter = ";";

  public void setCreateTableEndDelimiter(String pStr) {
    mCreateTableEndDelimiter = pStr;
  }

  public String getCreateTableEndDelimiter() {
    return mCreateTableEndDelimiter;
  }

  // -------------------------------------

  /**
   * method to execute when starting this component
   */
  public void doStartService() throws ServiceException {
    if (getRepository() == null)
      throw new ServiceException("Repository property is null.");

    setDataSource(getRepository().getDataSource());
  }

  /**
   * Get a DB connection
   * 
   * @return the connection
   * @exception SQLProcessorException
   *              if there is DB trouble or DataSource trouble
   **/
  Connection getConnection() throws SQLProcessorException {
    try {
      DataSource ds = getDataSource();
      if (ds == null)
        throw new SQLProcessorException("no DataSource");
      else
        return ds.getConnection();
    } catch (SQLException sqle) {
      if (isLoggingDebug()) {
        SQLException next = sqle;
        while (next != null) {
          logDebug(next);
          next = next.getNextException();
        }
      }
      throw new SQLProcessorException(sqle);
    }
  }

  // -------------------------------------
  /**
   * Close a DB connection, logging any SQLExceptions. It is okay to pass a null
   * connection here
   * 
   * @param pConnection
   *          connection to close, may be null
   **/
  private final void close(Connection pConnection) {
    if (pConnection != null) {
      try {
        pConnection.close();
      } catch (SQLException sqle) {
        if (isLoggingError())
          logError(sqle);
      }
    }
  }

  // -------------------------------------
  /**
   * Close a result set, logging any SQLExceptions. It is okay to pass a null
   * here
   * 
   * @param pResultSet
   *          result set to close, may be null
   **/
// private final void close(ResultSet pResultSet)
// {
// if (pResultSet != null)
// {
// try
// {
// pResultSet.close();
// }
// catch (SQLException sqle)
// {
// if (isLoggingError())
// logError(sqle);
// }
// }
// }

  // -------------------------------------
  /**
   * Close a statement, logging any SQLExceptions. It is okay to pass a null
   * here.
   * 
   * @param pStatement
   *          statement to close, may be null
   **/
  private final void close(Statement pStatement) {
    if (pStatement != null) {
      try {
        pStatement.close();
      } catch (SQLException sqle) {
        if (isLoggingError())
          logError(sqle);
      }
    }
  }

  // -------------------------------------
  /**
   * Perform the specified SQL statement in a new transaction which is commited.
   * 
   * @param pSQL
   *          SQL to execute
   * @return the # of rows affected
   * @exception SQLProcessorException
   *              if there is DB or xact trouble
   **/
  private int performSQL(String pSQL) throws SQLProcessorException {
    TransactionDemarcation td = new TransactionDemarcation();
    SQLProcessorException error = null;
    int rows = 0;
    try {
      td.begin(mRepository.getTransactionManager(),
          TransactionDemarcation.REQUIRES_NEW);
      Connection c = null;
      Statement s = null;
      try {
        // get DB connection
        c = getConnection();

        /*
         * * most of this method is annoying try/catch/finally blocks* inflicted
         * on us by JTA. the real work is here.
         */
        s = c.createStatement();
        // rows = s.executeUpdate(pSQL);
        s.execute(pSQL);
      } catch (SQLException sqle) {
        error = new SQLProcessorException(sqle);
      } finally {
        close(s);
        close(c);
      }
    } catch (TransactionDemarcationException e1) {
      if (error == null)
        error = new SQLProcessorException(e1);
      else if (isLoggingError())
        logError(e1);
    } finally {
      try {
        td.end();
      } catch (TransactionDemarcationException e2) {
        if (error == null)
          error = new SQLProcessorException(e2);
        else if (isLoggingError())
          logError(e2);
      }
    }

    if (error != null)
      throw error;
    else
      return rows;
  }

  /**
   * This method is used to create tables in a database. It takes a String that
   * contains all of the table creation statements and is of the format: CREATE
   * TABLE foo ( <field specifications> ); ... CREATE TABLE bar ( <field
   * specifications> ); Specifically, this is the format output by the GSA when
   * a call is made to generateSQL(); Before making the tables, this large
   * String will be split apart into an array of individual CREATE TABLE
   * statements using the createTableBeginDelimiter and createTableEndDelimiter
   * properties. By default, createTableBeginDelimiter = "CREATE TABLE" and
   * createTableEndDelimiter = ";"
   * 
   * @param String
   *          pStr - the String containing the CREATE TABLE statements
   * @param boolean pDrop - indicates whether to drop tables and recreate them
   *        if the tables already exist in the database
   * @return boolean true if any tables were created ( or dropped and created )
   * @exception SQLProcessorException
   *              if an error occurs trying to create the tables
   */
  public boolean createTables(List<String> pStatements, boolean pDrop)
      throws SQLProcessorException {
    boolean createdTables = false;

    // get the create statements to execute and make sure they are
    // in the proper order with regard to 'references' clauses
    if (isLoggingDebug())
      logDebug("Reordering CREATE TABLE statements so references don't fail...");
    List<String> statements = reorderCreateStatements(pStatements);

    // if dropping tables, do that before trying to create them
    // throws exception if all tables can't be dropped
    List<String> tableNames = getTableNames(statements);
    if (pDrop) {
      if (isLoggingInfo())
        logInfo("Dropping tables...");
      dropTables(tableNames);
    }

    // we can assume that if a table still exists it is because we
    // didn't try to drop it. If we did try to drop it but couldn't,
    // dropTables would throw an exception and this code would never
    // be executed
    if (isLoggingInfo())
      logInfo("Creating tables...");
    Iterator<String> iter = statements.iterator();
    while (iter.hasNext()) {
      String statement = iter.next();
      String name = getTableName(statement);
      boolean exists = tableExists(name);

      if (name != null && !exists) {
        if (isLoggingDebug())
          logDebug("Creating table: " + name);
        if (this.getRepository() instanceof InitializingGSA) {
          if (!isLoggingDebug()
              && ((InitializingGSA) this.getRepository())
                  .isLoggingCreateTables())
            logDebug(statement);
        }
        if (this.getRepository() instanceof InitializingVersionRepository) {
          if (!isLoggingDebug()
              && ((InitializingVersionRepository) this.getRepository())
                  .isLoggingCreateTables())
            logDebug(statement);
        }
        if (isDerby())
          statement = stripNull(statement);
        createTable(statement);
        createdTables = true;
      } else if (name != null && !pDrop) {
        if (isLoggingInfo())
          logInfo("Table already exists and dropTablesIfExist is false - not creating: "
              + name);
        // dropExistingTables must be false or else table would have been
        // dropped
      } else {
        // throw new SQLProcessorException("The table " + name +
        // " was not created because name was null or table couldn't be dropped.");
        logWarning("The table "
            + name
            + " was not created because name was null or table couldn't be dropped.");
      }
    }

    return createdTables;
  }

  /**
   * Removes any occurrence of the string "NULL" from a create statement if it
   * is not preceded by the word "NOT".
   * 
   * @param statement
   * @return
   */
  private String stripNull(String statement) {
    // first make this all uppercase

    StringBuffer subStatements = new StringBuffer();
    String tempStatement = statement.toUpperCase();
    StringTokenizer st = new StringTokenizer(tempStatement, ",");
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      int notNullIndex = tok.indexOf("NOT NULL");
      if (notNullIndex > -1) {
        // safe to return this unmodified
        subStatements.append(tok + ",\n");
      } else if (tok.indexOf("NULL") > -1) {
        // need to strip this one.
        // we assume that we can just remove the five characters above
        String temp = StringUtils.replace(tok, "NULL", "");
        // we also have to remove all the trailing spaces
        subStatements.append(temp.trim() + ",\n");
      } else {
        // safe to return. no null at all.
        if (st.hasMoreTokens())
          subStatements.append(tok + ",\n");
        else
          // End of statement, so no comma
          subStatements.append(tok);
      }
    }
    return subStatements.toString();
  }

  private boolean mIsDerby    = false;
  private boolean mIsDerbySet = false;

  /**
   * Returns true if the current database is Apache Derby. The first invocation
   * to this method will cache its answer.
   */
  public boolean isDerby() throws SQLProcessorException {
    if (!mIsDerbySet) {
      mIsDerby = isDerbyUncached();
      mIsDerbySet = true;
    }
    return mIsDerby;
  }

  /**
   * Returns true if the current database is Apache Derby This method call is
   * not cached and will make a database connection attempt on each invocation.
   * 
   * @return
   * @throws SQLProcessorException
   */
  private boolean isDerbyUncached() throws SQLProcessorException {
    boolean isDerby = false;
    Connection c = null;
    try {
      c = getConnection();
      DatabaseMetaData meta = c.getMetaData();
      if (APACHE_DERBY.equals(meta.getDatabaseProductName())) {
        isDerby = true;
      }
      return isDerby;
    } catch (SQLException e) {
      throw new SQLProcessorException(e);
    } finally {
      if (c != null) {
        try {
          c.close();
        } catch (SQLException e) {
          ; // eat it
        }
      }
    }
  }

  /**
   * This is a method that is used to execute a 'CREATE TABLE' call. The String
   * you pass in is expected to be of the format CREATE TABLE ( ..... )
   * 
   * @return void
   * @exception SQLProcessorException
   *              thrown if an error occurs creating the table
   */
  private void createTable(String pStr) throws SQLProcessorException {
    try {
      performSQL(pStr);
    } catch (SQLProcessorException spe) {
      throw new SQLProcessorException(
          "Caught exception executing create table statement \"" + pStr + "\"",
          spe);
    }
  }

  /**
   * This method is used to iteratively drop tables. The iterative effort is
   * necessary because tables may have references. ASSUMPTION: references only
   * exist for tables that are defined within this repository. If references
   * exist from tables outside this repository, this method will throw a
   * SQLProcessorException
   * 
   * @param Vector
   *          of CREATE TABLE statements indicating which tables to drop
   * @exception SQLProcessorException
   *              thrown if all tables can not be dropped
   */
  public void dropTablesFromCreateStatements(List<String> pCreateStatements)
      throws SQLProcessorException {
    List<String> names = getTableNames(pCreateStatements);
    dropTables(names);
  }

  /**
   * This method is used to iteratively drop tables. The iterative effort is
   * necessary because tables may have references. ASSUMPTION: references only
   * exist for tables that are defined within this repository. If references
   * exist from tables outside this repository, this method will throw a
   * SQLProcessorException
   * 
   * @param Vector
   *          of names of tables to be dropped
   * @exception SQLProcessorException
   *              thrown if all tables can not be dropped
   */
  private void dropTables(List<String> pNames) throws SQLProcessorException {
    // assuming only one table can be dropped each time, this should take
    // at most n iterations where n is the nbr of tables being dropped
    int maxIterations = pNames.size();

    // every table is tried at least once
    List<String> tablesToDrop = pNames;

    List<String> remainingTables;
    int attempt = 0;
    do {
      remainingTables = new ArrayList<String>();
      Iterator<String> tables = tablesToDrop.iterator();
      while (tables.hasNext()) {
        String table = tables.next();
        if (tableExists(table)) {
          try {
            logInfo("Attempting to drop table: " + table);
            dropTable(table);
            logInfo("Dropped table: " + table);
          } catch (SQLProcessorException spe) {
            // if this is the last iteration, throw an exception
            if (attempt + 1 >= maxIterations)
              throw spe;

            // otherwise track this table for the next try
            remainingTables.add(table);
          }
        }
      }
      tablesToDrop = remainingTables;

    } while ((attempt++ < maxIterations) && (!remainingTables.isEmpty()));
  }

  /**
   * This is a method that is used to execute a 'Drop Table' call. The method
   * creates the drop table statement by appending the name passed as a method
   * with the SQL that has been set as the dropTableSQL property. By default,
   * this property is set to "Drop table"
   * 
   * @param String
   *          - the name of the table to drop
   * @exception SQLProcessorException
   *              thrown if an error occurs trying to drop the table
   */
  private void dropTable(String pName) throws SQLProcessorException {
    String sql = getDropTableSQL() + " " + pName;

    try {

      logDebug("Attempting to drop table: " + pName);

      performSQL(sql);

    } catch (SQLProcessorException spe) {
      throw new SQLProcessorException(
          "Caught exception executing drop table statement \"" + sql + "\"",
          spe);
    }
  }

  /**
   * This method is used to extract table names from a Vector of CREATE
   * statements returned by either a call to getCreateStatements() or
   * getOrderedCreateStatements()
   * 
   * @return Vector of table names
   */
  private List<String> getTableNames(List<String> pStatements) {
    if (isLoggingDebug())
      logDebug("Getting table names...");

    List<String> names = new ArrayList<String>();

    // split the big string into a bunch of create table statements
    List<String> createStatements = pStatements;

    // now get the table name from each statement
    Iterator<String> iter = createStatements.iterator();
    while (iter.hasNext()) {
      String thisName = getTableName(iter.next());

      if (thisName != null && !names.contains(thisName)) {
        names.add(thisName);
        if (isLoggingDebug())
          logDebug("Found table name: " + thisName);
      }
    }

    return names;
  }

  /**
   * This is a method used to extract the table name from a CREATE TABLE
   * statement. It operates by finding the createTableBeginDelimiter and
   * extracting the next word after the delimiter.
   * 
   * @param String
   *          - the create table statement
   * @return String - the name of the table; null if name can't be found
   */
  private String getTableName(String pStr) {
    String STATEMENT_BEGIN = getCreateTableBeginDelimiter();

    if (isLoggingDebug() && (this.getRepository().getDebugLevel() > 6)) {
      logDebug("Extracting table name from create table statement: " + pStr);
      logDebug("Name is taken as word after createTableBeginDelimiter.  Delimiter is set to: "
          + getCreateTableBeginDelimiter());
    }

    int index = pStr.indexOf(STATEMENT_BEGIN);
    if (index == -1) {
      if (isLoggingWarning())
        logWarning("Could not extract name because start delimiter could not be found.  Returning null.");
      return null;
    }

    pStr = pStr.substring(index + STATEMENT_BEGIN.length());
    // loop to get rid of any spaces immediately after the
    // start delimiter
    while (pStr.startsWith(" ") && (pStr.length() > 1)) {
      pStr = pStr.substring(1);
    }

    int first_blank = pStr.indexOf(" ");
    if (!(first_blank > 0)) {
      if (isLoggingDebug())
        logDebug("Could not extract name because no word was found after the start delimiter.  Returning null.");
      return null;
    }

    String name = pStr.substring(0, first_blank);
    if (isLoggingDebug())
      logDebug("Extracted table name: " + name);
    return name;
  }

  /**
   * This method is used to break the large string passed to createTables() into
   * an array of CREATE TABLE statements. The string is split apart using the
   * createTableBeginDelimiter and createTableEndDelimiter Strings. These can be
   * set as the createTableBeginDelimiter and createTableEndDelimiter
   * properties.
   * 
   * @param String
   *          - String containing all the Create Table statements
   * @return Vector of CREATE TABLE statements
   */
  /*
   * private Vector getCreateStatements( String pStr ) { String STATEMENT_BEGIN
   * = getCreateTableBeginDelimiter(); String STATEMENT_END =
   * getCreateTableEndDelimiter();
   * 
   * Vector statements = new Vector();
   * 
   * // we need to make sure we strip off the potential 'missing tableinfos...'
   * // error, so we do that by initially recopying the string from the first //
   * CREATE TABLE. also with this check we catch the condition where the //
   * string being processed has no CREATE TABLE clauses int index =
   * pStr.indexOf( STATEMENT_BEGIN );
   * 
   * // now loop through and extract all of the CREATE statements String
   * remaining = pStr.substring( index ); while ( index != -1 ) { int stop =
   * remaining.indexOf( STATEMENT_END ); if ( stop == -1 ) { // error - this
   * string is malformed wrt what we expected because no end delimiter was found
   * if ( isLoggingError() )
   * logError("malformed string passed to getCreateStatements - an end delimiter '"
   * + STATEMENT_END +
   * "' could not be found. Abandoning parsing of Create table statements.");
   * break; }
   * 
   * String thisCreate = remaining.substring(0,stop); if ( isLoggingDebug() )
   * logDebug("Parsed create statement: " + thisCreate ); statements.add(
   * thisCreate );
   * 
   * // now see if there are any more statements remaining =
   * remaining.substring( stop + 1 ); index = remaining.indexOf( STATEMENT_BEGIN
   * ); // need to put this here so that we skip everything between the end of
   * the previous // CREATE TABLE and the beginning of the next if ( index != -1
   * ) remaining = remaining.substring( index ); }
   * 
   * if ( isLoggingDebug() ) logDebug("Found " + statements.size() +
   * " create statements.");
   * 
   * return statements; }
   */

  /**
   * This method is used to order CREATE TABLE statments such that we do not try
   * to create a table before any tables that it references. NOTE: if a
   * reference exists for a table outside of this repository we will print a
   * warning, but will _not_ throw an exception. If the referenced table doesn't
   * exist, an exception will be thrown when the referencing table is created.
   * 
   * @param String
   *          containing all of the CREATE TABLE statements as generated by a
   *          call to GSARepository.generateSQL()
   * @return Vector of individual CREATE statements that are in the proper order
   *         to execute
   * @exception SQLProcessorException
   *              if we detect a bad loop trying to resolve references
   */
  private List<String> reorderCreateStatements(List<String> pStatements)
      throws SQLProcessorException {
    List<String> statements = pStatements;
    List<String> names = getTableNames(statements);
    List<String> orderedStatements = new ArrayList<String>();

    // hashmap containing one entry for every table that references
    // another, and holds Vector of those tables it is waiting to be made
    HashMap<String, List<String>> refersTo = new HashMap<String, List<String>>();
    // hashmap containing one entry for every table that is references by
    // another, and holds Vector of all the tables that reference it
    HashMap<String, List<String>> referencedBy = new HashMap<String, List<String>>();

    // setup the tables so we know who makes which references
    Iterator<String> iter = statements.iterator();
    while (iter.hasNext()) {
      String statement = iter.next();
      String tableName = getTableName(statement);
      List<String> references = getTableReferences(statement, tableName);

      if (references.size() < 1) {
        orderedStatements.add(statement);
      } else {
        // organize the references this table has
        if (!checkReferencesInRepository(names, references)) {
          if (isLoggingWarning())
            logWarning("Table " + tableName
                + " references a table outside the repository.");
        }

        // create an entry in 'refersTo' for this table
        refersTo.put(tableName, references);

        // update referencedBy to include this table
        Iterator<String> refs = references.iterator();
        while (refs.hasNext()) {
          String ref = refs.next();
          List<String> v;
          if (!referencedBy.containsKey(ref)) {
            v = new ArrayList<String>();
            v.add(tableName);
            referencedBy.put(ref, v);
          } else {
            v = referencedBy.get(ref);
            v.add(tableName);
          }
        }
      }
    }

    // removed all of the previously the ordered statements
    iter = orderedStatements.iterator();
    while (iter.hasNext()) {
      String statement = iter.next();
      statements.remove(statement);
    }

    // now that we know all the references, order them appropriately
    // assuming we add one table per loop, this should take at most
    // n iterations where n is the starting number of statements to add
    int maxTries = statements.size();
    int attempt = 0;
    while (statements.size() > 0) {
      Iterator<String> iterator = statements.iterator();
      List<String> newlyAdded = new ArrayList<String>();
      while (iterator.hasNext()) {
        String statement = iterator.next();
        String tableName = getTableName(statement);

        // is this table isn't waiting for another table, add it
        if (!refersTo.containsKey(tableName)) {
          // this would be an error condition !!
        } else {
          List<String> waitingOnTables = refersTo.get(tableName);
          boolean okToAdd = true;
          Iterator<String> i = waitingOnTables.iterator();
          while (i.hasNext()) {
            String waitingOn = i.next();
            if (refersTo.containsKey(waitingOn)) {
              okToAdd = false;
            }
          }

          if (okToAdd) {
            orderedStatements.add(statement);
            newlyAdded.add(statement);
            // let the other tables know this one is made
            if (referencedBy.containsKey(tableName)) {
              List<String> tablesWaiting = referencedBy.get(tableName);
              Iterator<String> j = tablesWaiting.iterator();
              while (j.hasNext()) {
                String table = j.next();
                List<String> v = refersTo.get(table);
                v.remove(tableName);
              }
            }
          }
        }
      }

      // after each iteration, remove the newlyAdded statements from the list
      Iterator<String> k = newlyAdded.iterator();
      while (k.hasNext()) {
        String s = k.next();
        statements.remove(s);
      }

      // make sure we aren't looping infinitely
      if (attempt++ > maxTries) {
        if (isLoggingError()) {
          logError("Still trying to resolve: ");
          Iterator<String> left = statements.iterator();
          while (left.hasNext()) {
            String table = left.next();
            logError(table);
          }
        }
        throw new SQLProcessorException(
            "Could not order tables appropriately...failing.  Turn on loggingDebug for more info.");
      }
    }

    return orderedStatements;
  }

  /**
   * This method is used to extract the names of other tables that a table
   * references. expected format is: CREATE TABLE foo ( x int not null
   * references bar(id), y varchar null references doo(id), ... )
   * 
   * @param String
   *          the CREATE TABLE statement
   * @param String
   *          the name of the table
   * @return Vector containing names of referenced tables
   * @exception SQLProcessorException
   *              if the table has a reference to itself
   */
  private List<String> getTableReferences(String pStr, String tableName)
      throws SQLProcessorException {
    String REFERENCES = " references ";
    List<String> refs = new ArrayList<String>();

    int start = pStr.toLowerCase().indexOf(REFERENCES);
    while (start != -1) {
      pStr = pStr.substring(start + REFERENCES.length());
      String ref = pStr;
      // stop at a '('
      int stop = ref.indexOf("(");
      ref = ref.substring(0, stop);
      // remove spaces
      ref = ref.trim();

      // bail if the table references itself - i think that is just wrong
      /*
       * actually, jeff and mike said this should be allowed, so i won't throw
       * an exception, i just won't add it to the list of references either...
       */
      if (ref.equalsIgnoreCase(tableName)) {
        // do nothing
        // throw new SQLProcessorException("The create statement for table " +
        // tableName +
        // " contains a reference to itself.");
      } else if ((ref.length() > 0) && !refs.contains(ref))
        refs.add(ref);

      start = pStr.toLowerCase().indexOf(REFERENCES);
    }

    if (isLoggingDebug()) {
      Iterator<String> i = refs.iterator();
      while (i.hasNext()) {
        String s = i.next();
        logDebug("Found reference: " + s);
      }
    }

    return refs;
  }

  /**
   * This method is used to determine whether all the items in the second Vector
   * are contained in the first Vector.
   * 
   * @param Vector
   *          of the names of all the tables in the repository
   * @param Vector
   *          of the names of all the tables to check for
   * @return boolean true if all items are in the Vector; false otherwise
   */
  private boolean checkReferencesInRepository(List<String> pRepositoryTables,
      List<String> pCheckTables) {
    Iterator<String> iter = pCheckTables.iterator();
    while (iter.hasNext()) {
      String name = iter.next();
      if (!pRepositoryTables.contains(name))
        return false;
    }
    return true;
  }

  /**
   * Method to determine whether a table already exists in the database. The
   * method operates by appending the name passed as a parameter to the String
   * that has been set in the determineTableExistsSQL property
   * 
   * @param String
   *          - name of table to check for existence of
   * @return boolean - true if table exists; false otherwise
   */
  private boolean tableExists(String pTableName) {
    // don't bother with query if name is invalid
    if (pTableName == null || pTableName.length() == 0)
      return false;

    // create sql
    String sql = getDetermineTableExistsSQL() + " " + pTableName;

    // execute and check for an exception
    try {

      performSQL(sql);

    } catch (SQLProcessorException spe) {
      // we should only get an exception here if the table does NOT
      // exist. in that case, don't throw the exception - just return false
      if (isLoggingDebug()) {
        logDebug("Table existence is determined by whether an exception is received when querying the table.");
        logDebug("Caught exception checking whether table exists, so table doesn't exist.");
        logDebug("Checked for existence with this statement \"" + sql + "\"");
        logDebug("Set repository debugLevel > 6 to see full exception.");
        if (this.getRepository().getDebugLevel() > 6)
          logDebug(spe);
      }

      return false;
    }

    return true;
  }

  /**
   * Returns true if there is at least one table in this schema
   * This is handy for Derby since it will throw an error if one
   * attempts to try a select statement to determine if a table
   * exists and the schema has not yet been created.
   * @return
   */
  private boolean hasAnyTables() {
    boolean foundTables = false;
    Connection c = null;
    try {
      c = getConnection();
      DatabaseMetaData metadata = null;
      metadata = c.getMetaData();
      String[] names = { "TABLE" };
      ResultSet tableNames = metadata.getTables(null, null, null, names);
      while (tableNames.next()) {
        String tab = tableNames.getString("TABLE_NAME");
        foundTables = true;
        break;
      }
      tableNames.close();
    } catch (SQLProcessorException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        c.close();
      } catch (SQLException e) {
        ;
      }
    }
    return foundTables;
  }

  /* These methods are really used - they were just part of mgk's original class */

  // ---------- Property: (read-only) InsertSQL ----------
  /** SQL to insert a new id space into the DB table */
  transient String mInsertSQL;

  /**
   * Get property <code>InsertSQL</code>. The SQL is lazily generated.
   * 
   * @beaninfo description: SQL to insert a new id space into the DB table
   * @return InsertSQL
   **/
// private String getInsertSQL()
// {
// // build SQL string if needed
// if (mInsertSQL == null)
// {
// StringBuffer buf = new StringBuffer(300);
// buf.append("INSERT INTO ");
// /*
// buf.append(getTableName());
// buf.append('(');
// buf.append(getNameColumn()).append(',');
// buf.append(getSeedColumn()).append(',');
// buf.append(getBatchSizeColumn()).append(',');
// buf.append(getPrefixColumn()).append(',');
// buf.append(getSuffixColumn());
// */
//
// buf.append(')').append('\n');
// buf.append("VALUES (?, ?, ?, ?, ?)\n");
//
// mInsertSQL = buf.toString();
// }
//
// return mInsertSQL;
// }

  // ---------- Property: (read-only) UpdateSQL ----------
  /** SQL to execute to update a specific id space in the DB */
  transient String mUpdateSQL;

  /**
   * Get property <code>UpdateSQL</code>. The SQL is lazily generated.
   * 
   * @beaninfo description: SQL to execute to update a specific id space int the
   *           D0B
   * @return UpdateSQL
   **/
// private String getUpdateSQL()
// {
// // generate SQL if needed
// if (mUpdateSQL == null)
// {
// StringBuffer buf = new StringBuffer(300);
// buf.append("UPDATE ");
// /*
// buf.append(getTableName());
// buf.append(" SET ");
// buf.append(getSeedColumn()).append('=');
// buf.append(getSeedColumn()).append('+');
// buf.append(getBatchSizeColumn());
// buf.append(" WHERE ");
// buf.append(getNameColumn()).append(" = ?");
//
// */
// mUpdateSQL = buf.toString();
// }
//
// return mUpdateSQL;
// }

  // ---------- Property: (read-only) SelectSQL ----------
  /** SQL to execute to load a specific id space from the DB */
  transient String mSelectSQL;

  /**
   * Get property <code>SelectSQL</code>. The SQL is lazily generated.
   * 
   * @beaninfo description: SQL to execute to load a specific id space from the
   *           DB
   * @return SelectSQL
   **/
// private String getSelectSQL()
// {
// // generate SQL if needed
// if (mSelectSQL == null)
// {
// StringBuffer buf = new StringBuffer(300);
// buf.append("SELECT ");
// /*
// buf.append(getSeedColumn()).append(',');
// buf.append(getBatchSizeColumn()).append(',');
// buf.append(getPrefixColumn()).append(',');
// buf.append(getSuffixColumn());
// buf.append("  FROM ");
// buf.append(getTableName());
// buf.append(" WHERE ");
// buf.append(getNameColumn()).append(" = ?");
// */
//
// mSelectSQL = buf.toString();
// }
//
// return mSelectSQL;
// }

}
