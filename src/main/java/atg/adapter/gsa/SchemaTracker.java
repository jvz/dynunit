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

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class SchemaTracker {

    private HashMap<String, List<GSARepository>> mTableToRepository = new HashMap<String, List<GSARepository>>();

    /**
     * @return the tableToRepository
     */
    public HashMap<String, List<GSARepository>> getTableToRepository() {
        return mTableToRepository;
    }

    /**
     * @param pTableToRepository the tableToRepository to set
     */
    public void setTableToRepository(HashMap<String, List<GSARepository>> pTableToRepository) {
        mTableToRepository = pTableToRepository;
    }

    @Nullable
    private static SchemaTracker sSchemaTracker = null;

    private SchemaTracker() {
    }

    public static SchemaTracker getSchemaTracker() {
        if ( sSchemaTracker == null ) {
            sSchemaTracker = new SchemaTracker();
        }
        return sSchemaTracker;
    }

    /**
     * Resets the state in this class.
     */
    public void reset() {
        mTableToRepository.clear();
    }
}
