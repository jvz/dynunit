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

      String level = "unknown";

      if (logEvent instanceof DebugLogEvent) {
        level = "debug";
      }
      else if (logEvent instanceof ErrorLogEvent) {
        level = "error";
      }
      else if (logEvent instanceof InfoLogEvent) {
        level = "info";
      }
      else if (logEvent instanceof WarningLogEvent) {
        level = "warning";
      }

      out.println("**** " + level + "\t" + logEvent.getDateTimeStamp()
          + "\t" + logEvent.getTimeStamp() + "   \t" + logEvent.getOriginator()
          + "." + logEvent.getMessage());

      if (logEvent.getThrowable() != null) {
        logEvent.getThrowable().printStackTrace();
      }

    }
  }
}