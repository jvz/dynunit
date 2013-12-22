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

package atg.tools.dynunit.adapter.gsa;

import atg.adapter.gsa.GSARepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SchemaTracker {

    private SchemaTracker() {}

    private static interface Holder {
        static final SchemaTracker instance = new SchemaTracker();
    }

    public static SchemaTracker getInstance() {
        return Holder.instance;
    }

    private final Map<String, List<GSARepository>> tableRepositoryCache = new ConcurrentHashMap<String, List<GSARepository>>();

    public List<GSARepository> getTable(final String table) {
        return tableRepositoryCache.get(table);
    }

    public void putTable(final String table, final List<GSARepository> repositories) {
        tableRepositoryCache.put(table, repositories);
    }

    /**
     * Resets the state in this class.
     */
    public void reset() {
        tableRepositoryCache.clear();
    }
}
