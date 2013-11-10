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
public class AccessibleTableColumns {

    Logger log = LogManager.getLogger();

    public TableColumns mTableColumns;

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
        ColumnDefinitionNode node = (ColumnDefinitionNode) getPrivateField(fieldName);
        return node;
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
        ColumnDefinitionNode node = (ColumnDefinitionNode) getPrivateField(fieldName);
        return node;
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
        List node = (List) getPrivateField(fieldName);
        return node;
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
        List node = (List) getPrivateField(fieldName);
        return node;
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
        String node = (String) getPrivateField(fieldName);
        return node;
    }

    // ------------------------
    public Object getPrivateField(String fieldName) {
        Field columnDefinitionNode = null;
        Object field = null;
        try {
            columnDefinitionNode = mTableColumns.getClass().getDeclaredField(
                    fieldName
            );
            columnDefinitionNode.setAccessible(true);
            field = columnDefinitionNode.get(mTableColumns);
        } catch ( SecurityException e ) {
            log.catching(e);
        } catch ( NoSuchFieldException e ) {
            log.catching(e);
        } catch ( IllegalArgumentException e ) {
            log.catching(e);
        } catch ( IllegalAccessException e ) {
            log.catching(e);
        }
        return field;
    }

}
