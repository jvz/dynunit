package atg.tools.dynunit.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * Utility class to create component properties files. This is useful for making component files on the fly during
 * unit tests, Nucleus instantiation, etc.
 *
 * @author msicker
 * @version 1.0.0
 */
public final class ComponentUtil {

    private static final Logger logger = LogManager.getLogger();

    private ComponentUtil() {
    }

    /**
     * Creates a new component properties file in {@code parent} named {@code name} using the class
     * {@code canonicalClassName} and configured with the given {@code properties}.
     *
     * @param parent
     *         Directory to place the new component properties file.
     * @param name
     *         Name of component to create. Determines the file name.
     * @param canonicalClassName
     *         Full package-qualified class name to use for component.
     * @param properties
     *         Any properties to set in the component properties file.
     *
     * @return Newly created (or recreated) component properties file.
     *
     * @throws IOException
     *         if the properties file couldn't be written to.
     */
    public static File newComponent(final File parent,
                                    final String name,
                                    final String canonicalClassName,
                                    final Properties properties)
            throws IOException {
        logger.entry(parent, name, canonicalClassName, properties);
        makeDirectory(parent);
        final File output = getComponentPropertiesFile(parent, name);
        newComponentForFile(output, canonicalClassName, properties);
        return logger.exit(output);
    }

    /**
     * Creates a new component properties file in {@code parent} named {@code name} using the class {@code klass} and
     * configured with the given {@code properties}.
     *
     * @param parent
     *         Directory to place the new component properties file.
     * @param name
     *         Name of component to create. Determines the file name.
     * @param klass
     *         Class of component to create.
     * @param properties
     *         Any properties to set in the component properties file.
     *
     * @return Newly created (or recreated) component properties file.
     *
     * @throws IOException
     *         if the properties file couldn't be written to.
     */
    public static File newComponent(final File parent,
                                    final String name,
                                    final Class<?> klass,
                                    final Properties properties)
            throws IOException {
        logger.entry(parent, name, klass, properties);
        final File output = newComponent(parent, name, klass.getCanonicalName(), properties);
        return logger.exit(output);
    }

    /**
     * Creates a new component properties file in {@code parent} named the same as the class {@code klass} and
     * configured with the given {@code properties}.
     *
     * @param parent
     *         Directory to place the new component properties file.
     * @param klass
     *         Class of component to create. Determines the file name.
     * @param properties
     *         Any properties to set in the component properties file.
     *
     * @return Newly created (or recreated) component properties file.
     *
     * @throws IOException
     *         if the properties file couldn't be written to.
     */
    public static File newComponent(final File parent,
                                    final Class<?> klass,
                                    final Properties properties)
            throws IOException {
        logger.entry(parent, klass, properties);
        final File output = newComponent(parent, klass.getName(), klass.getCanonicalName(), properties);
        return logger.exit(output);
    }

    /**
     * Creates a new component properties file in {@code parent} using the class {@code klass}, named the same as
     * {@code klass}, and using no additional properties.
     *
     * @param parent
     *         Directory to place the new component properties file.
     * @param klass
     *         Class of component to create. Determines the file name.
     *
     * @return Newly created (or recreated) component properties file.
     *
     * @throws IOException
     *         if the properties file couldn't be written to.
     */
    public static File newComponent(final File parent,
                                    final Class<?> klass)
            throws IOException {
        logger.entry(parent, klass);
        final File output = newComponent(parent, klass, new Properties());
        return logger.exit(output);
    }

    /**
     * Creates a new component properties file at {@code output} using the class {@code canonicalClassName} and
     * configured with the given {@code properties}. This method is generally useful when you have a properties file
     * already and wish to overwrite it.
     *
     * @param output
     *         The component properties file to write to.
     * @param canonicalClassName
     *         Full package-qualified class name to use for component.
     * @param properties
     *         Any properties to set in the component properties file.
     *
     * @return The provided {@code output} component properties file.
     *
     * @throws IOException
     *         if {@code output} couldn't be written to.
     */
    public static File newComponentForFile(final File output,
                                           final String canonicalClassName,
                                           final Properties properties)
            throws IOException {
        logger.entry(output, canonicalClassName, properties);
        recreateFile(output);
        writeComponentPropertiesToFile(output, canonicalClassName, properties);
        return logger.exit(output);
    }

    /**
     * Creates a new component properties file at {@code output} using the class {@code klass} and configured with the
     * given {@code properties}. This method is generally useful when you have a properties file already and wish to
     * overwrite it.
     *
     * @param output
     *         The component properties file to write to.
     * @param klass
     *         Class of component to create.
     * @param properties
     *         Any properties to set in the component properties file.
     *
     * @return The provided {@code output} component properties file.
     *
     * @throws IOException
     *         if {@code output} couldn't be written to.
     */
    public static File newComponentForFile(final File output,
                                           final Class<?> klass,
                                           final Properties properties)
            throws IOException {
        logger.entry(output, klass, properties);
        newComponentForFile(output, klass.getCanonicalName(), properties);
        return logger.exit(output);
    }

    private static void makeDirectory(final File directory)
            throws IOException {
        logger.entry(directory);
        if (directory != null) {
            FileUtils.forceMkdir(directory);
        }
        logger.exit();
    }

    private static File getComponentPropertiesFile(final File parent, final String name)
            throws FileNotFoundException {
        logger.entry(parent, name);
        if (name == null) {
            throw logger.throwing(new FileNotFoundException("No name was given."));
        }
        return logger.exit(new File(parent, name + ".properties"));
    }

    private static void recreateFile(final File file)
            throws IOException {
        logger.entry(file);
        if (file != null) {
            if (file.exists()) {
                FileUtils.forceDelete(file);
            }
            if (!file.createNewFile()) {
                throw logger.throwing(new IOException("File already exists but can't be deleted."));
            }
        }
        logger.exit();
    }

    private static void writeComponentPropertiesToFile(final File output,
                                                       final String canonicalClassName,
                                                       final Properties properties)
            throws IOException {
        logger.entry(output, canonicalClassName, properties);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
            pw.print("$class=");
            pw.print(canonicalClassName);
            properties.store(pw, null);
        } finally {
            if (pw != null) {
                pw.close();
            }
            logger.exit();
        }
    }

}
