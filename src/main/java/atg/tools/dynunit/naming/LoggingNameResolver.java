package atg.tools.dynunit.naming;

import atg.core.exception.PropertyNotSetException;
import atg.naming.NameResolver;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLoggingSender;
import atg.tools.dynunit.nucleus.logging.ApacheLogListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author msicker
 * @version 1.0.0
 */
public class LoggingNameResolver implements NameResolver {

    private static final Logger logger = LogManager.getLogger();

    private Nucleus nucleus;
    private boolean debug;

    public LoggingNameResolver() {}

    public LoggingNameResolver(final Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    public void setNucleus(final Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    @Override
    public Object resolveName(final String name) {
        logger.entry(name);
        if (nucleus == null) {
            throw new PropertyNotSetException("nucleus");
        }
        return enableLoggingOnGenericService(nucleus.resolveName(name));
    }

    private Object enableLoggingOnGenericService(final Object component) {
        if (component instanceof ApplicationLoggingSender) {
            ApplicationLoggingSender loggingSender = (ApplicationLoggingSender) component;
            loggingSender.setLoggingDebug(debug);
            loggingSender.setLoggingInfo(true);
            loggingSender.setLoggingWarning(true);
            loggingSender.setLoggingError(true);
            loggingSender.addLogListener(new ApacheLogListener());
        }
        return component;
    }
}
