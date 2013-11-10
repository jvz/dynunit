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

package atg.nucleus;

import atg.applauncher.AppLauncher;
import atg.applauncher.AppLauncherException;
import atg.applauncher.AppModuleManager;
import atg.applauncher.MultiInstallLocalAppModuleManager;
import atg.applauncher.dynamo.DynamoServerLauncher;
import atg.core.io.FileUtils;
import atg.core.util.CommandProcessor;
import atg.core.util.JarUtils;
import atg.core.util.StringUtils;
import atg.nucleus.naming.ComponentName;
import atg.nucleus.servlet.NucleusServlet;
import atg.service.dynamo.ServerConfig;
import atg.test.util.DustStringUtils;
import atg.test.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * NucleusTestUtils
 *
 * @author adamb
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/nucleus/NucleusTestUtils.java#21 $
 *          <p/>
 *          This class contains some utility methods to make it faster
 *          to write a unit test that needs to resolve componants against Nucleus.
 */
public class NucleusTestUtils {

    //-------------------------------------

    private static final Logger log = LogManager.getLogger();

    public static final String ATG_DUST_TESTCONFIG = "atg.dust.testconfig";

    private static final String EXTRACT_TEMP_JAR_FILE_FOR_PATH = "Extract temp jar file for path ";

    private static final String ATG_DUST_CONFIG = "atg-dust-config-";

    private static final String FILE = "file:";

    private static final String ATG_DUST_TESTCONFIG_ENV = "ATG_DUST_TESTCONFIG";

    /**
     * Class version string
     */

    public static String CLASS_VERSION = "$Id: //test/UnitTests/base/main/src/Java/atg/nucleus/NucleusTestUtils.java#21 $$Change: 556195 $";

    /**
     * Whether or not to remove tempoary ATG server directories
     * created by startNucleusWithModules(). True by default, but
     * can be set to false for debugging.
     */
    private static boolean sRemoveTempAtgServerDirectories = true;

    /**
     * A map from Nucleus instance to temporary directory. Used by
     * startNucleusWithModules.
     */
    private static Map<Nucleus, File> sNucleusToTempAtgServerDirectory = Collections.synchronizedMap(new HashMap<Nucleus, File>());

    /**
     * Cache of the config path for a given Class. Used by getConfigpath.
     */
    private static Map<Class, Map<String, File>> sConfigDir = new HashMap<Class, Map<String, File>>();

    /**
     * Creates an Initial.properties file
     * pRoot The root directory of the configpath
     * pInitialServices A list of initial services
     *
     * @param pRoot            The root of the config path entry.
     * @param pInitialServices initial services list
     *
     * @return the create initial services properties file.
     * @throws IOException if an error occurs
     */
    public static File createInitial(File pRoot, List pInitialServices)
            throws IOException {
        Properties prop = new Properties();
        Iterator iter = pInitialServices.iterator();
        StringBuffer services = new StringBuffer();
        while ( iter.hasNext() ) {
            if ( services.length() != 0 ) {
                services.append(",");
            }
            services.append((String) iter.next());
        }
        prop.put("initialServices", services.toString());
        return NucleusTestUtils.createProperties(
                "Initial", new File(pRoot.getAbsolutePath()), "atg.nucleus.InitialService", prop
        );
    }
    // ---------------------

    /**
     * Creates a .properties file
     *
     * @param pComponentName Name of the component
     * @param pConfigDir     Name of the configuration directory. If null,
     *                       will add to the current working directory.
     * @param pClass         The class of the component (used for $class property).
     * @param pProps         Other properties of the component.
     *
     * @return The created file.
     * @throws IOException
     */
    public static File createProperties(String pComponentName,
                                        File pConfigDir,
                                        String pClass,
                                        Properties pProps)
            throws IOException {
        File prop;
        if ( pConfigDir == null ) {
            prop = new File("./" + pComponentName + ".properties");
        } else {
            pConfigDir.mkdirs();
            prop = new File(pConfigDir, pComponentName + ".properties");
            new File(prop.getParent()).mkdirs();
        }

        if ( prop.exists() ) {
            prop.delete();
        }
        prop.createNewFile();
        FileWriter fw = new FileWriter(prop);
        String classLine = "$class=" + pClass + "\n";
        try {
            if ( pClass != null ) {
                fw.write(classLine);
            }
            if ( pProps != null ) {
                Iterator iter = pProps.keySet().iterator();
                while ( iter.hasNext() ) {
                    String key = (String) iter.next();
                    String thisLine = key + "=" + StringUtils.replace(
                            pProps.getProperty(key), '\\', "\\\\"
                    ) + "\n";
                    fw.write(thisLine);
                }
            }
        } finally {
            fw.flush();
            fw.close();
        }
        return prop;
    }

    /**
     * Allows the absoluteName of the given service to be explicitly defined.
     * Normally this is determined by the object's location in the Nucleus
     * hierarchy.  For test items that are not really bound to Nucleus, it's
     * convenient to just give it an absolute name rather than going through
     * the whole configuration and binding process.
     *
     * @param pName    The absolute name value to set
     * @param pService The service whose absolute nameshould be set.
     */
    public static void setAbsoluteName(String pName, GenericService pService) {
        pService.mAbsoluteName = pName;
    }

    /**
     * Adds the given object, pComponent to Nucleus, pNucleus at the path given
     * by pComponentPath.
     *
     * @param pNucleus       The Nucleus instance to which the component should be added
     * @param pComponentPath the component path at which the component should be added
     * @param pComponent     the component instance to add
     */
    public static void addComponent(Nucleus pNucleus, String pComponentPath, Object pComponent) {
        // make sure it's not already there
        if ( pNucleus.resolveName(pComponentPath) != null ) {
            return;
        }
        ComponentName name = ComponentName.getComponentName(pComponentPath);
        ComponentName[] subNames = name.getSubNames();
        GenericContext[] contexts = new GenericContext[subNames.length - 1];
        contexts[0] = pNucleus;
        for ( int i = 1; i < subNames.length - 1; i++ ) {
            contexts[i] = new GenericContext();
            // Make sure it's not there
            GenericContext tmpContext = (GenericContext) contexts[i
                                                                  - 1].getElement(subNames[i].getName());
            if ( tmpContext == null ) {
                contexts[i - 1].putElement(subNames[i].getName(), contexts[i]);
            } else {
                contexts[i] = tmpContext;
            }
        }
        contexts[contexts.length - 1].putElement(
                subNames[subNames.length - 1].getName(), pComponent
        );
    }

    /**
     * Starts Nucleus using the given config directory
     *
     * @param configpath the config path directory entry
     *                   to use as the entire config path.
     *
     * @return the started Nucleus
     */
    public static Nucleus startNucleus(File configpath) {
        return startNucleus(configpath.getAbsolutePath());
    }

    /**
     * Starts Nucleus using the given config directory
     *
     * @param pSingleConfigpathEntry the path name of the config path
     *                               entry to specify.
     *
     * @return The started nucleus.
     */
    public static Nucleus startNucleus(String pSingleConfigpathEntry) {
        System.setProperty("atg.dynamo.license.read", "true");
        System.setProperty("atg.license.read", "true");
        NucleusServlet.addNamingFactoriesAndProtocolHandlers();
        return Nucleus.startNucleus(new String[] { pSingleConfigpathEntry });
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the "config/data" subdirectory of the passed in file.
     *
     * @param pBaseConfigDirectory the base configuration directory.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigpath(String pBaseConfigDirectory) {
        return getConfigpath(NucleusTestUtils.class, pBaseConfigDirectory, true);
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the pBaseConfigDirectory (or pBaseConfigDirectory +
     * "data") subdirectory of the the passed in class's location.<P>
     * <p/>
     * The directory location is calculated as (in psuedocode):
     * <code>
     * (pClassRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
     * </code>
     *
     * @param pClassRelativeTo     the class whose package the config/data
     *                             (or pBaseConfigDirectory/data) should be relative in.
     * @param pBaseConfigDirectory the base configuration directory If null,
     *                             uses "config".
     * @param pCreate              whether to create the config/data subdirectory if
     *                             it does not exist.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigpath(Class pClassRelativeTo,
                                     String pBaseConfigDirectory,
                                     boolean pCreate) {
        //System.out.println("getConfigpath(" +
        //                   pClassRelativeTo + ", " +
        //                   pBaseConfigDirectory + "," + pCreate + ")");
        Map<String, File> baseConfigToFile = sConfigDir.get(pClassRelativeTo);
        if ( baseConfigToFile == null ) {
            baseConfigToFile = new HashMap<String, File>();
            sConfigDir.put(pClassRelativeTo, baseConfigToFile);
        }

        File fileFound = baseConfigToFile.get(pBaseConfigDirectory);

        if ( fileFound == null ) {
            String configdirname = "config";
            String packageName = StringUtils.replace(
                    pClassRelativeTo.getPackage().getName(), '.', "/"
            );
            if ( pBaseConfigDirectory != null ) {
                configdirname = pBaseConfigDirectory;
            }

            String configFolder = packageName + "/data/" + configdirname;
            URL dataURL = pClassRelativeTo.getClassLoader().getResource(configFolder);

            // Mkdir
            if ( dataURL == null ) {
                URL root = pClassRelativeTo.getClassLoader().getResource(packageName);

                File f = new File(root.getFile());
                File f2 = new File(f, "/data/" + configdirname);
                if ( pCreate ) {
                    f2.mkdirs();
                }
                dataURL = NucleusTestUtils.class.getClassLoader().getResource(configFolder);
                if ( dataURL == null ) {
                    System.err.println(
                            "Warning: Could not find resource \"" +
                            configFolder + "\" in CLASSPATH"
                    );
                }
            }
            if ( dataURL != null ) {// check if this URL is contained within a jar file
                // if so, extract to a temp dir, otherwise just return
                // the directory
                fileFound = extractJarDataURL(dataURL);
                baseConfigToFile.put(pBaseConfigDirectory, fileFound);
            }
        }
        if ( fileFound != null ) {
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
     * a new File object initialied with <code>dataURL.getFile()</code>.
     *
     * @return A temporary directory to be used as a configdir
     */
    private static File extractJarDataURL(URL dataURL) {
        // TODO: Extract to a temp location
        // atg.core.util.JarUtils.extractEntry(arg0, arg1, arg2)
        int endIndex = dataURL.getFile().lastIndexOf('!');
        if ( endIndex == -1 ) {
            // Not a jar file url
            return new File(dataURL.getFile());
        }
        log.info(EXTRACT_TEMP_JAR_FILE_FOR_PATH + dataURL.getFile());
        File configDir = null;
        try {
            File tmpFile = File.createTempFile("atg-dust" + System.currentTimeMillis(), ".tmp");
            tmpFile.deleteOnExit();
            final File tmpDir = new File(
                    tmpFile.getParentFile(), ATG_DUST_CONFIG + System.currentTimeMillis()
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
            // Add a shutdown hook to delete this temp directory
            FileUtil.deleteDirectoryOnShutdown(tmpDir);
            configDir = new File(tmpDir, relativePath);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return configDir;
    }

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the "data" subdirectory of the passed in file.
     *
     * @param pBaseConfigDirectory the base configuration directory.
     * @param pCreate              whether to create the config/data subdirectory if
     *                             it does not exist.
     *
     * @return The calculated configuration path.
     */
    public static File getConfigpath(String pBaseConfigDirectory, boolean pCreate) {
        return getConfigpath(NucleusTestUtils.class, pBaseConfigDirectory, pCreate);
    }


    // ------------------------------------

    /**
     * A convenience method for returning the configpath for a test.
     * pConfigDirectory is the top level name to be used for the configpath.
     * Returns a file in the pBaseConfigDirectory (or pBaseConfigDirectory +
     * "data") subdirectory of the the passed in class's location.
     * <p/>
     * The directory location is calculated as (in psuedocode): <code>
     * (pClassRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
     * </code>
     * This method always creates the config/data subdirectory if it does not
     * exist.
     *
     * @param pClassRelativeTo     the class whose package the config/data (or
     *                             pBaseConfigDirectory/data) should be relative in.
     * @param pBaseConfigDirectory the base configuration directory If null, uses "config".
     *
     * @return The calculated configuration path.
     */

    public static File getConfigpath(Class pClassRelativeTo, String pBaseConfigDirectory) {
        return getConfigpath(pClassRelativeTo, pBaseConfigDirectory, true);
    }

    /**
     * Look up the global testconfig path.
     * This path is specified in either an
     *
     * @return
     */
    public static String getGlobalTestConfig() {
        // First Check System Property
        String config = System.getProperty(ATG_DUST_TESTCONFIG);
        // If NULL check environment variable
        if ( config == null ) {
            config = System.getenv(ATG_DUST_TESTCONFIG_ENV);
        }
        // If that's null, there is no global test config specified
        return config;
    }

    /**
     * This method starts nucleus with a config path calculated from the
     * specified list of Dynamo modules ("DAS", "DPS", "DSS",
     * "Publishing.base", etc). Additionally adds a directory calculated relative
     * to the location of pClassRelativeTo's package name from the classloader.
     * The added config directory is calculated as (in psuedocode):
     * <code>
     * (pClassRelativeTo's package location) + "/data/" +  (pClassRelativeTo's simpleClassName) +
     * "/config"
     * </code>
     * and is only added if the directory exists. <P>
     * <p/>
     * You must specify a <code>pInitialService</code> parameter, which
     * will be the initial service started by Nucleus (rather than the
     * normally Initial component, which would do a full Nucleus component
     * start-up). <P>
     * <p/>
     * This method also creates a temporary server directory, which is deleted
     * when shutdownNucleus in invoked on the returned directory. <P>
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
     * @param pModules         the list of modules to use to calculate the
     *                         Nucleus configuration path.
     * @param pClassRelativeTo the class whose name and package
     *                         will be used for the {packageName}/config/{ClassName}/data directory
     * @param pInitialService  the nucleus path of the Nucleus component
     *                         to start-up. This is a required property to prevent accidental
     *                         full start-up.
     *
     * @return the started Nucleus instance that should later be shut down
     *         with the shutdownNucleus method.
     * @throws ServletException if an error occurs
     */
    public static Nucleus startNucleusWithModules(String[] pModules,
                                                  Class pClassRelativeTo,
                                                  String pInitialService)
            throws ServletException {
        return startNucleusWithModules(
                new NucleusStartupOptions(
                        pModules,
                        pClassRelativeTo,
                        pClassRelativeTo.getSimpleName() + "/config",
                        pInitialService
                )
        );
    }

    /**
     * This method starts nucleus with a config path calculated from the
     * specified list of Dynamo modules ("DAS", "DPS", "DSS",
     * "Publishing.base", etc). Additionally adds a directory calculated relative
     * to the location of pClassRelativeTo's package name from the classloader.
     * The added config directory is calculated as (in psuedocode):
     * <code>
     * (pClassRelativeTo's package location) + "/data/" +  (pBaseConfigDirectory or "config")
     * </code>
     * and is only added if the directory exists. <P>
     * <p/>
     * You must specify a <code>pInitialService</code> parameter, which
     * will be the initial service started by Nucleus (rather than the
     * normally Initial component, which would do a full Nucleus component
     * start-up). <P>
     * <p/>
     * This method also creates a temporary server directory, which is deleted
     * when shutdownNucleus in invoked on the returned directory. <P>
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
     * <p/>
     * See NucleusStartupOptions for the effects of individual properties
     * of pOptions.
     *
     * @param pOptions the startup Options for Nucleus.
     *
     * @return the started Nucleus instance that should later be shut down
     *         with the shutdownNucleus method.
     * @throws ServletException if an error occurs
     */
    public static Nucleus startNucleusWithModules(NucleusStartupOptions pOptions)
            throws ServletException {

        if ( pOptions.getInitialService() == null ) {
            throw new IllegalArgumentException("Initial service must be specified.");
        }

        // now let's try to find dynamo home...
        String dynamoRootStr = findDynamoRoot();

        if ( dynamoRootStr == null ) {
            throw new ServletException("Could not find dynamo root.");
        }

        if ( !new File(dynamoRootStr).exists() ) {
            throw new ServletException(
                    "Could not find dynamo root at " +
                    dynamoRootStr + " because directory does not exist."
            );
        }

        if ( DynamoEnv.getProperty("atg.dynamo.root") == null ) {
            // make sure root is set as a property
            DynamoEnv.setProperty("atg.dynamo.root", dynamoRootStr);
        }

        if ( DynamoEnv.getProperty("atg.dynamo.home") == null ) {
            // make sure home is set as a property
            DynamoEnv.setProperty(
                    "atg.dynamo.home", dynamoRootStr + File.separator + "home"
            );
        }


        File dynamoRootFile = new File(dynamoRootStr);

        String strModulesString = DustStringUtils.joinStringsWithQuoting(
                pOptions.getModules(), File.pathSeparatorChar
        );

        DynamoEnv.setProperty("atg.dynamo.modules", strModulesString);


        // our temporary server directory.
        File fileServerDir = null;

        try {

            AppModuleManager modMgr = new MultiInstallLocalAppModuleManager(
                    dynamoRootStr, dynamoRootFile, strModulesString
            );

            AppLauncher launcher = AppLauncher.getLauncher(modMgr, strModulesString);

            // Start Nucleus
            String configpath = DynamoServerLauncher.calculateConfigPath(
                    launcher, pOptions.getLiveconfig(), pOptions.getLayersAsString(), false, null
            );

            // use the NucleusTestUtils config dir as a base, since it
            // empties out license checks, etc.
            File fileBaseConfig = getConfigpath(NucleusTestUtils.class, null, false);

            if ( (fileBaseConfig != null) && fileBaseConfig.exists() ) {
                configpath = configpath + File.pathSeparator +
                             fileBaseConfig.getAbsolutePath();
            }


            // add the additional config path as the last arg, if needed
            File fileTestConfig = getConfigpath(
                    pOptions.getClassRelativeTo(), pOptions.getBaseConfigDirectory(), false
            );

            // now add it to the end of our config path
            if ( (fileTestConfig != null) && fileTestConfig.exists() ) {
                configpath = configpath + File.pathSeparator +
                             fileTestConfig.getAbsolutePath();
            } else if ( fileTestConfig != null ) {
                log.error(
                        "Warning: did not find directory " + fileTestConfig.getAbsolutePath()
                );
            }
            String dustHome = System.getenv("DUST_HOME");
            if ( dustHome != null ) {
                configpath = configpath
                             + File.pathSeparator
                             + dustHome
                             + File.separatorChar
                             + "licenseconfig";
            } else {
                log.warn(
                        "The DUST_HOME environment variable is not set."
                        + " License files (if needed) should be placed in $DUST_HOME/licenseconfig."
                );
            }
            // finally, create a server dir.
            fileServerDir = createTempServerDir();

            System.setProperty(
                    "atg.dynamo.server.home", fileServerDir.getAbsolutePath()
            );
            System.setProperty("atg.dynamo.license.read", "true");
            System.setProperty("atg.license.read", "true");
            NucleusServlet.addNamingFactoriesAndProtocolHandlers();

            ArrayList<String> listArgs = new ArrayList<String>();
            listArgs.add(configpath);
            listArgs.add("-initialService");
            listArgs.add(pOptions.getInitialService());

            PropertyEditors.registerEditors();
            log.info("Starting nucleus with arguments: " + listArgs);
            Nucleus n = Nucleus.startNucleus(listArgs.toArray(new String[0]));

            // remember our temporary server directory for later deletion
            sNucleusToTempAtgServerDirectory.put(n, fileServerDir);
            // clear out the variable, so our finally clause knows not to
            // delete it
            fileServerDir = null;

            return n;
        } catch ( AppLauncherException e ) {
            throw new ServletException(e);
        } catch ( IOException e ) {
            throw new ServletException(e);
        } finally {
            if ( (fileServerDir != null) && sRemoveTempAtgServerDirectories ) {
                try {
                    // a non-null value means it was created, but not added to our list,
                    // so we should nuke it.
                    FileUtils.deleteDir(fileServerDir.getAbsolutePath());
                } catch ( IOException e ) {
                    // we shouldn't rethrow here, since we might block
                    // the exception in the main clause, so we'll do the bad
                    // thing and print the stack trace and swallow the exception
                    e.printStackTrace();
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


        if ( dynamoRootStr == null ) {
            // let's try to look at an environment variable, just to
            // see....
            dynamoRootStr = CommandProcessor.getProcEnvironmentVar("DYNAMO_ROOT");
        }

        if ( dynamoRootStr == null ) {
            // try dynamo home
            String dynamoHomeStr = DynamoEnv.getProperty("atg.dynamo.home");
            if ( StringUtils.isEmpty(dynamoHomeStr) ) {
                dynamoHomeStr = null;
            }

            if ( dynamoHomeStr == null ) {
                dynamoHomeStr = CommandProcessor.getProcEnvironmentVar("DYNAMO_HOME");

                if ( StringUtils.isEmpty(dynamoHomeStr) ) {
                    dynamoHomeStr = null;
                }

                if ( dynamoHomeStr != null ) {
                    // make sure home is set as a property
                    DynamoEnv.setProperty("atg.dynamo.home", dynamoHomeStr);
                }
            }

            if ( dynamoHomeStr != null ) {
                dynamoRootStr = dynamoHomeStr.trim() + File.separator + "..";
            }
        }

        if ( dynamoRootStr == null ) {
            // okay, start searching upwards for something that looks like
            // a dynamo directory, which should be the case for devtools
            File currentDir = new File(new File(".").getAbsolutePath());
            String strDynamoHomeLocalConfig = "Dynamo"
                                              + File.separator
                                              + "home"
                                              + File.separator
                                              + "localconfig";

            while ( currentDir != null ) {
                File filePotentialHomeLocalconfigDir = new File(
                        currentDir, strDynamoHomeLocalConfig
                );
                if ( filePotentialHomeLocalconfigDir.exists() ) {
                    dynamoRootStr = new File(currentDir, "Dynamo").getAbsolutePath();
                    log.debug("Found dynamo root via parent directory: " + dynamoRootStr);
                    break;
                }
                currentDir = currentDir.getParentFile();
            }
        }

        if ( dynamoRootStr == null ) {
            // okay, we are not devtools-ish, so let's try using our ClassLoader
            // to figure things out.

            URL urlClass = NucleusTestUtils.class.getClassLoader()
                                                 .getResource("atg/nucleus/Nucleus.class");

            // okay... this should be jar URL...
            if ( (urlClass != null) && "jar".equals(urlClass.getProtocol()) ) {
                String strFile = urlClass.getFile();
                int separator = strFile.indexOf('!');
                strFile = strFile.substring(0, separator);

                File fileCur = null;
                try {
                    fileCur = urlToFile(new URL(strFile));
                } catch ( MalformedURLException e ) {
                    // ignore
                }

                if ( fileCur != null ) {
                    String strSubPath = "DAS/taglib/dspjspTaglib/1.0".replace(
                            '/', File.separatorChar
                    );
                    while ( (fileCur != null) && fileCur.exists() ) {
                        if ( new File(fileCur, strSubPath).exists() ) {
                            dynamoRootStr = fileCur.getAbsolutePath();
                            log.debug(
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

    /**
     * Try to convert a file URL to a File object. You'd think this would be
     * easier, but no.
     */
    private static File urlToFile(URL url) {
        URI uri;

        if ( !"file".equals(url.getProtocol()) ) {
            throw new IllegalArgumentException("URL must be a file URL, got " + url);
        }

        try {
            // this is the step that can fail, and so
            // it should be this step that should be fixed
            uri = url.toURI();
        } catch ( URISyntaxException e ) {
            // OK if we are here, then obviously the URL did
            // not comply with RFC 2396. This can only
            // happen if we have illegal unescaped characters.
            // If we have one unescaped character, then
            // the only automated fix we can apply, is to assume
            // all characters are unescaped.
            // If we want to construct a URI from unescaped
            // characters, then we have to use the component
            // constructors:
            try {
                uri = new URI(
                        url.getProtocol(),
                        url.getUserInfo(),
                        url.getHost(),
                        url.getPort(),
                        url.getPath(),
                        url.getQuery(),
                        url.getRef()
                );
            } catch ( URISyntaxException e1 ) {
                // The URL is broken beyond automatic repair
                throw new IllegalArgumentException("broken URL: " + url);
            }
        }
        return new File(uri);
    }

    /**
     * Create a temporary, empty server directory. This is to satisfy
     * Dynamo's need to have a server directory, yet not conflict if
     * multiple tests are running at the same time against the same Dynamo
     * instance. The directory name is generated by File.createTempFile.
     *
     * @return the created temporary server directory.
     * @throws IOException if an error occurs
     */
    private static File createTempServerDir()
            throws IOException {
        File fileTemp = File.createTempFile("tempServer", "dir");
        fileTemp.delete();
        if ( !fileTemp.mkdir() ) {
            throw new IOException(
                    "Unable to create directory " + fileTemp.getAbsolutePath()
            );
        }
        for ( String strSubDir : ServerConfig.smConfigFileDirs ) {
            File fileSubDir = new File(fileTemp, strSubDir);
            if ( !fileSubDir.mkdirs() ) {
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
     * to the location of pClassRelativeTo's package name from the classloader.
     * The added config directory is calculated as (in psuedocode):
     * <code>
     * (pClassRelativeTo's package location) + "/data/" +  (pBaseConfigDirectory or "config")
     * </code>
     * and is only added if the directory exists. <P>
     * <p/>
     * You must specify a <code>pInitialService</code> parameter, which
     * will be the initial service started by Nucleus (rather than the
     * normally Initial component, which would do a full Nucleus component
     * start-up). <P>
     * <p/>
     * This method also creates a temporary server directory, which is deleted
     * when shutdownNucleus in invoked on the returned directory. <P>
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
     * @param pModules             the list of modules to use to calculate the
     *                             Nucleus configuration path.
     * @param pClassRelativeTo     the class whose package the config/data
     *                             (or pBaseConfigDirectory/data) should be relative in.
     * @param pBaseConfigDirectory the base configuration directory. If
     *                             this parameter is non-null, the relative configuration
     *                             subdirectory
     *                             will be
     *                             ("data/" + pBaseConfigDirectory) rather than "data/config".
     * @param pInitialService      the nucleus path of the Nucleus component
     *                             to start-up. This is a required property to prevent accidental
     *                             full start-up.
     *
     * @return the started Nucleus instance that should later be shut down
     *         with the shutdownNucleus method.
     * @throws ServletException if an error occurs
     */
    public static Nucleus startNucleusWithModules(String[] pModules,
                                                  Class pClassRelativeTo,
                                                  String pBaseConfigDirectory,
                                                  String pInitialService)
            throws ServletException {
        return startNucleusWithModules(
                new NucleusStartupOptions(
                        pModules, pClassRelativeTo, pBaseConfigDirectory, pInitialService
                )
        );
    }

    /**
     * Shutdown the specified Nucleus and try to delete the associated
     * temporary server directory. Typically used on a Nucleus created
     * by startNucleusWithModules.
     *
     * @param pNucleus the nucleus instance to shut down.
     *
     * @throws ServiceException if an error occurs
     * @throws IOException      if an error occurs (such as a failure
     *                          to remove the temporary server directory).
     */
    public static void shutdownNucleus(Nucleus pNucleus)
            throws ServiceException, IOException {
        boolean bComplete = false;
        try {
            if ( pNucleus.isRunning() ) {
                pNucleus.stopService();
            }
            bComplete = true;
        } finally {
            File fileTempAtgServDirectory = sNucleusToTempAtgServerDirectory.get(pNucleus);

            // try to delete the temp server directory.
            if ( sRemoveTempAtgServerDirectories &&
                 (fileTempAtgServDirectory != null) &&
                 fileTempAtgServDirectory.exists() ) {
                try {
                    FileUtils.deleteDir(fileTempAtgServDirectory.getAbsolutePath());
                } catch ( IOException e ) {
                    if ( bComplete ) {
                        // only throw if we if we finished our try clause, because
                        // otherwise we might block our initial exception
                        throw e;
                    }
                } finally {
                    sNucleusToTempAtgServerDirectory.remove(pNucleus);
                }
            }
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
        } catch ( IOException e ) {
            log.catching(e);
        } finally {
            try {
                if ( socket != null ) {
                    socket.close();
                }
            } catch ( IOException e ) {
                log.catching(e);
            }
        }
        return freePort;
    }

    //-------------------------------------------------------

    /**
     * A class representing NucleusStartupOptions, used by
     * startNucleusWithModules().
     */
    public static class NucleusStartupOptions {

        /**
         * List of dynamo modules.
         */
        private String[] mModules;

        /**
         * Class whose package data subdir is relative to.
         */
        private Class mClassRelativeTo;

        /**
         * The base config directory, realtive to mClassRelativeTo's package
         * + "/data". If null, then "config"
         */
        private String mBaseConfigDirectory;

        /**
         * The Nucleus path of the intial service to resolve.
         */
        private String mInitialService;

        private String[] mLayers;

        private boolean mLiveconfig;

        /**
         * This constructor creates NucleusStartupOptions with the
         * specified list of Dynamo modules ("DAS", "DPS", "DSS",
         * "Publishing.base", etc).
         * Additionally sets opts to add a directory calculated relative
         * to the location of pClassRelativeTo's package name from the classloader.
         * The added config directory is calculated as (in psuedocode):
         * <code>
         * (pClassRelativeTo's package location) + "/data/" +  (pClassRelativeTo's simpleClassName)
         * +
         * "/config"
         * </code>
         * and is only added if the directory exists. <P>
         * <p/>
         * You must specify a <code>pInitialService</code> parameter, which
         * will be the initial service started by Nucleus (rather than the
         * normally Initial component, which would do a full Nucleus component
         * start-up). <P>
         *
         * @param pModules         the list of modules to use to calculate the
         *                         Nucleus configuration path.
         * @param pClassRelativeTo the class whose name and package
         *                         will be used for the {packageName}/config/{simpleClassName}/data
         *                         directory
         * @param pInitialService  the nucleus path of the Nucleus component
         *                         to start-up. This is a required property to prevent accidental
         *                         full start-up.
         *
         * @return the started Nucleus instance that should later be shut down
         *         with the shutdownNucleus method.
         * @throws ServletException if an error occurs
         */
        public NucleusStartupOptions(String[] pModules,
                                     Class pClassRelativeTo,
                                     String pInitialService) {


            mModules = pModules;
            mClassRelativeTo = pClassRelativeTo;
            mInitialService = pInitialService;
            mBaseConfigDirectory = pClassRelativeTo.getSimpleName() + "/config";
        }

        /**
         * This constructor creates NucleusStartupOptions with the
         * specified list of Dynamo modules ("DAS", "DPS", "DSS",
         * "Publishing.base", etc).
         * Additionally sets opts to add a directory calculated relative
         * to the location of pClassRelativeTo's package name from the classloader.
         * The added config directory is calculated as (in psuedocode):
         * <code>
         * (pClassRelativeTo's package location) + "/" + (pConfigDirectory or "data") + "/config"
         * </code>
         * and is only added if the directory exists. <P>
         * <p/>
         * You must specify a <code>pInitialService</code> parameter, which
         * will be the initial service started by Nucleus (rather than the
         * normally Initial component, which would do a full Nucleus component
         * start-up). <P>
         *
         * @param pModules             the list of modules to use to calculate the
         *                             Nucleus configuration path.
         * @param pClassRelativeTo     the class whose package the config/data
         *                             (or pBaseConfigDirectory/data) should be relative in.
         * @param pBaseConfigDirectory the base configuration directory. If
         *                             this parameter is non-null, the relative configuration
         *                             subdirectory will be
         *                             ("data/" + pBaseConfigDirectory) rather than "data/config".
         * @param pInitialService      the nucleus path of the Nucleus component
         *                             to start-up. This is a required property to prevent
         *                             accidental
         *                             full start-up.
         *
         * @return the started Nucleus instance that should later be shut down
         *         with the shutdownNucleus method.
         * @throws ServletException if an error occurs
         */
        public NucleusStartupOptions(String[] pModules,
                                     Class pClassRelativeTo,
                                     String pBaseConfigDirectory,
                                     String pInitialService) {

            mModules = pModules;
            mClassRelativeTo = pClassRelativeTo;
            mInitialService = pInitialService;
            mBaseConfigDirectory = pBaseConfigDirectory;
        }

        /**
         * Return the list of modules for starting Nucleus. These modules
         * are the modules whose config path will be included.
         */
        public String[] getModules() {
            return mModules;
        }

        //-------------------------------------
        // property: baseConfigDirectory

        /**
         * Return the "class relative to" property. This is the Class whose
         * package the config directory will be relative to.
         */
        public Class getClassRelativeTo() {
            return mClassRelativeTo;
        }


        //-------------------------------------
        // property: layers

        /**
         * Gets the initialService. This is the InitialService for Nucleus
         * to resolve at start-up. Required.
         */
        public String getInitialService() {
            return mInitialService;
        }

        /**
         * Set the basic config directory. This is the directory that will be
         * taked on to the package path of the classRelativeTo class. If this
         * property is non-null, the relative configuration subdirectory will
         * be ("data/" + baseConfigDirectory).
         */
        public String getBaseConfigDirectory() {
            return mBaseConfigDirectory;
        }

        /**
         * Returns the Dynamo layers to run with.
         */
        public String[] getLayers() {
            return mLayers;
        }

        /**
         * Sets the Dynamo layers to run with.
         */
        public void setLayers(String[] pLayers) {
            mLayers = pLayers;
        }

        //-------------------------------------
        // property: liveconfig

        /**
         * Return the layers as a string appropriate for passing to
         * DynamoServerLauncher, calculateConfigPath.
         *
         * @return null if layers is null. Otherwise returns a space delimited
         *         list of layers.
         */
        public String getLayersAsString() {
            if ( mLayers == null ) {
                return null;
            }
            StringBuilder strbuf = new StringBuilder();
            for ( String strCur : mLayers ) {
                if ( strbuf.length() != 0 ) {
                    strbuf.append(" ");
                }
                strbuf.append(strCur);
            }
            return strbuf.toString();
        }

        /**
         * Returns property liveconfig.
         *
         * @return true if liveconfig should be set, false otherwise.
         */
        public boolean getLiveconfig() {
            return mLiveconfig;
        }

        /**
         * Sets property liveconfig.
         *
         * @param pLiveconfig true if Nucleus should be started in liveconfig
         *                    mode, false otherwise.
         */
        public void setLiveconfig(boolean pLiveconfig) {
            mLiveconfig = pLiveconfig;
        }

        /**
         * Modify Nucleus command-line options, as needed. This will
         * be invoked just before Nucleus is started as a final
         * chance to adjust any command-line options.
         */
        public void modifyNucleusCommandLineOptions(List<String> listArgs) {
        }
    } // end inner-class NucleusStartupOptions

}
