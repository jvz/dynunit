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

package atg.nucleus.logging;

import static java.lang.System.out;

/**
 * Simple class to get logging in unit testing working. Will print messages and
 * {@link Throwable}'s to {@link System#out}.
 *
 * @author robert
 */
public class ConsoleLogListener
        implements LogListener {

    /**
     *
     */
    public void logEvent(final LogEvent logEvent) {
        if ( logEvent != null ) {

            String level = "unknown";

            if ( logEvent instanceof DebugLogEvent ) {
                level = "debug";
            } else if ( logEvent instanceof ErrorLogEvent ) {
                level = "error";
            } else if ( logEvent instanceof InfoLogEvent ) {
                level = "info";
            } else if ( logEvent instanceof WarningLogEvent ) {
                level = "warning";
            }

            out.println(
                    String.format(
                            "**** %s\t%s\t%s\t%s\t%s",
                            level,
                            logEvent.getDateTimeStamp(),
                            logEvent.getTimeStamp(),
                            logEvent.getOriginator(),
                            logEvent.getMessage()
                    )
            );

            if ( logEvent.getThrowable() != null ) {
                logEvent.getThrowable().printStackTrace();
            }

        }
    }
}