package atg.tools.dynunit.nucleus.logging;

import atg.nucleus.logging.DebugLogEvent;
import atg.nucleus.logging.ErrorLogEvent;
import atg.nucleus.logging.InfoLogEvent;
import atg.nucleus.logging.LogEvent;
import atg.nucleus.logging.LogListener;
import atg.nucleus.logging.TraceLogEvent;
import atg.nucleus.logging.WarningLogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author msicker
 * @version 1.0.0
 */
public class ApacheLogListener
        implements LogListener {

    @Override
    public void logEvent(final LogEvent logEvent) {
        if (logEvent == null) {
            return;
        }
        log(
                getLoggerForEvent(logEvent),
                getLoggingLevelForEvent(logEvent),
                getLogMessageForEvent(logEvent),
                getExceptionForEvent(logEvent)
        );
    }

    private static Logger getLoggerForEvent(final LogEvent logEvent) {
        final String originator = logEvent.getOriginator();
        return originator == null ? LogManager.getRootLogger() : LogManager.getLogger(originator);
    }

    private static Level getLoggingLevelForEvent(final LogEvent logEvent) {
        if (logEvent instanceof TraceLogEvent) {
            return Level.TRACE;
        }
        if (logEvent instanceof DebugLogEvent) {
            return Level.DEBUG;
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
        return Level.FATAL;
    }

    private static String getLogMessageForEvent(final LogEvent logEvent) {
        return logEvent.getMessage();
    }

    private static Throwable getExceptionForEvent(final LogEvent logEvent) {
        return logEvent.getThrowable();
    }

    private static void log(final Logger logger,
                            final Level logLevel,
                            final String logMessage,
                            final Throwable exception) {
        if (exception == null) {
            logger.log(logLevel, logMessage);
        }
        else {
            logger.log(logLevel, logMessage, exception);
        }
    }
}
