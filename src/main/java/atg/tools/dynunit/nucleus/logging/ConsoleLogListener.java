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

package atg.tools.dynunit.nucleus.logging;

import atg.nucleus.logging.DebugLogEvent;
import atg.nucleus.logging.ErrorLogEvent;
import atg.nucleus.logging.InfoLogEvent;
import atg.nucleus.logging.LogEvent;
import atg.nucleus.logging.LogListener;
import atg.nucleus.logging.WarningLogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Simple class to get logging in unit testing working. Will print messages and
 * {@link Throwable}'s to {@link System#out}.
 *
 * @author robert
 * @author jvz
 */
public class ConsoleLogListener
        implements LogListener {

    @Override
    public void logEvent(@Nullable final LogEvent logEvent) {
        if ( logEvent != null ) {
            final Logger logger;
            final Level level;
            final String originator = logEvent.getOriginator();
            final Throwable ex = logEvent.getThrowable();

            if ( originator != null ) {
                logger = LogManager.getLogger(originator);
            } else {
                logger = LogManager.getLogger();
            }

            if ( logEvent instanceof DebugLogEvent) {
                level = Level.DEBUG;
            } else if ( logEvent instanceof InfoLogEvent) {
                level = Level.INFO;
            } else if ( logEvent instanceof WarningLogEvent) {
                level = Level.WARN;
            } else if ( logEvent instanceof ErrorLogEvent) {
                level = Level.ERROR;
            } else {
                level = Level.TRACE; // or would fatal be better here?
            }

            if ( ex != null ) {
                logger.log(level, logEvent.getMessage(), ex);
            } else {
                logger.log(level, logEvent.getMessage());
            }

        }
    }
}