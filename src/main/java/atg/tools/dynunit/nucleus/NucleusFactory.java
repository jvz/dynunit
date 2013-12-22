package atg.tools.dynunit.nucleus;

import atg.nucleus.Nucleus;
import atg.tools.dynunit.test.configuration.BasicConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static atg.tools.dynunit.util.PropertiesUtil.setSystemProperty;
import static atg.tools.dynunit.util.PropertiesUtil.setSystemPropertyIfEmpty;

// TODO: needs to be thread-safe if it's a factory like this
// TODO: should start up a global nucleus to keep track of sub-nuclei

/**
 * @author msicker
 * @version 1.0.0
 */
public final class NucleusFactory {

    private static final Logger logger = LogManager.getLogger();

    private NucleusFactory() {
    }

    private static interface Holder {

        public static NucleusFactory instance = new NucleusFactory();
    }

    public static NucleusFactory getFactory() {
        logger.entry();
        return logger.exit(Holder.instance);
    }

    private Map<File, Nucleus> nuclei = new ConcurrentHashMap<File, Nucleus>();
    private BasicConfiguration configuration = new BasicConfiguration();
    private boolean debug;
    private String environment;
    private String atgConfigPath;
    private String localConfigPath;
    private List<String> configDestinationDirs = new LinkedList<String>();

    public void setDebug(final boolean debug) {
        logger.entry(debug);
        this.debug = debug;
        logger.exit();
    }

    public Nucleus createNucleus(@NotNull final File configPath)
            throws IOException {
        logger.entry(configPath);
        final Nucleus existing = getAlreadyRunningNucleus(configPath);
        if (existing != null) {
            return existing;
        }
        setUpConfiguration(configPath);
        readDynamoLicense();
        setSystemPropertiesFromEnvironment();
        final String fullConfigPath = buildAtgConfigPath(configPath);
        setSystemAtgConfigPath(fullConfigPath);
        final Nucleus nucleus = initializeNucleusWithConfigPath(fullConfigPath);
        nuclei.put(configPath, nucleus);
        return logger.exit(nucleus);
    }

    public Nucleus createNucleusWithModules(@NotNull final File configPath, final String[] modules) {
        throw new UnsupportedOperationException();
    }

    private Nucleus getAlreadyRunningNucleus(final File configPath) {
        logger.entry(configPath);
        if (nuclei.containsKey(configPath)) {
            final Nucleus nucleus = nuclei.get(configPath);
            if (nucleus != null && nucleus.isRunning()) {
                return logger.exit(nucleus);
            }
        }
        return logger.exit(null);
    }

    private void setUpConfiguration(final File configPath)
            throws IOException {
        logger.entry(configPath);
        configuration.setDebug(debug);
        configuration.setRoot(configPath);
        configuration.createPropertiesByConfigurationLocation();
        logger.exit();
    }

    private void readDynamoLicense() {
        logger.entry();
        setSystemPropertyIfEmpty("atg.dynamo.license.read", "true");
        setSystemPropertyIfEmpty("atg.license.read", "true");
        logger.exit();
    }

    private void setSystemPropertiesFromEnvironment() {
        logger.entry();
        final String[] properties = StringUtils.split(environment, ';');
        if (properties == null) {
            logger.exit();
            return;
        }
        for (String property : properties) {
            final String[] entry = StringUtils.split(property, '=');
            if (entry.length > 1) {
                final String key = entry[0];
                final String value = entry[1];
                logger.debug("Setting property {} = {}", key, value);
                setSystemProperty(key, value);
            }
        }
        logger.exit();
    }

    private String buildAtgConfigPath(final File configPath) {
        logger.entry(configPath);
        final List<String> configPaths = new LinkedList<String>();
        if (atgConfigPath != null) {
            configPaths.add(atgConfigPath);
        }

        if (configDestinationDirs.size() > 0) {
            configPaths.addAll(configDestinationDirs);
        }
        else {
            configPaths.add(configPath.getAbsolutePath());
        }

        if (localConfigPath != null) {
            configPaths.add(localConfigPath.replace('/', File.separatorChar));
        }

        return logger.exit(StringUtils.join(configPaths, ';'));
    }

    private void setSystemAtgConfigPath(final String configPath) {
        logger.entry(configPath);
        final File systemAtgConfigPath = new File(configPath);
        final String absoluteConfigPath = systemAtgConfigPath.getAbsolutePath();
        logger.info("ATG-Config-Path: {}", absoluteConfigPath);
        setSystemProperty("atg.configpath", absoluteConfigPath);
        logger.exit();
    }

    private Nucleus initializeNucleusWithConfigPath(final String configPath) {
        logger.entry(configPath);
        logger.debug("Starting Nucleus using config path: {}", configPath);
        return logger.exit(Nucleus.startNucleus(new String[]{ configPath }));
    }

}
