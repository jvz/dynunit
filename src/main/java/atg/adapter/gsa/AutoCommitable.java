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

import java.sql.Connection;

/**
 * Interface for code that should be executed with autoCommit=true.
 *
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/AutoCommitable.java#1 $
 */
public interface AutoCommitable {

    /**
     * Work to be done withing the scope of an autoCommit transaction
     * should be placed into the implementation of this method.
     * Hand of the implementation (typically via Anonymous Inner Class) to an
     * instance of <code>DoInAutoCommitLand.doInAutoCommit</code>
     *
     * @param pConnection
     */
    public void doInAutoCommit(Connection pConnection);
}
