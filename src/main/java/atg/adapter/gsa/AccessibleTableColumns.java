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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

/**
 * This class uses reflection to allow access to private member variables
 * withing a GSA Table class.
 *
 * @author adamb
 */
class AccessibleTableColumns {

    private static final Logger logger = LogManager.getLogger();

    private final TableColumns mTableColumns;

    public AccessibleTableColumns(TableColumns pTable) {
        mTableColumns = pTable;
    }

    // ------------------------

    /**
     * Returns the mHead field of the Table class passed to the constructor of
     * this class.
     *
     * @return
     */
    public ColumnDefinitionNode getHead() {
        String fieldName = "mHead";
        return (ColumnDefinitionNode) getPrivateField(fieldName);
    }

    // ------------------------

    /**
     * Returns the mTail field of the Table class passed to the constructor of
     * this class.
     *
     * @return
     */
    public ColumnDefinitionNode getTail() {
        String fieldName = "mTail";
        return  (ColumnDefinitionNode) getPrivateField(fieldName);
    }

    // ------------------------

    /**
     * Returns the mPrimaryKeys field of the Table class passed to the constructor of
     * this class.
     *
     * @return
     */
    public List getPrimaryKeys() {
        String fieldName = "mPrimaryKeys";
        return  (List) getPrivateField(fieldName);
    }

    //------------------------

    /**
     * Returns the mForeignKeys field of the Table class passed to the constructor of
     * this class.
     *
     * @return
     */
    public List getForeignKeys() {
        String fieldName = "mForeignKeys";
        return  (List) getPrivateField(fieldName);
    }

    // ------------------------

    /**
     * Returns the mMultiColumnName field of the Table class passed to the
     * constructor of this class.
     *
     * @return
     */
    public String getMultiColumnName() {
        String fieldName = "mMultiColumnName";
        return  (String) getPrivateField(fieldName);
    }

    // ------------------------
    Object getPrivateField(String fieldName) {
        Field columnDefinitionNode = null;
        Object field = null;
        try {
            columnDefinitionNode = mTableColumns.getClass().getDeclaredField(
                    fieldName
            );
            columnDefinitionNode.setAccessible(true);
            field = columnDefinitionNode.get(mTableColumns);
        } catch ( SecurityException e ) {
            logger.catching(e);
        } catch ( NoSuchFieldException e ) {
            logger.catching(e);
        } catch ( IllegalArgumentException e ) {
            logger.catching(e);
        } catch ( IllegalAccessException e ) {
            logger.catching(e);
        }
        return field;
    }

}
