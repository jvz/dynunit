/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.adapter.gsa;

import atg.repository.RepositoryException;
import org.apache.ddlutils.DatabaseOperationException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.ForeignKey;
import org.apache.ddlutils.model.IndexColumn;
import org.apache.ddlutils.model.Reference;
import org.apache.ddlutils.model.UniqueIndex;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to generate drop and alter a database schema required for
 * a given repository. It uses the Apache DDLUtils tools for the actual schema
 * manipulation. To use the class first initialize it's "model" by passing a
 * GSARepository to the constructor. Afterwards you may invoke the action
 * methods such as:
 * <ul>
 * <li>createSchema - Creates schema including constraints.
 * <li>dropSchema - Drops the schema including constraints.
 * <li>alterSchema - Attempts to alter an existing schema into the one currently
 * required for the given repository.
 * </ul>
 * These methods affect the DataSource used by the given GSARepository. If that
 * DataSource is not accessible then these methods will fail. Schema
 * modification may continue or fail on error. Set the <code>strict</code>
 * property to true to always fail on error. The default is to continue on
 * error.
 *
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/
 *          GSARepositorySchemaGenerator.java#1 $
 */
public class GSARepositorySchemaGenerator {

    // The repository upon which we are working.
    private GSARepository mRepository = null;

    // The DDLUtils Platform object
    private Platform mPlatform = null;

    // The DDLUtils Database Model
    private Database mDatabase = null;

    // Tool for mapping database types
    private DatabaseTypeNameToJDBC mDatabaseTypeNameToJDBC = null;

    static Set mUsedFKNames = new HashSet();

    // -----------------------------

    /**
     * Creates a new GSARepositorySchemaGenerator and initializes it with a model
     * based upon the given repository.
     *
     * @param pRepository
     * @param pIncludeExistingTables If true, the model will include the existing database tables
     *                               as
     *                               well as
     *                               tables from the current repository.
     *
     * @throws RepositoryException
     */
    public GSARepositorySchemaGenerator(GSARepository pRepository, boolean pIncludeExistingTables)
            throws RepositoryException {
        buildModel(pRepository, pIncludeExistingTables);
    }

    // -----------------------------

    /**
     * Creates a new GSARepositorySchemaGenerator and initializes it with a model
     * based upon the given repository.
     *
     * @param pRepository
     *
     * @throws RepositoryException
     */
    public GSARepositorySchemaGenerator(GSARepository pRepository)
            throws RepositoryException {
        buildModel(pRepository, false);
    }

    // -----------------------------

    /**
     * Initialize this class with a model for the given repository. Any previous
     * model will be discarded.
     *
     * @param pRepository
     *
     * @throws RepositoryException
     */
    public void buildModel(GSARepository pRepository)
            throws RepositoryException {
        buildModel(pRepository, false);
    }

    // -----------------------------

    /**
     * Initialize this class with a model for the given repository. Any previous
     * model will be discarded.
     *
     * @param pRepository
     * @param pIncludeExistingTables If true the existing tables in the database will
     *                               be added to the model.
     *
     * @throws RepositoryException
     */
    public void buildModel(GSARepository pRepository, boolean pIncludeExistingTables)
            throws RepositoryException {
        mDatabaseTypeNameToJDBC = new DatabaseTypeNameToJDBC(
                pRepository.getDatabaseTableInfo()
        );
        mRepository = pRepository;
        mPlatform = PlatformFactory.createNewPlatformInstance(
                pRepository.getDataSource()
        );
        if ( pIncludeExistingTables ) {
            mDatabase = mPlatform.readModelFromDatabase(pRepository.getAbsoluteName());
        } else {
            mDatabase = new Database();
            mDatabase.setName(pRepository.getAbsoluteName());
            mDatabase.setVersion("1.0");
        }
        String[] names = pRepository.getItemDescriptorNames();
        for ( String name : names ) {
            GSAItemDescriptor desc = (GSAItemDescriptor) pRepository.getItemDescriptor(name);
            Table[] tables = desc.getTables();
            // first do primary tables
            processTables(pRepository, tables, true);
        }
        for ( String name : names ) {
            GSAItemDescriptor desc = (GSAItemDescriptor) pRepository.getItemDescriptor(name);
            Table[] tables = desc.getTables();
            // then do the rest
            desc.getPrimaryTable();
            processTables(pRepository, tables, false);
        }

    }

    /**
     * Walks the tables of this repository building up a DDLUtils model.
     *
     * @param pRepository
     * @param tables
     * @param pPrimary    - if True only processes primary tables
     *
     * @throws RepositoryException
     */
    private void processTables(GSARepository pRepository, Table[] tables, boolean pPrimary)
            throws RepositoryException {
        for ( Table table : tables ) {
            if ( !table.isInherited() && (table.isPrimaryTable() == pPrimary) ) {
                // track tables here. if we have multiple repositories
                // using the same table we don't want to double create.
                // actually the problem is more a single
                // repository that is reusing a table for multiple
                // purposes

                List<GSARepository> repositoriesUsingTable = SchemaTracker.getSchemaTracker()
                                                                          .getTableToRepository()
                                                                          .get(table.getName());
                // Skip if we have added this table
                // and it is already in the model
                // Actually, checking the model
                // is probably enough

                if ( repositoriesUsingTable != null && (mDatabase.findTable(table.getName())
                                                        != null) ) {
                    if ( pRepository.isLoggingDebug() ) {
                        pRepository.logDebug(
                                "Table "
                                + table.getName()
                                + " already defined by repository "
                                + repositoriesUsingTable.toString()
                                + " skipping schema creation for this table. multi="
                                + table.isMultiTable()
                                + " auxiliary="
                                + table.isAuxiliaryTable()
                                + " primary="
                                + table.isPrimaryTable()
                        );
                    }
                    if ( !repositoriesUsingTable.contains(pRepository) ) {
                        repositoriesUsingTable.add(pRepository);
                    }
                } else {
                    // Only add the model if we have never seen this table created
                    buildSingleTableModel(mDatabase, table, pRepository);
                    repositoriesUsingTable = new ArrayList<GSARepository>();
                    repositoriesUsingTable.add(pRepository);
                }
                SchemaTracker.getSchemaTracker().getTableToRepository().put(
                        table.getName(), repositoriesUsingTable
                );
            }
        }
    }

    // -----------------------------

    /**
     * Adds the definition of the given table to the current DDLUtils database
     * model.
     *
     * @param pDb
     * @param pTable
     * @param pRepository
     *
     * @throws RepositoryException
     */
    void buildSingleTableModel(Database pDb, Table pTable, GSARepository pRepository)
            throws RepositoryException {
        // XXX: holy shitwaffles simplify this shit
        TableColumns columns = new TableColumns(
                pTable, pRepository.getDatabaseTableInfo()
        );
        pTable.collectColumnsForName(columns);
        AccessibleTableColumns atable = new AccessibleTableColumns(columns);

        // --------------------------
        // Table Definition
        // --------------------------
        org.apache.ddlutils.model.Table t = new org.apache.ddlutils.model.Table();
        t.setName(pTable.getName());
        pDb.addTable(t);

        // --------------------------
        // Add Columns
        // --------------------------

        ColumnDefinitionNode columnDefinition = null;
        boolean proceed = false;

        for ( columnDefinition = atable.getHead(), proceed = true;
              columnDefinition != null && proceed;
              columnDefinition = columnDefinition.mNext ) {
            // No need to iterate the next time if there is just one element in the
            // linked list
            if ( atable.getHead() == atable.getTail() ) {
                proceed = false;
            }

            Column c = new Column();

            // --------------------------
            // Column Name
            // --------------------------

            c.setName(columnDefinition.mColumnName);
            t.addColumn(c);

            // --------------------------
            // Column Type
            // --------------------------

            setupColumnType(pRepository, columnDefinition, c);

            // --------------------------
            // Primary Key
            // --------------------------

            if ( atable.getPrimaryKeys().contains(c.getName()) || c.getName()
                                                                   .equals(atable.getMultiColumnName()) ) {
                c.setPrimaryKey(true);
            }

            // --------------------------
            // Null/NotNull
            // --------------------------

            if ( columnDefinition.mIsRequired || atable.getPrimaryKeys()
                                                       .contains(columnDefinition.mColumnName) ) {
                c.setRequired(true);
            } else {
                c.setRequired(false);
            }

            // --------------------------
            // Unique Index
            // DDLUtils doesn't yet to UNIQUE constraints.. Hmph
            // --------------------------

            if ( columnDefinition.mIsUnique ) {
                UniqueIndex uniqueIndex = new UniqueIndex();
                uniqueIndex.setName("uidx_" + t.getName() + "_" + c.getName());
                uniqueIndex.addColumn(new IndexColumn(c));
                t.addIndex(uniqueIndex);
            }

            // --------------------------
            // References Constraint
            // --------------------------

            if ( columnDefinition.mReferenced != null && !columns.mVersioned ) {

                ForeignKey foreignKey = new ForeignKey();
                Reference reference = new Reference();
                String referencedTableName = columnDefinition.mReferenced.substring(
                        0, columnDefinition.mReferenced.indexOf("(")
                );
                String referencedColumnName = columnDefinition.mReferenced.substring(
                        columnDefinition.mReferenced.indexOf("(") + 1,
                        columnDefinition.mReferenced.indexOf(")")
                );
                org.apache.ddlutils.model.Table referencedTable = pDb.findTable(referencedTableName);
                String fkName = (t.getName()
                                 + c.getName()
                                 + "FK"
                                 + referencedTableName
                                 + referencedColumnName);


                foreignKey.setName(fkName);

                if ( referencedTable != null ) {
                    Column referencedColumn = referencedTable.findColumn(referencedColumnName);
                    if ( referencedTable.getName().equals(t.getName())
                         && pRepository.isLoggingDebug()
                         && referencedColumn.getName().equals(c.getName()) ) {
                        if ( pRepository.isLoggingDebug() ) {
                            pRepository.logDebug(
                                    "Skipping foreign key constraint, table and column are the same. Table.Column="
                                    + referencedTableName
                                    + "."
                                    + referencedColumnName
                            );
                        }
                    } else {
                        reference.setForeignColumn(referencedColumn);
                        reference.setLocalColumn(c);
                        foreignKey.addReference(reference);
                        foreignKey.setForeignTable(referencedTable);
                        // try to find existing fk
                        ForeignKey existingKey = t.findForeignKey(foreignKey);
                        // don't add this fk if the name is already used
                        if ( existingKey == null ) {
                            t.addForeignKey(foreignKey);
                        }
                    }
                } else {
                    if ( pRepository.isLoggingDebug() ) {
                        pRepository.logDebug(
                                "skipping adding fk, referenced table is null" + fkName
                        );
                    }
                }

                // --------------------------
                // Foreign Keys
                // --------------------------

                if ( atable.getForeignKeys() != null && !columns.mVersioned ) {
                    // TODO: Add ForeignKeys
                }
            }
        }

    }

    /**
     * Determines the appropriate jdbc type for the given ColumnDefinitionNode and
     * sets that in Column "c".
     *
     * @param pRepository
     * @param columnDefinition
     * @param c
     */
    void setupColumnType(GSARepository pRepository,
                         ColumnDefinitionNode columnDefinition,
                         Column c) {
        c.setDescription(columnDefinition.mDataTypeString);
        String typeName = null;
        String size = null;
        if ( columnDefinition.mDataTypeString.contains("(") ) {
            typeName = columnDefinition.mDataTypeString.substring(
                    0, columnDefinition.mDataTypeString.indexOf("(")
            );
            size = columnDefinition.mDataTypeString.substring(
                    columnDefinition.mDataTypeString.indexOf("(") + 1,
                    columnDefinition.mDataTypeString.indexOf(")")
            );
        } else {
            typeName = columnDefinition.mDataTypeString;
        }

        String precision = null;
        String scale = null;
        if ( size != null ) {
            if ( size.contains(",") ) {
                precision = size.substring(0, size.indexOf(","));
                scale = size.substring(size.indexOf(",") + 1, size.length());
                c.setPrecisionRadix(Integer.parseInt(precision.trim()));
                c.setScale(Integer.parseInt(scale.trim()));
            } else {
                c.setSize(size);
            }
        }

        c.setTypeCode(mDatabaseTypeNameToJDBC.databaseTypeNametoJDBCType(typeName));
    }

    // -----------------------------

    /**
     * Creates the schema based on the current model. If no model has been
     * created, this method throws a NoModelException.
     *
     * @param pContinueOnError - If true, continue on error, else fail.
     * @param pDrop            - If true, drops schema first before attempting to create it.
     *
     * @throws SQLException
     * @throws DatabaseOperationException
     */
    public void createSchema(final boolean pContinueOnError, final boolean pDrop)
            throws DatabaseOperationException {
        boolean success = new DoInAutoCommit(this, mRepository).doInAutoCommit(
                new AutoCommitable() {
                    @Override
                    public void doInAutoCommit(Connection pConnection) {
                        mPlatform.createTables(
                                pConnection, mDatabase, pDrop, pContinueOnError
                        );
                    }
                }
        );
        if ( !success ) {
            throw new DatabaseOperationException("Failed to create tables.");
        }
    }

    // -----------------------------

    /**
     * Drops the schema based on the current model. If no model has been created,
     * this method throws a NoModelException.
     *
     * @param pContinueOnError - If true, continue on error, else fail.
     *
     * @throws SQLException
     * @throws DatabaseOperationException
     */
    public void dropSchema(final boolean pContinueOnError)
            throws DatabaseOperationException {
        boolean success = new DoInAutoCommit(this, mRepository).doInAutoCommit(
                new AutoCommitable() {
                    @Override
                    public void doInAutoCommit(Connection pConnection) {
                        mPlatform.dropTables(pConnection, mDatabase, pContinueOnError);
                    }
                }
        );
        if ( !success ) {
            throw new DatabaseOperationException("Failed to drop tables.");
        }
    }

    // -----------------------------

    /**
     * Alters the schema based on the current model. If no model has been created,
     * this method throws a NoModelException. This method attempts to preserve the
     * data in the target database.
     *
     * @param pContinueOnError - If true, fail on error, else continue on error.
     *
     * @throws SQLException
     * @throws DatabaseOperationException
     */
    public void alterSchema(final boolean pContinueOnError)
            throws DatabaseOperationException, SQLException {
        mPlatform.alterTables(
                mRepository.getConnection(), mDatabase, pContinueOnError
        );
        boolean success = new DoInAutoCommit(this, mRepository).doInAutoCommit(
                new AutoCommitable() {
                    @Override
                    public void doInAutoCommit(Connection pConnection) {
                        mPlatform.alterTables(pConnection, mDatabase, pContinueOnError);
                    }
                }
        );
        if ( !success ) {
            throw new DatabaseOperationException("Failed to alter tables.");
        }
    }
}
