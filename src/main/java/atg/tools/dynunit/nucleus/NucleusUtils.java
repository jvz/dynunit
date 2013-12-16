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

package atg.tools.dynunit.nucleus;

import atg.applauncher.AppLauncher;
import atg.applauncher.AppLauncherException;
import atg.applauncher.AppModuleManager;
import atg.applauncher.MultiInstallLocalAppModuleManager;
import atg.applauncher.dynamo.DynamoServerLauncher;
import atg.core.util.CommandProcessor;
import atg.core.util.JarUtils;
import atg.nucleus.DynamoEnv;
import atg.nucleus.GenericContext;
import atg.nucleus.GenericService;
import atg.nucleus.InitialService;
import atg.nucleus.Nucleus;
import atg.nucleus.PropertyEditors;
import atg.nucleus.ServiceException;
import atg.nucleus.naming.ComponentName;
import atg.nucleus.servlet.NucleusServlet;
import atg.service.dynamo.ServerConfig;
import atg.tools.dynunit.DynUnit;
import atg.tools.dynunit.test.util.FileUtil;
import atg.tools.dynunit.util.ComponentUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static atg.tools.dynunit.util.PropertiesUtil.getSystemProperty;
import static atg.tools.dynunit.util.PropertiesUtil.setDynamoProperty;
import static atg.tools.dynunit.util.PropertiesUtil.setDynamoPropertyIfEmpty;
import static atg.tools.dynunit.util.PropertiesUtil.setSystemProperty;
import static atg.tools.dynunit.util.PropertiesUtil.setSystemPropertyIfEmpty;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

// TODO: factor out configPath methods to its own class
// TODO: migrate parts to NucleusFactory

/**
 * Utility class for aiding in Nucleus component resolution.
 *
 * @author adamb
 * @author msicker
 */
@Deprecated
public class NucleusUtils {

    //-------------------------------------

    public static final String DYNUNIT_TESTCONFIG = "atg.tools.dynunit.testconfig";
    private static final Logger logger = LogManager.getLogger();
    private static final String EXTRACT_TEMP_JAR_FILE_FOR_PATH = "Extract temp jar file for path ";
    private static final String DYNUNIT_CONFIG = "dynunit-config-";
    private static final String FILE = "file:";
    private static final String DYNUNIT_TESTCONFIG_ENV = "DYNUNIT_TESTCONFIG";
    /**
     * A map from Nucleus instance to temporary directory. Used by
     * startNucleusWithModules.
     */
    private static final ConcurrentMap<Nucleus, File> nucleiConfigPathsCache = new ConcurrentHashMap<Nucleus, File>();
    /**
     * Cache of the config path for a given Class. Used by getConfigPath.
     */
    private static final ConcurrentMap<Class, Map<String, File>> configPath = new ConcurrentHashMap<Class, Map<String, File>>();

    /**
     * Creates an Initial.properties file
     *
     * @param rootConfigPath
     *         The root of the config path entry.
     * @param initialServices
     *         initial services list
     *
     * @return the create initial services properties file.
     *
     * @throws IOException
     *         if an error occurs
     */
    public static File createInitial(File rootConfigPath, Iterable<String> initialServices)
            throws IOException {
        Properties prop = new Properties();
        prop.setProperty("initialServices", StringUtils.join(initialServices, ','));
        return ComponentUtil.newComponent(rootConfigPath, "Initial", InitialService.class, prop);
    }

    /**
     * @see atg.tools.dynunit.util.ComponentUtil#newComponent(java.io.File, String, String, java.util.Properties)
     */
    @Deprecated
    public static File createProperties(String componentName,
                                        @Nullable File configDir,
                                        String className,
                                        Properties properties)
            throws IOException {
        return ComponentUtil.newComponent(configDir, componentName, className, properties);
    }

    /**
     * Allows the absoluteName of the given service to be explicitly defined.
     * Normally this is determined by the object's location in the Nucleus
     * hierarchy.  For test items that are not really bound to Nucleus, it's
     * convenient to just give it an absolute name rather than going through
     * the whole configuration and binding process.
     *
     * @param absoluteName
     *         The absolute name value to set
     * @param service
     *         The service whose absolute name should be set.
     */
    public static void setAbsoluteName(String absoluteName, GenericService service)
            throws IllegalAccessException {
        FieldUtils.writeDeclaredField(service, "mAbsoluteName", absoluteName, true);
    }

    /**
     * Adds the given object, component to Nucleus, nucleus at the path given
     * by componentPath.
     *
     * @param nucleus
     *         The Nucleus instance to which the component should be added
     * @param componentPath
     *         the component path at which the component should be added
     * @param component
     *         the component instance to add
     */
    public static void addComponent(Nucleus nucleus, String componentPath, Object component) {
        // make sure it's not already there
        if (nucleus.resolveName(componentPath) != null) {
            return;
        }
        ComponentName name = ComponentName.getComponentName(componentPath);
        ComponentName[] subNames = name.getSubNames();
        GenericContext[] contexts = new GenericContext[subNames.length - 1];
        contexts[0] = nucleus;
        for (int i = 1; i < subNames.length - 1; i++) {
            contexts[i] = new GenericContext();
            // Make sure it's not there
            GenericContext tmpContext = (GenericContext) contexts[i
                    - 1].getElement(subNames[i].getName());
            if (tmpContext == null) {
                contexts[i - 1].putElement(subNames[i].getName(), contexts[i]);
            }
            else {
                contexts[i] = tmpContext;
            }
        }
        contexts[contexts.length - 1].putElement(
                subNames[subNames.length - 1].getName(), component
        );
    }

    /**
     * Starts Nucleus using the given config directory
     *
     * @param configPath
     *         the config path directory entry
     *         to use as the entire config path.
     *
     * @return the started Nucleus
     */
    public static Nucleus startNucleus(File configPath) {
        return startNucleus(configPath.getAbsolutePath());
    }

    /**
     * Starts Nucleus using the given config directory
     *
     * @param configPath
     *         the path name of the config path
     *         entry to specify.
     *
     * @return The started nucleus.
     */
    public static Nucleus startNucleus(String configPath) {
        setSystemPropertyIfEmpty("atg.dynamo.license.read", "true");
        setSystemPropertyIfEmpty("atg.license.read", "true");
        NucleusServlet.addNamingFactoriesAndProtocolHandlers();
        return Nucleus.startNucleus(new String[]{ configPath });
    }

    /**
     * A convenience method for returning the configpath for a test.
     * baseConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the "config/data" subdirectory of the passed in file.
     *
     * @param baseConfigDirectory
     *         the base configuration directory.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigPath(String baseConfigDirectory) {
        return getConfigPath(NucleusUtils.class, baseConfigDirectory, true);
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the baseConfigDirectory (or baseConfigDirectory +
     * "data") subdirectory of the the passed in class's location.<P>
     * <p/>
     * The directory location is calculated as (in psuedo-code):
     * <code>
     * (classRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
     * </code>
     *
     * @param classRelativeTo
     *         the class whose package the config/data
     *         (or baseConfigDirectory/data) should be relative in.
     * @param baseConfigDirectory
     *         the base configuration directory If null,
     *         uses "config".
     * @param createDirectory
     *         whether to create the config/data subdirectory if
     *         it does not exist.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigPath(Class classRelativeTo,
                                     String baseConfigDirectory,
                                     boolean createDirectory) {
        Map<String, File> baseConfigToFile = configPath.get(classRelativeTo);
        if (baseConfigToFile == null) {
            baseConfigToFile = new ConcurrentHashMap<String, File>();
            configPath.put(classRelativeTo, baseConfigToFile);
        }

        File fileFound = baseConfigToFile.get(baseConfigDirectory);

        if (!baseConfigToFile.containsKey(baseConfigDirectory)) {
            String configdirname = "config";
            String packageName = StringUtils.replaceChars(
                    classRelativeTo.getPackage().getName(), '.', '/'
            );
            if (baseConfigDirectory != null) {
                configdirname = baseConfigDirectory;
            }

            String configFolder = packageName + "/data/" + configdirname;
            URL dataURL = classRelativeTo.getClassLoader().getResource(configFolder);

            // Mkdir
            if (dataURL == null) {
                URL root = classRelativeTo.getClassLoader().getResource(packageName);

                File f = null;
                if (root != null) {
                    f = new File(root.getFile());
                }
                File f2 = new File(f, "/data/" + configdirname);
                if (createDirectory) {
                    f2.mkdirs();
                }
                dataURL = NucleusUtils.class.getClassLoader().getResource(configFolder);
                if (dataURL == null) {
                    System.err.println(
                            "Warning: Could not find resource \"" +
                                    configFolder + "\" in CLASSPATH"
                    );
                }
            }
            if (dataURL != null) {// check if this URL is contained within a jar file
                // if so, extract to a temp dir, otherwise just return
                // the directory
                fileFound = extractJarDataURL(dataURL);
                baseConfigToFile.put(baseConfigDirectory, fileFound);
            }
        }
        if (fileFound != null) {
            System.setProperty(
                    "atg.configpath", fileFound.getAbsolutePath()
            );
        }
        return fileFound;
    }

    /**
     * This method is used to extract a configdir from a jar archive.
     * Given a URL this method will extract the jar contents to a temp dir and return that path.
     * It also adds a shutdown hook to cleanup the tmp dir on normal jvm completion.
     * If the given URL does not appear to be a path into a jar archive, this method returns
     * a new File object initialized with <code>dataURL.getFile()</code>.
     *
     * @return A temporary directory to be used as a configdir
     */
    private static File extractJarDataURL(URL dataURL) {
        // TODO: Extract to a temp location
        // atg.core.util.JarUtils.extractEntry(arg0, arg1, arg2)

        int endIndex = dataURL.getFile().lastIndexOf('!');
        if (endIndex == -1) {
            // Not a jar file url
            return new File(dataURL.getFile());
        }
        logger.info(EXTRACT_TEMP_JAR_FILE_FOR_PATH + dataURL.getFile());
        File configDir = null;
        try {
            final File tempFile = FileUtil.newTempFile();
            final File tmpDir = new File(
                    tempFile.getParentFile(), DYNUNIT_CONFIG + System.currentTimeMillis()
            );

            String jarPath = dataURL.getFile().substring(0, endIndex);
            // Strip leading file:
            int fileColonIndex = jarPath.indexOf(FILE) + FILE.length();
            jarPath = jarPath.substring(fileColonIndex, jarPath.length());
            JarUtils.unJar(new File(jarPath), tmpDir, false);
            // Now get the configpath dir relative to this temp dir
            String relativePath = dataURL.getFile().substring(
                    endIndex + 1, dataURL.getFile().length()
            );
            FileUtils.forceDeleteOnExit(tmpDir);
            configDir = new File(tmpDir, relativePath);
        } catch (IOException e) {
            logger.catching(e);
        }
        return configDir;
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the "data" subdirectory of the passed in file.
     *
     * @param baseConfigDirectory
     *         the base configuration directory.
     * @param createDirectory
     *         whether to create the config/data subdirectory if
     *         it does not exist.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigPath(String baseConfigDirectory, boolean createDirectory) {
        return getConfigPath(NucleusUtils.class, baseConfigDirectory, createDirectory);
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the baseConfigDirectory (or baseConfigDirectory +
     * "data") subdirectory of the the passed in class's location.
     * <p/>
     * The directory location is calculated as (in pseudo-code): <code>
     * (classRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
     * </code>
     * This method always creates the config/data subdirectory if it does not
     * exist.
     *
     * @param classRelativeTo
     *         the class whose package the config/data (or
     *         baseConfigDirectory/data) should be relative in.
     * @param baseConfigDirectory
     *         the base configuration directory If null, uses "config".
     *
     * @return The calculated configuration path.
     */

    public static File getConfigPath(Class classRelativeTo, String baseConfigDirectory) {
        return getConfigPath(classRelativeTo, baseConfigDirectory, true);
    }

    public static String getGlobalTestConfig() {
        String config = getSystemProperty(DYNUNIT_TESTCONFIG);
        if (config == null) {
            config = System.getenv(DYNUNIT_TESTCONFIG_ENV);
        }
        // If that's null, there is no global test config specified
        return config;
    }

    public static Nucleus startNucleusWithModules(String[] modules,
                                                  Class classRelativeTo,
                                                  String initialService)
            throws ServletException, FileNotFoundException {
        return startNucleusWithModules(
                new NucleusStartupOptions(
                        modules,
                        classRelativeTo,
                        classRelativeTo.getSimpleName() + "/config", // FIXME: factor into private method
                        initialService
                )
        );
    }

    private static File getExistingFile(final String name)
            throws FileNotFoundException {
        notEmpty(name);
        final File file = new File(name);
        if (file.exists()) {
            return file;
        }
        throw new FileNotFoundException(name);
    }

    public static Nucleus startNucleusWithModules(NucleusStartupOptions startupOptions)
            throws ServletException, FileNotFoundException {
        notNull(startupOptions);
        notEmpty(startupOptions.getInitialService());
        final File dynamoRoot = getExistingFile(findDynamoRoot());
        setDynamoPropertyIfEmpty("atg.dynamo.root", dynamoRoot.getAbsolutePath());
        final File dynamoHome = new File(dynamoRoot, "home");
        setDynamoPropertyIfEmpty("atg.dynamo.home", dynamoHome.getAbsolutePath());
        final String modulesPath = StringUtils.join(startupOptions.getModules(), File.pathSeparatorChar);
        setDynamoProperty("atg.dynamo.modules", modulesPath);
        setSystemPropertyIfEmpty("atg.dynamo.license.read", "true");
        setSystemPropertyIfEmpty("atg.license.read", "true");

        // our temporary server directory.
        File fileServerDir = null;

        try {

            AppModuleManager moduleManager = new MultiInstallLocalAppModuleManager(
                    dynamoRoot.getAbsolutePath(), dynamoRoot, modulesPath
            );

            AppLauncher launcher = AppLauncher.getLauncher(moduleManager, modulesPath);

            // Start Nucleus
            String configpath = DynamoServerLauncher.calculateConfigPath(
                    launcher, startupOptions.getLiveConfig(), startupOptions.getLayersAsString(), false, null
            );

            // use the NucleusUtils config dir as a base, since it
            // empties out license checks, etc.
            File fileBaseConfig = getConfigPath(NucleusUtils.class, null, false);

            if ((fileBaseConfig != null) && fileBaseConfig.exists()) {
                configpath = configpath + File.pathSeparator +
                        fileBaseConfig.getAbsolutePath();
            }


            // add the additional config path as the last arg, if needed
            File fileTestConfig = getConfigPath(
                    startupOptions.getClassRelativeTo(), startupOptions.getBaseConfigDirectory(), false
            );

            // now add it to the end of our config path
            if ((fileTestConfig != null) && fileTestConfig.exists()) {
                configpath = configpath + File.pathSeparator +
                        fileTestConfig.getAbsolutePath();
            }
            else if (fileTestConfig != null) {
                logger.error(
                        "Warning: did not find directory {}", fileTestConfig.getAbsolutePath()
                );
            }
            String dynUnitHome = DynUnit.getHome();
            if (dynUnitHome != null) {
                configpath = configpath
                        + File.pathSeparator
                        + dynUnitHome
                        + File.separatorChar
                        + "licenseconfig";
            }
            // finally, create a server dir.
            fileServerDir = createTempServerDir();

            setSystemProperty(
                    "atg.dynamo.server.home", fileServerDir.getAbsolutePath()
            );
            NucleusServlet.addNamingFactoriesAndProtocolHandlers();

            ArrayList<String> listArgs = new ArrayList<String>();
            listArgs.add(configpath);
            listArgs.add("-initialService");
            listArgs.add(startupOptions.getInitialService());

            PropertyEditors.registerEditors();
            logger.info("Starting nucleus with arguments: " + listArgs);
            Nucleus n = Nucleus.startNucleus(listArgs.toArray(new String[listArgs.size()]));

            // remember our temporary server directory for later deletion
            nucleiConfigPathsCache.put(n, fileServerDir);
            // clear out the variable, so our finally clause knows not to
            // delete it
            fileServerDir = null;

            return n;
        } catch (AppLauncherException e) {
            throw logger.throwing(new ServletException(e));
        } catch (IOException e) {
            throw logger.throwing(new ServletException(e));
        } finally {
            if (fileServerDir != null) {
                try {
                    // a non-null value means it was created, but not added to our list,
                    // so we should nuke it.
                    FileUtils.deleteDirectory(fileServerDir);
                } catch (IOException e) {
                    logger.catching(Level.ERROR, e);
                }
            }
        }
    }

    /**
     * A crazily ugly and elaborate method where we try to discover
     * DYNAMO_ROOT by various means. This is mostly made complicated
     * by the ROAD DUST environment being so different from devtools.
     */
    private static String findDynamoRoot() {
        // now let's try to find dynamo home...
        String dynamoRootStr = DynamoEnv.getProperty("atg.dynamo.root");


        if (dynamoRootStr == null) {
            // let's try to look at an environment variable, just to
            // see....
            dynamoRootStr = CommandProcessor.getProcEnvironmentVar("DYNAMO_ROOT");
        }

        if (dynamoRootStr == null) {
            // try dynamo home
            String dynamoHomeStr = DynamoEnv.getProperty("atg.dynamo.home");
            if (StringUtils.isEmpty(dynamoHomeStr)) {
                dynamoHomeStr = null;
            }

            if (dynamoHomeStr == null) {
                dynamoHomeStr = CommandProcessor.getProcEnvironmentVar("DYNAMO_HOME");

                if (StringUtils.isEmpty(dynamoHomeStr)) {
                    dynamoHomeStr = null;
                }

                if (dynamoHomeStr != null) {
                    // make sure home is set as a property
                    DynamoEnv.setProperty("atg.dynamo.home", dynamoHomeStr);
                }
            }

            if (dynamoHomeStr != null) {
                dynamoRootStr = dynamoHomeStr.trim() + File.separator + "..";
            }
        }

        if (dynamoRootStr == null) {
            // okay, start searching upwards for something that looks like
            // a dynamo directory, which should be the case for devtools
            File currentDir = new File(new File(".").getAbsolutePath());
            String strDynamoHomeLocalConfig = "Dynamo"
                    + File.separator
                    + "home"
                    + File.separator
                    + "localconfig";

            while (currentDir != null) {
                File filePotentialHomeLocalconfigDir = new File(
                        currentDir, strDynamoHomeLocalConfig
                );
                if (filePotentialHomeLocalconfigDir.exists()) {
                    dynamoRootStr = new File(currentDir, "Dynamo").getAbsolutePath();
                    logger.debug("Found dynamo root via parent directory: " + dynamoRootStr);
                    break;
                }
                currentDir = currentDir.getParentFile();
            }
        }

        if (dynamoRootStr == null) {
            // okay, we are not devtools-ish, so let's try using our ClassLoader
            // to figure things out.

            URL urlClass = NucleusUtils.class.getClassLoader()
                                             .getResource("atg/nucleus/Nucleus.class");

            // okay... this should be jar URL...
            if ((urlClass != null) && "jar".equals(urlClass.getProtocol())) {
                String strFile = urlClass.getFile();
                int separator = strFile.indexOf('!');
                strFile = strFile.substring(0, separator);

                File fileCur = null;
                try {
                    fileCur = urlToFile(new URL(strFile));
                } catch (MalformedURLException e) {
                    // ignore
                }

                if (fileCur != null) {
                    String strSubPath = "DAS/taglib/dspjspTaglib/1.0".replace(
                            '/', File.separatorChar
                    );
                    while ((fileCur != null) && fileCur.exists()) {
                        if (new File(fileCur, strSubPath).exists()) {
                            dynamoRootStr = fileCur.getAbsolutePath();
                            logger.debug(
                                    "Found dynamo root by Nucleus.class location: " + dynamoRootStr
                            );


                            break;
                        }
                        fileCur = fileCur.getParentFile();
                    }
                }
            }
        }

        return dynamoRootStr;
    }

    @Deprecated
    private static File urlToFile(URL url) {
        return toFile(url);
    }

    /**
     * Create a temporary, empty server directory. This is to satisfy
     * Dynamo's need to have a server directory, yet not conflict if
     * multiple tests are running at the same time against the same Dynamo
     * instance. The directory name is generated by File.createTempFile.
     *
     * @return the created temporary server directory.
     *
     * @throws IOException
     *         if an error occurs
     */
    private static File createTempServerDir()
            throws IOException {
        File fileTemp = File.createTempFile("tempServer", "dir");
        fileTemp.delete();
        if (!fileTemp.mkdir()) {
            throw new IOException(
                    "Unable to create directory " + fileTemp.getAbsolutePath()
            );
        }
        for (String strSubDir : ServerConfig.smConfigFileDirs) {
            File fileSubDir = new File(fileTemp, strSubDir);
            if (!fileSubDir.mkdirs()) {
                throw new IOException(
                        "Unable to create directory " + fileSubDir.getAbsolutePath()
                );
            }
        }
        return fileTemp;
    }

    /**
     * This method starts nucleus with a config path calculated from the
     * specified list of Dynamo modules ("DAS", "DPS", "DSS",
     * "Publishing.base", etc). Additionally adds a directory calculated relative
     * to the location of classRelativeTo's package name from the classloader.
     * The added config directory is calculated as (in pseudo-code):
     * <code>
     * (classRelativeTo's package location) + "/data/" +  (baseConfigDirectory or "config")
     * </code>
     * and is only added if the directory exists. <P>
     * <p/>
     * You must specify a <code>initialService</code> parameter, which
     * will be the initial service started by Nucleus (rather than the
     * normally Initial component, which would do a full Nucleus component
     * start-up). <P>
     * <p/>
     * This method also creates a temporary server directory, which is deleted
     * when stopNucleus in invoked on the returned directory. <P>
     * <p/>
     * Note: If you need to start up a complete ATG instance, you should
     * use DUST rather than a unit test. <P>
     * <p/>
     * Note: You may also wish to use a {@see
     * atg.nucleus.ConfigCreationFilter}. You can either set a value for
     * Nucleus.CREATION_FILTER_CLASS_PROPERTY_NAME
     * ("atg.nucleus.Nucleus.creationFilterClass") as a DynamoEnv or System
     * property, or set the creationFilter property in Nucleus.properties in
     * your configuration. This allows on to block creation of referenced
     * components without having to make additional properties file changes.
     * <p/>
     * Note 3: Nucleus's classReplacementMap can also be useful for replacing
     * a component instance with a subclass.
     *
     * @param modules
     *         the list of modules to use to calculate the
     *         Nucleus configuration path.
     * @param classRelativeTo
     *         the class whose package the config/data
     *         (or baseConfigDirectory/data) should be relative in.
     * @param baseConfigDirectory
     *         the base configuration directory. If
     *         this parameter is non-null, the relative configuration
     *         subdirectory
     *         will be
     *         ("data/" + baseConfigDirectory) rather than "data/config".
     * @param initialService
     *         the nucleus path of the Nucleus component
     *         to start-up. This is a required property to prevent accidental
     *         full start-up.
     *
     * @return the started Nucleus instance that should later be shut down
     * with the stopNucleus method.
     *
     * @throws ServletException
     *         if an error occurs
     */
    public static Nucleus startNucleusWithModules(String[] modules,
                                                  Class classRelativeTo,
                                                  String baseConfigDirectory,
                                                  String initialService)
            throws ServletException, FileNotFoundException {
        return startNucleusWithModules(
                new NucleusStartupOptions(
                        modules, classRelativeTo, baseConfigDirectory, initialService
                )
        );
    }

    /**
     * Shutdown the specified Nucleus and try to delete the associated
     * temporary server directory. Typically used on a Nucleus created
     * by startNucleusWithModules.
     *
     * @param nucleus
     *         the nucleus instance to shut down.
     *
     * @throws ServiceException
     *         if an error occurs
     * @throws IOException
     *         if an error occurs (such as a failure
     *         to remove the temporary server directory).
     */
    public static void stopNucleus(Nucleus nucleus)
            throws IOException, ServiceException {
        if (nucleus.isRunning()) {
            try {
                nucleus.stopService();
            } catch (ServiceException e) {
                throw logger.throwing(e);
            } finally {
                cleanUpNucleusTemporaryFiles(nucleus);
            }
        }
    }

    private static void cleanUpNucleusTemporaryFiles(final Nucleus nucleus)
            throws IOException {
        final File temporaryFilesDirectory = nucleiConfigPathsCache.get(nucleus);
        if (temporaryFilesDirectory == null) {
            return;
        }
        try {
            FileUtils.deleteDirectory(temporaryFilesDirectory);
        } finally {
            nucleiConfigPathsCache.remove(nucleus);
        }
    }

    /**
     * This method returns a free port number on the current machine. There is
     * some chance that the port number could be taken by the time the caller
     * actually gets around to using it.
     * <p/>
     * This method returns -9999 if it's not able to find a port.
     */
    public static int findFreePort() {
        ServerSocket socket = null;
        int freePort = -9999;
        try {
            socket = new ServerSocket(0);
            freePort = socket.getLocalPort();
        } catch (IOException e) {
            logger.catching(e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.catching(e);
            }
        }
        return freePort;
    }

    /**
     * A class representing NucleusStartupOptions, used by
     * startNucleusWithModules().
     */
    public static class NucleusStartupOptions {

        /**
         * List of dynamo modules.
         */
        private final String[] modules;
        /**
         * Class whose package data subdir is relative to.
         */
        private final Class<?> classRelativeTo;
        /**
         * The base config directory, relative to classRelativeTo's package
         * + "/data". If null, then "config"
         */
        private final String baseConfigDirectory;
        /**
         * The Nucleus path of the initial service to resolve.
         */
        private final String initialService;
        private String[] layers;
        private boolean liveConfig;

        /**
         * This constructor creates NucleusStartupOptions with the
         * specified list of Dynamo modules ("DAS", "DPS", "DSS",
         * "Publishing.base", etc).
         * Additionally sets opts to add a directory calculated relative
         * to the location of classRelativeTo's package name from the classloader.
         * The added config directory is calculated as (in psuedocode):
         * <code>
         * (classRelativeTo's package location) + "/data/" +  (classRelativeTo's simpleClassName)
         * +
         * "/config"
         * </code>
         * and is only added if the directory exists. <P>
         * <p/>
         * You must specify a <code>initialService</code> parameter, which
         * will be the initial service started by Nucleus (rather than the
         * normally Initial component, which would do a full Nucleus component
         * start-up). <P>
         *
         * @param modules
         *         the list of modules to use to calculate the
         *         Nucleus configuration path.
         * @param classRelativeTo
         *         the class whose name and package
         *         will be used for the {packageName}/config/{simpleClassName}/data
         *         directory
         * @param initialService
         *         the nucleus path of the Nucleus component
         *         to start-up. This is a required property to prevent accidental
         *         full start-up.
         */
        @ConstructorProperties({ "modules", "classRelativeTo", "initialServices" })
        public NucleusStartupOptions(String[] modules,
                                     Class<?> classRelativeTo,
                                     String initialService) {


            this.modules = modules;
            this.classRelativeTo = classRelativeTo;
            this.initialService = initialService;
            baseConfigDirectory = classRelativeTo.getSimpleName() + File.separatorChar + "config";
        }

        /**
         * This constructor creates NucleusStartupOptions with the
         * specified list of Dynamo modules ("DAS", "DPS", "DSS",
         * "Publishing.base", etc).
         * Additionally sets opts to add a directory calculated relative
         * to the location of classRelativeTo's package name from the classloader.
         * The added config directory is calculated as (in pseudo-code):
         * <code>
         * (classRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
         * </code>
         * and is only added if the directory exists. <P>
         * <p/>
         * You must specify a <code>initialService</code> parameter, which
         * will be the initial service started by Nucleus (rather than the
         * normally Initial component, which would do a full Nucleus component
         * start-up). <P>
         *
         * @param modules
         *         the list of modules to use to calculate the
         *         Nucleus configuration path.
         * @param classRelativeTo
         *         the class whose package the config/data
         *         (or baseConfigDirectory/data) should be relative in.
         * @param baseConfigDirectory
         *         the base configuration directory. If
         *         this parameter is non-null, the relative configuration
         *         subdirectory will be
         *         ("data/" + baseConfigDirectory) rather than "data/config".
         * @param initialService
         *         the nucleus path of the Nucleus component
         *         to start-up. This is a required property to prevent
         *         accidental full start-up.
         */
        @ConstructorProperties({ "modules", "classRelativeTo", "baseConfigDirectory", "initialService" })
        public NucleusStartupOptions(String[] modules,
                                     Class classRelativeTo,
                                     String baseConfigDirectory,
                                     String initialService) {

            this.modules = modules;
            this.classRelativeTo = classRelativeTo;
            this.initialService = initialService;
            this.baseConfigDirectory = baseConfigDirectory;
        }

        /**
         * Return the list of modules for starting Nucleus. These modules
         * are the modules whose config path will be included.
         */
        public String[] getModules() {
            return modules;
        }

        //-------------------------------------
        // property: baseConfigDirectory

        /**
         * Return the "class relative to" property. This is the Class whose
         * package the config directory will be relative to.
         */
        public Class getClassRelativeTo() {
            return classRelativeTo;
        }


        //-------------------------------------
        // property: layers

        /**
         * Gets the initialService. This is the InitialService for Nucleus
         * to resolve at start-up. Required.
         */
        public String getInitialService() {
            return initialService;
        }

        /**
         * Set the basic config directory. This is the directory that will be
         * tacked on to the package path of the classRelativeTo class. If this
         * property is non-null, the relative configuration subdirectory will
         * be ("data/" + baseConfigDirectory).
         */
        public String getBaseConfigDirectory() {
            return baseConfigDirectory;
        }

        /**
         * Returns the Dynamo layers to run with.
         */
        public String[] getLayers() {
            return layers;
        }

        /**
         * Sets the Dynamo layers to run with.
         */
        public void setLayers(String[] layers) {
            this.layers = layers;
        }

        //-------------------------------------
        // property: liveconfig

        /**
         * Return the layers as a string appropriate for passing to
         * DynamoServerLauncher, calculateConfigPath.
         *
         * @return null if layers is null. Otherwise returns a space delimited
         * list of layers.
         */
        public String getLayersAsString() {
            return StringUtils.join(layers, ' ');
        }

        /**
         * Returns property liveconfig.
         *
         * @return true if liveconfig should be set, false otherwise.
         */
        public boolean getLiveConfig() {
            return liveConfig;
        }

        /**
         * Sets property liveconfig.
         *
         * @param liveConfig
         *         true if Nucleus should be started in liveconfig
         *         mode, false otherwise.
         */
        public void setLiveConfig(boolean liveConfig) {
            this.liveConfig = liveConfig;
        }

        /**
         * Modify Nucleus command-line options, as needed. This will
         * be invoked just before Nucleus is started as a final
         * chance to adjust any command-line options.
         */
        public void modifyNucleusCommandLineOptions(List<String> listArgs) {
        }
    }

}
