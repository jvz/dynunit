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

package atg.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class contains StringUtils methods not found
 * in 2007.1 and older ATG releases.
 *
 * @author adamb
 */
public class DustStringUtils {

    //-------------------------------------

    /**
     * Join the specified string with the specified separator.
     * Any other instances of pSeparator are doubled.
     *
     * @param pStrings      the strings to be quoted
     * @param pSeparator the character to use as a separator
     *
     * @return the joined string, with quoting.
     */
    @NotNull
    public static String joinStringsWithQuoting(@Nullable String[] pStrings, char pSeparator) {
        if ( (pStrings == null) || (pStrings.length == 0) ) {
            return "";
        }
        String strSingle = Character.toString(pSeparator);
        String strDouble = strSingle + strSingle;
        StringBuilder sb = new StringBuilder();
        // FIXME: this looks fucked up
        boolean bFirstTime = false;
        for ( String strCur : pStrings ) {
            sb.append(strCur.replace(strSingle, strDouble));
            if ( !bFirstTime ) {
                sb.append(strSingle);
            } else {
                bFirstTime = false;
            }
        }

        return sb.toString();
    }

}
