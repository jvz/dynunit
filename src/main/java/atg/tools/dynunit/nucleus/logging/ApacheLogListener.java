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
import atg.nucleus.logging.ExternalLogSystemLogListener;
import atg.nucleus.logging.InfoLogEvent;
import atg.nucleus.logging.LogEvent;
import atg.nucleus.logging.TraceLogEvent;
import atg.nucleus.logging.WarningLogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple LogListener that translates log events to Log4j2.
 *
 * @author msicker
 * @version 1.0.0
 */
public class ApacheLogListener
        extends ExternalLogSystemLogListener {

    @Override
    public void logEvent(final LogEvent logEvent) {
        if (logEvent == null) {
            return;
        }
        final Logger logger = getLoggerForEvent(logEvent);
        final Level level = getLoggingLevelForEvent(logEvent);
        final String message = logEvent.getMessage();
        final Throwable exception = logEvent.getThrowable();
        logger.log(level, message, exception);
    }

    private Logger getLoggerForEvent(final LogEvent logEvent) {
        final String originator = getPseudoClassNameForNucleusPath(logEvent.getOriginator());
        return LogManager.getLogger(originator);
    }

    private Level getLoggingLevelForEvent(final LogEvent logEvent) {
        if (logEvent instanceof TraceLogEvent) {
            return isUseInfoForDebug() ? Level.INFO : Level.TRACE;
        }
        if (logEvent instanceof DebugLogEvent) {
            return isUseInfoForDebug() ? Level.INFO : Level.DEBUG;
        }
        if (logEvent instanceof InfoLogEvent) {
            return Level.INFO;
        }
        if (logEvent instanceof WarningLogEvent) {
            return Level.WARN;
        }
        if (logEvent instanceof ErrorLogEvent) {
            return Level.ERROR;
        }
        return Level.INFO;
    }
}
