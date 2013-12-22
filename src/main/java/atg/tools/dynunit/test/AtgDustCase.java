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

package atg.tools.dynunit.test;

import atg.nucleus.Nucleus;
import atg.tools.dynunit.naming.LoggingNameResolver;
import atg.tools.dynunit.test.configuration.BasicConfiguration;
import atg.tools.dynunit.test.configuration.RepositoryConfiguration;
import atg.tools.dynunit.test.util.FileUtil;
import atg.tools.dynunit.test.util.RepositoryManager;
import atg.tools.dynunit.util.ComponentUtil;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static atg.tools.dynunit.util.PropertiesUtil.setSystemProperty;
import static atg.tools.dynunit.util.PropertiesUtil.setSystemPropertyIfEmpty;

// TODO: this should be converted to some sort of runner

/**
 * Replacement base class for AtgDustTestCase. Extend this class and use
 * the following 'pattern' whenever you want to junit test some atg components:
 * <ul>
 * <li><b>Copy</b> all needed configuration and repository mapping files to a
 * staging location outside of your source tree using<b>
 * {@link AtgDustCase#copyConfigurationFiles(String[], String, String...)}</b>.
 * The staging directory will automatically be used as the configuration
 * directory. Copying all needed priorities to a location outside of the source
 * tree is the preferred method, because this frameworks creates properties on
 * the fly and that could pollute your current source tree.</li>
 * <!--
 * <li><b>
 * <p/>
 * <i>Or: </i></b>tell {@link AtgDustCase} class where the configuration
 * location is by using <b>{@link AtgDustCase#setConfigurationLocation(String)}
 * </b>, but be aware that the location will also be used for properties file
 * generation.</li>
 * -->
 * </ul>
 * <p/>
 * <!-- p> <b>Rule of thumb:</b> When running repository tests, copy everything
 * outside of your source tree (or when you use maven, use the target directory
 * ). If you run basic component/formhandler tests, pointing it to your existing
 * configuration directory might be sufficient.
 * <p/>
 * </p-->
 * <p/>
 * Repository based tests are depended on one of the two steps previously
 * described plus:
 * <ul>
 * <li><b>{@link AtgDustCase#prepareRepository(String, String...)}</b> for
 * testing against an default in-memory hsql database or <b>
 * {@link AtgDustCase#prepareRepository(String, java.util.Properties, boolean, boolean, String...)}
 * </b> for testing against an existing database.</li>
 * </ul>
 * <p/>
 * If you need to generate some components "on the fly":
 * <ul>
 * <li><b>{@link AtgDustCase#createPropertyFile(String, String, Class)}</b></li>
 * </ul>
 * <p/>
 * <p>
 * Example usage can be found in test.SongsRepositoryTest.
 * </p>
 * <p/>
 * <p>
 * This class overrides Junit 3 and not Junit 4 because currently Junit 4 has
 * some test runner/eclipse related bugs which makes it impossible for me to use
 * it.
 * </p>
 *
 * @author robert
 */
public class AtgDustCase
        extends TestCase {

    private static final Logger logger = LogManager.getLogger();

    private final RepositoryManager repositoryManager = new RepositoryManager();

    private final BasicConfiguration basicConfiguration = new BasicConfiguration();

    private File configurationLocation;

    private Nucleus nucleus;

    private LoggingNameResolver loggingNameResolver;

    private boolean debug;

    private String atgConfigPath;

    private String environment;

    private String localConfig;

    private List<String> configDstsDir;

    @Nullable
    private static final Map<String, Long> CONFIG_FILES_TIMESTAMPS;

    private static Map<String, Long> CONFIG_FILES_GLOBAL_FORCE = null;

    private static Class<?> perflib;

    private static final File TIMESTAMP_SER = new File(
            FileUtils.getTempDirectory(), "dynunit-timestamp.ser"
    );

    private static final File GLOBAL_FORCE_SER = new File(
            FileUtils.getTempDirectory(), "dynunit-global-force.ser"
    );

    private static long SERIAL_TTL = 43200000L;

    /**
     * Every *.properties file copied using this method will have it's scope (if one is available) set to global.
     *
     * @param sourceDirectories
     *         One or more directories containing needed configuration files.
     * @param destinationDirectory
     *         where to copy the above files to. This will also be the
     *         configuration location.
     * @param excludedDirectories
     *         One or more directories not to include during the copy
     *         process. Use this one to speeds up the test cycle
     *         considerably. You can also call it with an empty
     *         {@link String[]} or <code>null</code> if nothing should be
     *         excluded
     *
     * @throws IOException
     *         Whenever some file related error's occur.
     */
    protected final void copyConfigurationFiles(@NotNull final String[] sourceDirectories,
                                                @NotNull final String destinationDirectory,
                                                @Nullable final String... excludedDirectories)
            throws IOException {
        logger.entry(sourceDirectories, destinationDirectory, excludedDirectories);
        setConfigurationLocation(destinationDirectory);

        logger.info("Copying configurating files and forcing global scope on all configs.");
        preCopyingOfConfigurationFiles(sourceDirectories, excludedDirectories);
        for (final String sourceDirectory : sourceDirectories) {
            FileUtils.copyDirectory(new File(sourceDirectory), new File(destinationDirectory), new FileFilter() {
                @Override
                public boolean accept(final File file) {
                    return ArrayUtils.contains(excludedDirectories, file.getName());
                }
            });
        }
        forceGlobalScopeOnAllConfigs(destinationDirectory);

        if (FileUtil.isDirty()) {
            FileUtil.serialize(
                    GLOBAL_FORCE_SER, FileUtil.getConfigFilesTimestamps()
            );
        }
        logger.exit();
    }

    /**
     * Donated by Remi Dupuis
     *
     * @param properties
     *
     * @throws IOException
     */
    protected final void manageConfigurationFiles(Properties properties)
            throws IOException {
        logger.entry(properties);

        String atgConfigPath = properties.getProperty("atgConfigsJars")
                                         .replace("/", File.separator);
        String[] configs = properties.getProperty("configs").split(",");
        String environment = properties.getProperty("environment");
        String localConfig = properties.getProperty("localConfig");
        String[] excludes = properties.getProperty("excludes").split(",");
        String rootConfigDir = properties.getProperty("rootConfigDir").replace(
                "/", File.separator
        );
        int i = 0;
        for (String conf : configs) {
            String src = conf.split(" to ")[0];
            String dst = conf.split(" to ")[1];
            configs[i] = (rootConfigDir
                    + "/"
                    + src.trim()
                    + " to "
                    + rootConfigDir
                    + "/"
                    + dst.trim()).replace(
                    "/", File.separator
            );
            i++;
        }
        i = 0;
        for (String dir : excludes) {
            excludes[i] = dir.trim();
            i++;
        }
        final List<String> srcsAsList = new ArrayList<String>(configs.length);
        final List<String> distsAsList = new ArrayList<String>(configs.length);

        for (String config : configs) {
            srcsAsList.add(config.split(" to ")[0]);
            distsAsList.add(config.split(" to ")[1]);
        }

        this.atgConfigPath = atgConfigPath;
        this.environment = environment;
        this.localConfig = localConfig;
        // The Last dstdir is used for Configuration location
        setConfigurationLocation(distsAsList.get(distsAsList.size() - 1));

        logger.debug("Copying configuration files and forcing global scope on all configs.");
        preCopyingOfConfigurationFiles(
                srcsAsList.toArray(new String[srcsAsList.size()]), excludes
        );

        logger.info("Copying configuration files and forcing global scope on all configs");
        // copy all files to it's destination
        for (String config : configs) {
            FileUtil.copyDirectory(
                    config.split(" to ")[0],
                    config.split(" to ")[1],
                    Arrays.asList(excludes)
            );
            logger.debug(config);
            logger.debug(config.split(" to ")[0]);
            logger.debug(config.split(" to ")[1]);
        }

        // forcing global scope on all configurations
        for (String config : configs) {
            String dstDir = config.split(" to ")[1];
            // forcing global scope on all property files
            forceGlobalScopeOnAllConfigs(dstDir);
        }
        this.configDstsDir = distsAsList;

    }

    /**
     * @param configurationStagingLocation
     *         The location where the property file should be created. This will also set the
     *         {@link AtgDustCase#configurationLocation}.
     * @param nucleusComponentPath
     *         Nucleus component path (e.g /Some/Service/Impl).
     * @param klass
     *         The {@link Class} implementing the nucleus component specified in previous argument.
     *
     * @throws IOException
     *         If we have some File related errors
     */
    protected final void createPropertyFile(final String configurationStagingLocation,
                                            final String nucleusComponentPath,
                                            final Class<?> klass)
            throws IOException {
        configurationLocation = new File(configurationStagingLocation);
        final String componentFileName = nucleusComponentPath.replaceAll("/", File.separator) + ".properties";
        final File componentFile = new File(configurationLocation, componentFileName);
        ComponentUtil.newComponentForFile(componentFile, klass);
    }

    /**
     * Prepares a test against an in-memory hsql database.
     *
     * @param repoPath
     *         the nucleus component path of the repository to be tested.
     * @param definitionFiles
     *         one or more repository definition files.
     *
     * @throws IOException
     *         The moment we have some properties/configuration related
     *         error
     * @throws SQLException
     *         Whenever there is a database related error
     */
    protected final void prepareRepository(final String repoPath, final String... definitionFiles)
            throws SQLException, IOException {

        final Properties properties = new Properties();
        properties.put("driver", "org.hsqldb.jdbcDriver");
        properties.put("url", "jdbc:hsqldb:mem:testDb");
        properties.put("user", "sa");
        properties.put("password", "");

        prepareRepository(repoPath, properties, true, true, definitionFiles);

    }

    /**
     * Prepares a test against an existing database.
     *
     * @param repositoryPath
     *         The the repository to be tested, specified as nucleus
     *         component path.
     * @param connectionProperties
     *         A {@link Properties} instance with the following values (in
     *         this example the properties are geared towards an mysql
     *         database):
     *         <p/>
     *         <pre>
     *                                                                                                                     final
     *                                                             Properties properties = new
     *                                                                                         Properties();
     *
     *                                                             properties.put(&quot;driver&quot;,
     *                                                                                         &quot;com.mysql.jdbc.Driver&quot;);
     *
     *                                                             properties.put(&quot;url&quot;,
     *                                                                                         &quot;jdbc:mysql://localhost:3306/someDb&quot;);
     *
     *                                                             properties.put(&quot;user&quot;,
     *                                                                                         &quot;someUserName&quot;);
     *
     *                                                             properties.put(&quot;password&quot;,
     *                                                                                         &quot;somePassword&quot;);
     *                                                                                                                     </pre>
     * @param dropTables
     *         If <code>true</code> then existing tables will be dropped and
     *         re-created, if set to <code>false</code> the existing tables
     *         will be used.
     * @param createTables
     *         if set to <code>true</code> all non existing tables needed for
     *         the current test run will be created, if set to
     *         <code>false</code> this class expects all needed tables for
     *         this test run to be already created
     * @param definitionFiles
     *         One or more needed repository definition files.
     *
     * @throws IOException
     *         The moment we have some properties/configuration related
     *         error
     * @throws SQLException
     *         Whenever there is a database related error
     */
    protected final void prepareRepository(final String repositoryPath,
                                           final Properties connectionProperties,
                                           final boolean dropTables,
                                           final boolean createTables,
                                           final String... definitionFiles)
            throws SQLException, IOException {

        final Map<String, String> connectionSettings = new HashMap<String, String>();

        for (final Entry<Object, Object> entry : connectionProperties.entrySet()) {
            connectionSettings.put(
                    (String) entry.getKey(), (String) entry.getValue()
            );

        }
        final RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();

        repositoryConfiguration.setDebug(debug);
        repositoryConfiguration.setRoot(configurationLocation);
        repositoryConfiguration.createPropertiesByConfigurationLocation();
        repositoryConfiguration.createFakeXADataSource(connectionProperties);
        repositoryConfiguration.createRepository(repositoryPath, dropTables, createTables, definitionFiles);

        repositoryManager.initializeMinimalRepositoryConfiguration(
                configurationLocation,
                repositoryPath,
                connectionSettings,
                dropTables,
                debug,
                definitionFiles
        );
    }

    /**
     * Method for retrieving a fully injected ATG component.
     *
     * @param nucleusComponentPath
     *         Path to a nucleus component (e.g. /Some/Service/Impl).
     *
     * @return Fully injected instance of the component registered under previous argument or {@code null} if there
     * is an error.
     *
     * @throws IOException
     */
    protected Object resolveNucleusComponent(final String nucleusComponentPath)
            throws IOException {
        logger.entry(nucleusComponentPath);
        startNucleus(configurationLocation);
        final Object component = loggingNameResolver.resolveName(nucleusComponentPath);
        return logger.exit(component);
    }

    /**
     * Call this method to set the configuration location.
     *
     * @param configurationLocation
     *         The configuration location to set. Most of the time this
     *         location is a directory containing all repository definition
     *         files and component property files which are needed for the
     *         test.
     */
    protected final void setConfigurationLocation(final String configurationLocation) {
        logger.entry(configurationLocation);
        this.configurationLocation = new File(configurationLocation);
        logger.debug("Using configuration location: {}", this.configurationLocation.getPath());
        logger.exit();
    }

    /**
     * Always make sure to call this because it will do necessary clean up
     * actions (shutting down in-memory database (if it was used) and the
     * nucleus) so he next test can run safely.
     */
    @Override
    protected void tearDown()
            throws Exception {
        super.tearDown();
        repositoryManager.shutdownInMemoryDbAndCloseConnections();
        if (nucleus != null) {
            nucleus.doStopService();
            nucleus.stopService();
            nucleus.destroy();
        }
    }

    /**
     * Enables or disables the debug level of nucleus components.
     *
     * @param debug
     *         Setting this to <code>true</code> will enable debug on all
     *         (currently only on repository related) components, setting it
     *         to <code>false</code> turns the debug off again.
     */
    protected void setDebug(final boolean debug) {
        this.debug = debug;
    }

    private void startNucleus(final File configPath)
            throws IOException {
        if (nucleus == null || !nucleus.isRunning()) {
            basicConfiguration.setDebug(debug);
            basicConfiguration.setRoot(configPath);
            basicConfiguration.createPropertiesByConfigurationLocation();
            setSystemPropertyIfEmpty("atg.dynamo.license.read", "true");
            setSystemPropertyIfEmpty("atg.license.read", "true");
            // TODO: Can I safely keep this one disabled?
            // NucleusServlet.addNamingFactoriesAndProtocolHandlers();

            if (StringUtils.isNotEmpty(environment)) {
                for (final String property : StringUtils.split(environment, ';')) {
                    final String[] strings = StringUtils.split(property, '=');
                    final String key = strings[0];
                    final String value = strings[1];
                    setSystemProperty(key, value);
                    logger.debug("{} = {}", key, value);
                }
            }

            String fullConfigPath = "";
            if (atgConfigPath != null && !atgConfigPath.equals("")) {
                fullConfigPath = atgConfigPath + ";" + fullConfigPath;
            }
            if (configDstsDir != null && configDstsDir.size() > 0) {
                for (String dst : configDstsDir) {
                    fullConfigPath = fullConfigPath + dst + ";";
                }
            }
            else {
                fullConfigPath = configPath.getAbsolutePath();
            }
            if (localConfig != null && !localConfig.equals("")) {
                fullConfigPath = fullConfigPath + localConfig.replace("/", File.separator);
            }

            logger.info("The full config path used to start nucleus: {}", fullConfigPath);
            System.setProperty(
                    "atg.configpath", new File(fullConfigPath).getAbsolutePath()
            );
            nucleus = Nucleus.startNucleus(new String[]{ fullConfigPath });
            loggingNameResolver = new LoggingNameResolver(nucleus);
        }
    }

    private void preCopyingOfConfigurationFiles(final String[] srcDirs, final String excludes[])
            throws IOException {
        boolean isDirty = false;
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return ArrayUtils.contains(excludes, file.getName());
            }
        };
        // TODO: use FileUtils.copyDirectory, etc.
        for (final String src : srcDirs) {
            final Collection<File> srcFiles = FileUtils.listFiles(new File(src), null, true);
            for (final File file : srcFiles) {
                if (!Arrays.asList(
                        excludes == null ? new String[]{ } : excludes
                ).contains(file.getName()) && !file.getPath().contains(".svn") && file.isFile()) {
                    if (CONFIG_FILES_TIMESTAMPS.get(file.getPath()) != null
                            && file.lastModified() == CONFIG_FILES_TIMESTAMPS.get(file.getPath())) {
                    }
                    else {
                        CONFIG_FILES_TIMESTAMPS.put(
                                file.getPath(), file.lastModified()
                        );
                        isDirty = true;
                    }
                }
            }
        }
        if (isDirty) {
            logger.debug("Config files timestamps map is dirty an will be re serialized");

            FileUtil.serialize(TIMESTAMP_SER, CONFIG_FILES_TIMESTAMPS);
        }

        FileUtil.setConfigFilesTimestamps(CONFIG_FILES_TIMESTAMPS);
        FileUtil.setConfigFilesGlobalForce(CONFIG_FILES_GLOBAL_FORCE);
    }

    private void forceGlobalScopeOnAllConfigs(final String dstDir)
            throws IOException {
        // TODO: convert perflib usage to standard JDK
        if (perflib == null) {
            for (final File file : FileUtils.listFiles(
                    new File(
                            dstDir
                    ), new String[]{ "properties" }, true
            )) {
                FileUtil.forceGlobalScope(file);
            }
        }
        else {
            try {
                List<File> payload = (List<File>) FileUtils.listFiles(
                        new File(
                                dstDir
                        ), new String[]{ "properties" }, true
                );

                Method schedule = perflib.getMethod(
                        "schedule", new Class[]{
                        int.class, List.class, Class.class, String.class, Class[].class, List.class
                }
                );

                List<Object> list = new ArrayList<Object>();
                list.add("$scope=");
                list.add("$scope=global\n");
                schedule.invoke(
                        perflib.newInstance(),
                        4,
                        payload,
                        FileUtil.class,
                        "searchAndReplace",
                        new Class[]{
                                String.class, String.class, File.class
                        },
                        list
                );
            } catch (Exception e) {
                logger.catching(e);
            }
        }

    }

    static {
        final String s = System.getProperty("SERIAL_TTL");
        if (s == null) {
            logger.debug(
                    "SERIAL_TTL has not been set. Using default value of {} ms, or start VM with -DSERIAL_TTL=n",
                    SERIAL_TTL
            );
        }
        else {
            logger.debug("SERIAL_TTL set to {}", s);
        }
        try {
            SERIAL_TTL = s != null ? Long.parseLong(s) * 1000 : SERIAL_TTL;
        } catch (NumberFormatException e) {
            logger.catching(e);
            logger.warn("The value given by SERIAL_TTL, {}, could not be parsed.", s);
        }
        CONFIG_FILES_TIMESTAMPS = FileUtil.deserialize(
                TIMESTAMP_SER, SERIAL_TTL
        );
        CONFIG_FILES_GLOBAL_FORCE = FileUtil.deserialize(
                GLOBAL_FORCE_SER, SERIAL_TTL
        );

        try {
            perflib = Class.forName("com.bsdroot.util.concurrent.SchedulerService");
        } catch (ClassNotFoundException e) {
            logger.catching(e);
            logger.debug(
                    "com.bsdroot.util.concurrent experimental performance library not found, continuing normally"
            );
        }
    }

}