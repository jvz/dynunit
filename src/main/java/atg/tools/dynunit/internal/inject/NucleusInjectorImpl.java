package atg.tools.dynunit.internal.inject;

import atg.nucleus.Nucleus;
import atg.tools.dynunit.Nuke;
import atg.tools.dynunit.inject.NucleusInjector;
import atg.tools.dynunit.nucleus.NucleusFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.ClassUtils.PACKAGE_SEPARATOR_CHAR;
import static org.apache.commons.lang3.ClassUtils.getPackageCanonicalName;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

/**
 * @author msicker
 * @version 1.0.0
 */
public class NucleusInjectorImpl
        implements NucleusInjector {

    private static final Logger logger = LogManager.getLogger();

    private Nucleus nucleus;
    private File configPath;
    private Object testInstance;
    private Map<Field, String> injectableFields;

    @Override
    public void init(final Object testInstance) {
        logger.entry(testInstance);
        this.testInstance = testInstance;
        findInjectableFields();
        try {
            injectNucleus();
            injectComponents();
        } catch (IOException e) {
            logException(e);
            logger.error("Couldn't create Nucleus using configPath: {}", configPath.getAbsolutePath());
        } catch (IllegalAccessException e) {
            logException(e);
            logger.error("Can't inject component instance.");
        }
        logger.exit();
    }

    private void findInjectableFields() {
        logger.entry();
        final Field[] fields = testInstance.getClass().getDeclaredFields();
        injectableFields = new ConcurrentHashMap<Field, String>(fields.length);
        for (Field field : fields) {
            if (isFieldInjectable(field)) {
                logger.debug("Found injectable field: {}", field.getName());
                addInjectableField(field);
            }
        }
        logger.exit();
    }

    private boolean isFieldInjectable(final Field field) {
        logger.entry(field);
        return logger.exit(field.isAnnotationPresent(Inject.class));
    }

    private void addInjectableField(final Field field) {
        logger.entry(field);
        injectableFields.put(field, getFieldComponentName(field));
        logger.exit();
    }

    private void removeInjectableField(final Field field) {
        logger.entry(field);
        injectableFields.remove(field);
        logger.exit();
    }

    private String getFieldComponentName(final Field field) {
        logger.entry(field);
        final Named componentName = field.getAnnotation(Named.class);
        if (componentName == null) {
            return logger.exit(generateDefaultComponentName(field));
        }
        else {
            return logger.exit(componentName.value());
        }
    }

    private String generateDefaultComponentName(final Field field) {
        logger.entry(field);
        final String packageCanonicalName = getPackageCanonicalName(field.getDeclaringClass());
        return logger.exit("/" + packageCanonicalName.replace(PACKAGE_SEPARATOR_CHAR, '/'));
    }

    private void injectNucleus()
            throws IOException, IllegalAccessException {
        logger.entry();
        for (final Field field : injectableFields.keySet()) {
            final Nuke payload = nuke(field);
            if (payload != null) {
                initializeConfigPath(payload.value());
                initializeNucleus();
                injectNucleusIntoField(field);
                removeInjectableField(field);
            }
        }
        logger.exit();
    }

    private Nuke nuke(final Field field) {
        logger.entry(field);
        return logger.exit(field.getAnnotation(Nuke.class));
    }

    private void initializeConfigPath(final String configPath) {
        logger.entry(configPath);
        this.configPath = new File(configPath);
        logger.exit();
    }

    private void initializeNucleus()
            throws IOException {
        logger.entry();
        nucleus = NucleusFactory.getFactory().createNucleus(configPath);
        logger.exit();
    }

    private void injectNucleusIntoField(final Field field)
            throws IllegalAccessException {
        logger.entry(field);
        writeField(field, testInstance, nucleus, true);
        logger.exit();
    }

    private void injectComponents()
            throws IllegalAccessException {
        logger.entry();
        for (final Map.Entry<Field, String> entry : injectableFields.entrySet()) {
            writeField(entry.getKey(), testInstance, resolveName(entry.getValue()), true);
        }
        logger.exit();
    }

    private Object resolveName(final String componentName) {
        logger.entry(componentName);
        return logger.exit(nucleus.resolveName(componentName));
    }

    private void logException(final Throwable exception) {
        logger.entry(exception);
        logger.catching(Level.ERROR, exception);
        logger.exit();
    }
}
