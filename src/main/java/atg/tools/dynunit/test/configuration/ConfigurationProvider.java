package atg.tools.dynunit.test.configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author msicker
 * @version 1.0.0
 */
public abstract class ConfigurationProvider {

    private File root;
    private boolean debug;

    public ConfigurationProvider() {
    }

    public ConfigurationProvider(final File root) {
        this.root = root;
    }

    public ConfigurationProvider(final boolean debug) {
        this.debug = debug;
    }

    public ConfigurationProvider(final File root, final boolean debug) {
        this.root = root;
        this.debug = debug;
    }

    public abstract void createPropertiesByConfigurationLocation()
            throws IOException;

    public File getRoot() {
        return root;
    }

    public void setRoot(final File root) {
        this.root = root;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
}
