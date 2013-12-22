package atg.tools.dynunit.nucleus.logging;

import atg.nucleus.InsertableGenericContext;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;

/**
 * ClassLoggingFactory implementation that uses custom ApacheLogging instances. This class should be used for the
 * component {@code /atg/dynamo/service/logging/ClassLoggingFactory}. Either way, once this class is initialized, it
 * will set the default ClassLoggingFactory to an instance of this class.
 *
 * @author msicker
 * @version 1.0.0
 */
public class ApacheClassLoggingFactory
        extends InsertableGenericContext
        implements ClassLoggingFactory.Factory {

    static {
        ClassLoggingFactory.setFactory(new ApacheClassLoggingFactory());
    }

    @Override
    public ApplicationLogging getLoggerForClass(final Class datClass) {
        return new ApacheLogging(datClass);
    }

    @Override
    public ApplicationLogging getLoggerForClassName(final String className) {
        return new ApacheLogging(className);
    }
}
