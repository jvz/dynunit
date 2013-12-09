package atg.tools.dynunit;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.tools.dynunit.inject.NucleusInjectorFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * @author msicker
 * @version 1.0.0
 */
public class DynUnit {

    private static final Logger logger = LogManager.getLogger();

    public static void init(final Object testInstance) {
        logger.entry(testInstance);
        NucleusInjectorFactory.getInjector().init(testInstance);
        logger.exit();
    }

    public static void stop(final Object testInstance) {
        logger.entry(testInstance);
        boolean foundNucleus = false;
        final Field[] fields = testInstance.getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(Nuke.class)) {
                foundNucleus = true;
                try {
                    final Nucleus nucleus = (Nucleus) FieldUtils.readField(field, testInstance, true);
                    logger.info("Found Nucleus: {}.", nucleus.getAbsoluteName());
                    if (nucleus.isRunning()) {
                        logger.info("Stopping Nucleus.");
                        nucleus.stopService();
                    }
                } catch (IllegalAccessException e) {
                    logger.catching(e);
                    logger.error("Can't access test instance's Nucleus. Strange.");
                } catch (ServiceException e) {
                    logger.catching(e);
                    logger.warn("Problem stopping Nucleus.");
                }
            }
        }
        if (!foundNucleus) {
            logger.error("Couldn't find nucleus field to stop!");
        }
        logger.exit();
    }

}
