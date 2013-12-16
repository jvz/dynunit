package atg.tools.dynunit;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.tools.dynunit.inject.NucleusInjectorFactory;
import atg.tools.dynunit.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * @author msicker
 * @version 1.0.0
 */
public final class DynUnit {

    public static final String DYNUNIT_HOME_PROPERTY = "atg.tools.dynunit.home";

    private DynUnit() {
    }

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

    public static String getHome() {
        logger.entry();
        String dynUnitHome = PropertiesUtil.getSystemProperty(DYNUNIT_HOME_PROPERTY);
        if (StringUtils.isEmpty(dynUnitHome)) {
            dynUnitHome = System.getenv("DYNUNIT_HOME");
            if (StringUtils.isEmpty(dynUnitHome)) {
                logger.error("Can't find DynUnit home directory. Please set the {} property.", DYNUNIT_HOME_PROPERTY);
            }
        }
        return logger.exit(StringUtils.defaultString(dynUnitHome));
    }

}
