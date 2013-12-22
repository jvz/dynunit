/*
 * Copyright 2013 Matt Sicker and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package atg.tools.dynunit.test.configuration;

import atg.nucleus.Nucleus;
import atg.service.lockmanager.ClientLockManager;
import atg.tools.dynunit.nucleus.logging.ApacheClassLoggingFactory;
import atg.tools.dynunit.nucleus.logging.ApacheLogListener;
import atg.tools.dynunit.util.ComponentUtil;
import atg.xml.tools.XMLToolsFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides a simple Nucleus configuration to start with for simple Nucleus tests. Only sets up some essential
 * services.
 *
 * @author robert
 * @author msicker
 */
public final class BasicConfiguration {

    private static final Logger logger = LogManager.getLogger();
    private boolean debug = false;
    private File root;
    private File atgDynamoService;

    public boolean isDebug() {
        logger.entry();
        return logger.exit(debug);
    }

    public void setDebug(final boolean debug) {
        logger.entry(debug);
        this.debug = debug;
        logger.exit();
    }

    public void createPropertiesByConfigurationLocation(final File root)
            throws IOException {
        logger.entry(root);
        this.root = root;
        atgDynamoService = new File(root, "atg" + File.separatorChar + "dynamo" + File.separatorChar + "service");

        createClientLockManager();
        createApacheLog();
        createGlobal();
        createInitialServices();
        createXMLToolsFactory();

        logger.info("Created basic configuration fileset");
        logger.exit();
    }

    private void createClientLockManager()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("lockServerAddress", "localhost");
        // FIXME: shouldn't this look for an available port?
        properties.setProperty("lockServerPort", "9010");
        properties.setProperty("useLockServer", "false");
        ComponentUtil.newComponent(atgDynamoService, ClientLockManager.class, properties);
        logger.exit();
    }

    private void createApacheLog()
            throws IOException {
        logger.entry();
        final File atgDynamoServiceLogging = new File(atgDynamoService, "logging");
        ComponentUtil.newComponent(atgDynamoServiceLogging, "ApacheLog", ApacheLogListener.class);
        ComponentUtil.newComponent(atgDynamoServiceLogging, "ClassLoggingFactory", ApacheClassLoggingFactory.class);
        logger.exit();
    }

    private void createGlobal()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        // we definitely want to override the default log listeners here as ATG provides a couple rather useless ones
        // by default: a screen log (which appears to use JCL) and a log dispatcher which writes different log level
        // events to different log files like warn.log, debug.log, etc.
        properties.setProperty("logListeners", "/atg/dynamo/service/logging/ApacheLog");
        properties.setProperty("loggingDebug", Boolean.toString(isDebug()));
        ComponentUtil.newComponent(root, "GLOBAL", properties);
        logger.exit();
    }

    private void createInitialServices()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("initialServiceName", "/Initial");
        ComponentUtil.newComponent(root, Nucleus.class, properties);
        logger.exit();
    }

    private void createXMLToolsFactory()
            throws IOException {
        logger.entry();
        final File xml = new File(atgDynamoService, "xml");
        ComponentUtil.newComponent(xml, "XMLToolsFactory", XMLToolsFactoryImpl.class);
        logger.exit();
    }

}
