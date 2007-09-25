package atg.nucleus.logging;

import static java.lang.System.out;

/**
 * Simple class to get logging in unit testing working. Will print messages and
 * {@link Throwable}'s to {@link System#out}.
 * 
 * @author robert
 * 
 */
public class ConsoleLogListener implements LogListener {

  /**
   * 
   */
  public void logEvent(final LogEvent logEvent) {
    if (logEvent != null) {

      String logLevel = "unknown";

      if (logEvent instanceof DebugLogEvent) {
        logLevel = "debug";
      }
      else if (logEvent instanceof ErrorLogEvent) {
        logLevel = "error";
      }
      else if (logEvent instanceof InfoLogEvent) {
        logLevel = "info";
      }
      else if (logEvent instanceof WarningLogEvent) {
        logLevel = "warning";
      }

      out.println("**** " + logLevel + "\t" + logEvent.getDateTimeStamp()
          + "\t" + logEvent.getTimeStamp() + "   " + logEvent.getOriginator()
          + "." + logEvent.getMessage());

      if (logEvent.getThrowable() != null) {
        logEvent.getThrowable().printStackTrace();
      }

    }
  }
}