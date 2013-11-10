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

package atg.junit.nucleus;

import atg.applauncher.AppLauncher;
import atg.applauncher.AppModule;
import atg.nucleus.DynamoEnv;
import atg.nucleus.Nucleus;
import atg.service.dynamo.LicenseImpl;
import atg.service.email.ContentPart;
import atg.service.email.EmailEvent;
import atg.service.email.MimeMessageUtils;
import atg.service.email.SMTPEmailSender;
import atg.servlet.ServletUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

/**
 * This class is used to hold useful utilty methods people may
 * need when running tests.
 */
public class TestUtils
        extends atg.nucleus.GenericService {

    private static Logger log = Logger.getLogger(TestUtils.class);

    // names of app servers types that may be specified by the
    // 'atg.dynamo.appserver' system property
    // Dynamo currently does not distinguish between generic
    // Tomcat and JBoss, everything is just referred to as 'tomcat'
    public static final String APP_SERVER_DAS = "das";

    public static final String APP_SERVER_BEA = "weblogic";

    public static final String APP_SERVER_IBM = "websphere";

    public static final String APP_SERVER_TOMCAT = "tomcat";

    // names of various vendors that ATG works with
    public static final String VENDOR_ATG = "ATG";

    public static final String VENDOR_BEA = "BEA";

    public static final String VENDOR_IBM = "IBM";

    public static final String VENDOR_JBOSS = "JBOSS";

    // the variable that points to the installation directory for dynamo
    private static final String ROOT_VAR = "atg.dynamo.root";

    private static final String HOME_VAR = "atg.dynamo.home";

    private static final String ATG_J2EESERVER_ROOT = "atg.j2eeserver.root";

    // these are used to lookup some system settings from the VM
    private static final String JAVA_VAR = "java.vm.info";

    private static final String JAVA_VERSION = "java.vm.version";

    private static final String JAVA_BUILD_VERSION = "java.version";

    private static final String COMPILER_VAR = "java.compiler";

    private static final String OS_VAR = "os.name";

    private static final String OS_VERSION_VAR = "os.version";

    // the system variable that returns the name of the app server being used
    private static final String APP_SERVER = "atg.dynamo.appserver";

    // the Configuration component used by Dynamo
    private static final String CONFIGURATION_COMPONENT = "/atg/dynamo/Configuration";

    // mailhost used to send email
    private static final String MAILHOST = "mailsvr.atg.com";

    // value returned by several methods, as noted in javadoc, if a
    // piece of information can not be definitively determined.  in
    // particular, used when reporting about product build and version
    // information
    public static final String UNKNOWN_INFO = "unknown";

    /**
     * property to track the DUST version being used.  utilized by
     * ATGXMLFileTestResultReported so we can tag XML result files for
     * compatibility validation when passed to the XML file logger
     */
    public static int DUST_VERSION = 1;

    /**
     * specifies the DUST version being used.  utilized by
     * ATGXMLFileTestResultReporter so XML result files can be tagged
     * for compatibility validation when passed to the XML file
     * logger
     */
    public void setDustVersion(int pVersion) {
        DUST_VERSION = pVersion;
    }

    /**
     * returns the DUST version being used.  utilized by
     * ATGXMLFileTestResultReporter so XML result files can be tagged
     * for compatibility validation when passed to the XML file
     * logger
     */
    public int getDustVersion() {
        return DUST_VERSION;
    }

    /**
     * property to track the DUST user.  utilized when results are
     * logged to the database to correlate the result with a user
     * account in the test management system.
     */
    public static String DUST_USERNAME = System.getProperty("user.name");

    /**
     * specifies the DUST user.  utilized when results are logged to
     * the database to correlate the result with a user account in the
     * test management system.
     */
    public void setDustUsername(String pUsername) {
        DUST_USERNAME = pUsername;
    }

    /**
     * returns the DUST user name.  utilized when results are logged to
     * the database to correlate the result with a user account in the
     * test management system.
     */
    public String getDustUsername() {
        if ( DUST_USERNAME == null || DUST_USERNAME.trim().length() == 0 ) {
            return System.getProperty("user.name");
        } else {
            return DUST_USERNAME;
        }
    }

    /**
     * property to track which testrun a result is part of.  utilized
     * by TSM to correlate a specifid result with the testrun used to
     * install and configure the test Dynamo.
     */
    public static String TSM_TESTRUN = null;

    /**
     * Specifies the TSM testrun this result is part of.  utilized by
     * TSM to correlate a specifid result with the testrun used to
     * install and configure the test Dynamo.
     */
    public void setTsmTestrun(String pId) {
        TSM_TESTRUN = pId;
    }

    /**
     * Returns the TSM testrun this result is part of.  utilized by TSM
     * to correlate a specifid result with the testrun used to install
     * and configure the test Dynamo.
     */
    public String getTsmTestrun() {
        return TSM_TESTRUN;
    }

    /**
     * property to track the p4 sync time for tests.  utilized by TSM
     * to inform end-users of time at which machine was last synced.
     * must be specified by test setup before test is run.
     */
    public static String P4SYNCTIME = null;

    /**
     * property to track the p4 sync time for tests.  utilized by TSM
     * to inform end-users of time at which machine was last synced.
     * must be specified by test setup before test is run.
     */
    public void setP4Synctime(String pTime) {
        P4SYNCTIME = pTime;
    }

    /**
     * property to track the p4 sync time for tests.  utilized by TSM
     * to inform end-users of time at which machine was last synced.
     * must be specified by test setup before test is run.
     */
    public String setP4Synctime() {
        return P4SYNCTIME;
    }

    /**
     * Returns the directory in which Dynamo was installed.  If the
     * installation directory was not specified during the DUST
     * installation, returns null.  Should <b>only</b> be used by
     * System tests.
     */
    public static File DYNAMO_INSTALL_DIR = null;

    /**
     * Returns the directory in which Dynamo was installed.  If the
     * installation directory can not be successfully determined returns
     * null.
     * <br><b>NOTE:</b> There is no reliable System property (or
     * other inherent mechanism) to determine the Dynamo installation
     * directory when running as BigEar, so this value is set during the
     * DUST build process.  The SystemTests.base build step writes the
     * file
     * SystemTests/base/config/atg/junit/nucleus/TestUtils.properties
     * with the proper value.  Consequently, this method should only be
     * used by System tests, not Unit tests.
     */
    public File getDynamoInstallDir() {
        return DYNAMO_INSTALL_DIR;
    }

    /**
     * Specifies the directory in which Dynamo was installed.
     */
    public void setDynamoInstallDir(File pDir) {
        DYNAMO_INSTALL_DIR = pDir;
    }

    /**
     * Returns the root directory for this Dynamo.  If the root
     * directory can not be successfully located returns null.  Operates
     * according to this logic:
     * <ul>
     * <li>If "home" Dynamo module can be found, return parent directory
     * containing that module.  (This should resolve when running 'BigEar')
     * <li>Otherwise, return value of System property 'atg.dynamo.root'.
     * (this could still be null)
     * </ul>
     */
    public static File getDynamoRootDir() {
        File root = null;
        try {
            root = getDynamoHomeDir().getParentFile();
        } catch ( Throwable t ) {
        }
        if ( root == null ) {
            try {
                root = new File(System.getProperty(ROOT_VAR));
            } catch ( Throwable t ) {
            }
        }
        return root;
    }

    /**
     * Returns Dynamo's "home" module installation directory.  If the
     * directory can not be successfully located returns null.
     * <br>Logic works like this:
     * <ul>
     * <li>If "home" Dynamo module can be found, return directory representing
     * that module.  This should resolve when running 'BigEar' and may
     * point to a subdirectory of the application server's directory used
     * to deploy ear files.  On DAS it should resolve to
     * <DYNAMO_INSTALL_DIR>/home.
     * <li>Otherwise, return value of System property 'atg.dynamo.home'.
     * (this could be null)
     * </ul>
     */
    public static File getDynamoHomeDir() {
        File root = null;
        try {
            root = getModuleResourceFile("home", ".").getCanonicalFile();
        } catch ( Throwable t ) {
        }
        if ( root == null ) {
            try {
                root = new File(System.getProperty(HOME_VAR));
            } catch ( Throwable t ) {
            }
        }

        return root;
    }

    /**
     * returns the root install directory for the ATG J2EE Server, or null if
     * the ATG J2EE Server is not installed.
     */
    public static File getAtgJ2eeServerInstallDir() {
        try {
            return new File(System.getProperty(ATG_J2EESERVER_ROOT));
        } catch ( Throwable t ) {
            return null;
        }
    }


    /**
     * Returns the product name of the app server being used. For ATG
     * we currently assume it's called 'ATGDAS' if it's a separate
     * product, since there is no definitive way to figure out the
     * product name from MANIFEST files. For all other app servers it
     * returns the value of getAppServerType().
     */
    public static String getAppServerProductName() {
        if ( getAppServerType().equals(APP_SERVER_DAS) ) {
            return getAtgJ2eeServerProductName();
        } else {
            return getAppServerType();
        }
    }

    /**
     * Returns the name of the ATG J2EE Server product this is installed, or
     * null if a separate ATG J2EE Server build is not installed.
     */
    public static String getAtgJ2eeServerProductName() {
        // TODO: Bug 78552 was opened to add a MANIFEST entry containing
        // the product name so we don't have to hard code this to
        // 'ATGDAS'.
        if ( getAtgJ2eeServerInstallDir() != null ) {
            return "ATGDAS";
        } else {
            return null;
        }
    }

    /**
     * Returns the version number of the app server being used.  For
     * DAS this version comes from the J2EEServer MANIFEST file, or is
     * UNKNOWN_INFO if the MANIFEST can't be found. Such may be the
     * case if a person is using devtools to build their product. For
     * 3PAS the version number is extracted from their configuration
     * file (typically some well known XML file).
     */
    public static String getAppServerVersion() {
        String apptype = getAppServerType();
        if ( apptype.equals(APP_SERVER_DAS) ) {
            return getAtgVersion(getAtgJ2eeServerModule());
        } else if ( apptype.equals(APP_SERVER_BEA) ) {
            return getBeaVersion();
        } else if ( apptype.equals(APP_SERVER_IBM) ) {
            return getWasVersion();
        } else if ( apptype.equals(APP_SERVER_TOMCAT) ) {
            return getJBossVersion();
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the build number of the app server being used.  For DAS
     * this version comes from the J2EEServer MANIFEST file, or is
     * UNKNOWN_INFO if the MANIFEST can't be found. Such may be the
     * case if a person is using devtools to build their product. For
     * 3PAS the build number is always UNKNOWN_INFO.
     */
    public static String getAppServerBuildNumber() {
        String apptype = getAppServerType();
        if ( apptype.equals(APP_SERVER_DAS) ) {
            return getAtgBuildNumber(getAtgJ2eeServerModule());
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the patch version of the app server being used.  For DAS
     * this version comes from the J2EEServer MANIFEST file, or is
     * UNKNOWN_INFO if the MANIFEST can't be found. Such may be the
     * case if a person is using devtools to build their product. For
     * 3PAS the patch version is always UNKNOWN_INFO.
     */
    public static String getAppServerPatchVersion() {
        String apptype = getAppServerType();
        if ( apptype.equals(APP_SERVER_DAS) ) {
            return getAtgPatchVersion(getAtgJ2eeServerModule());
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the patch build number of the app server being used.
     * For DAS this version comes from the J2EEServer MANIFEST file, or
     * is UNKNOWN_INFO if the MANIFEST can't be found. Such may be the
     * case if a person is using devtools to build their product. For
     * 3PAS the patch build number is always UNKNOWN_INFO.
     */
    public static String getAppServerPatchBuildNumber() {
        String apptype = getAppServerType();
        if ( apptype.equals(APP_SERVER_DAS) ) {
            return getAtgPatchBuildNumber(getAtgJ2eeServerModule());
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the vendor name of the App server manufacturer.  if a
     * vendor can not be determined it returns UNKNOWN_INFO.
     */
    public static String getAppServerVendor() {
        String apptype = getAppServerType();
        if ( apptype.equals(APP_SERVER_DAS) ) {
            return VENDOR_ATG;
        } else if ( apptype.equals(APP_SERVER_BEA) ) {
            return VENDOR_BEA;
        } else if ( apptype.equals(APP_SERVER_IBM) ) {
            return VENDOR_IBM;
        } else if ( apptype.equals(APP_SERVER_TOMCAT) ) {
            return VENDOR_JBOSS;
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns true if the Dynamo product is being used; false if only the ATG
     * J2EE Server product is running.
     */
    public static boolean isDynamoInstalled() {
        try {
            // if j2ee server is not installed then Dynamo must be...
            if ( getAtgJ2eeServerInstallDir() == null ) {
                return true;
            }
            // if the j2ee server root is the same as the dynamo root then
            // we're running only the j2ee server
            else {
                return (!getAtgJ2eeServerInstallDir().getCanonicalFile()
                        .equals(getDynamoRootDir().getCanonicalFile()));
            }
        } catch ( IOException ioe ) {
            // this should never happen, but if it does return false...
            return false;
        }
    }

    /**
     * This method returns the name of the Dynamo product that is
     * installed.  Because there is no guaranteed way to determine the
     * installed product this method makes a best-guess.  If the
     * version is less than 5.5 then the method skips down from DCS, to
     * DPS, etc. until it finds a product that exists. If the version
     * 5.5, 5.6, or 5.6.1 then this method just returns 'AppDAP' since
     * that is the only Dynamo product we have for those versions.
     * Likewise, if the version is NOT anything between 4 and 5.6, then
     * we return 'ATG' since that is the only version we have for
     * copper, etc.  If the method can't determine a version it returns
     * UNKNOWN_INFO
     */
    public static String getDynamoProductName() {
        AppModule module = getAtgDynamoModule();
        if ( module == null ) {
            return UNKNOWN_INFO;
        }
        String version = getAtgVersion(module);

        if ( version == null ) {
            return UNKNOWN_INFO;
        } else if ( version.startsWith("5.5") || version.startsWith("5.6") ) {
            // this is an AppDAP build of 5.5, 5.6, or 5.6.1
            return "AppDAP";
        } else if ( !version.startsWith("4") && !version.startsWith("5.0") && !version.startsWith(
                "5.1"
        ) ) {
            // assume this is an ATG build from version 6.x
            // TODO: Bug 78552 was opened to add a MANIFEST entry containing
            // the product name so we don't have to guess at it.
            return "ATG";
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns information about the ATG Dynamo product being used.
     * does not include information about the app server that may be in
     * use.  returns null if Dynamo is not running.
     */
    public static String getDynamoProductInfo() {
        StringBuffer sb = new StringBuffer();
        AppModule dynamo = getAtgDynamoModule();

        if ( dynamo == null ) {
            return null;
        }

        sb.append(getDynamoProductName() + " version " + getAtgVersion(dynamo));
        String build = getAtgBuildNumber(dynamo);
        if ( !build.equals(UNKNOWN_INFO) ) {
            sb.append(" build " + build);
        }
        String patch_version = getAtgPatchVersion(dynamo);
        String patch_build = getAtgPatchBuildNumber(dynamo);
        if ( !(patch_version == null) && !patch_version.equals(UNKNOWN_INFO) ) {
            sb.append(" with patch " + patch_version + " build " + patch_build);
        }

        return sb.toString();
    }

    /**
     * Returns a summary of information about the App Server product
     * being used.
     */
    public static String getAppServerProductInfo() {
        StringBuffer sb = new StringBuffer();

        sb.append(getAppServerProductName() + " version " + getAppServerVersion());
        String build = getAppServerBuildNumber();
        if ( !build.equals(UNKNOWN_INFO) ) {
            sb.append(" build " + build);
        }
        String patch_version = getAppServerPatchVersion();
        String patch_build = getAppServerPatchBuildNumber();
        if ( !(patch_version == null) && !patch_version.equals(UNKNOWN_INFO) ) {
            sb.append(" with patch " + patch_version + " build " + patch_build);
        }

        return sb.toString();
    }

    /**
     * returns the java version that Dynamo is using
     *
     * @return String version of java Dynamo is using
     */
    public static String getJavaVersion() {
        return System.getProperty(JAVA_VERSION);
    }

    /**
     * returns the java build version (java.version) that Dynamo is using
     *
     * @return String build version of java Dynamo is using
     */
    public static String getJavaBuildVersion() {
        return System.getProperty(JAVA_BUILD_VERSION);
    }

    /**
     * returns detailed version information about the jdk being used
     */
    public static String getJavaVersionDetails() {
        return TestUtils.getJavaInfo() + " - " + TestUtils.getJavaVersion();
    }

    /**
     * returns info about the java build that Dynamo is using
     *
     * @return String info about java build Dynamo is using
     */
    public static String getJavaInfo() {
        return System.getProperty(JAVA_VAR);
    }

    /**
     * returns the type of compiler that Dynamo is using
     *
     * @return String compiler Dynamo is using
     */
    public static String getCompilerType() {
        return System.getProperty(COMPILER_VAR);
    }

    /**
     * returns the type of Operating System that Dynamo is running on
     */
    public static String getOperatingSystemType() {
        return System.getProperty(OS_VAR) + " version " + System.getProperty(OS_VERSION_VAR);
    }

    /**
     * returns the hostname of the machine that Dynamo is running on
     */
    public static String getHostname() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostName();
        } catch ( UnknownHostException uhe ) {
        }

        return "unknown";
    }

    /**
     * returns the name of the app server that dynamo is using
     */
    public static String getAppServerType() {
        if ( ServletUtil.isWebLogic() ) {
            return APP_SERVER_BEA;
        } else if ( ServletUtil.isWebSphere() ) {
            return APP_SERVER_IBM;
        } else if ( ServletUtil.isDynamoJ2EEServer() ) {
            return APP_SERVER_DAS;
        } else if ( isGenericAppServer() ) {
            return APP_SERVER_TOMCAT;
        } else {
            return System.getProperty(APP_SERVER);
        }
    }

    /**
     * Returns true if Dynamo is running on a 'generic' (aka Tomcat)
     * j2ee appserver; false otherwise.
     * ServletUtil.isGenericJ2EEServer() method call does not exist in
     * early Dynamo versions, so use reflection to invoke it.  As of
     * ATG 7x, isGenericJ2EEServer() really means "are we running on
     * JBOSS" - I'm not sure whether we intend to differentiate between
     * JBOSS and other 'generic' Tomcat app servers.
     */
    public static boolean isGenericAppServer() {
        try {
            ServletUtil.class.newInstance();
            return ((Boolean) invokeMethod(
                    dynamoEnv(), "isGenericJ2EEServer", null, null, null
            )).booleanValue();
        } catch ( Throwable t ) {
        }
        return false;
    }

    /**
     * returns the WAS home directory if running on WAS.  otherwise,
     * returns null
     */
    public static String getWasHomeDir() {
        return System.getProperty("was.install.root");
    }

    /**
     * returns the WAS version number.  if not running against WAS, or
     * if the {WAS.install.root}/properties/version/BASE.product file
     * can not be found, or if an error occurs parsing the file then
     * returns UNKNOWN_INFO.
     */
    public static String getWasVersion() {
        String version = null;
        try {
            // WAS 5
            File f = new File(getWasHomeDir(), "properties/version/BASE.product");
            // WAS 6
            if ( !f.exists() ) {
                f = new File(getWasHomeDir(), "properties/version/WAS.product");
            }

            String[] children1 = { "version" };
            List<Node> nodes1 = XmlUtils.getNodes(f, false, children1);
            if ( nodes1 != null ) {
                Iterator<Node> iter1 = nodes1.iterator();
                while ( iter1.hasNext() ) {
                    Node n1 = iter1.next();
                    version = XmlUtils.getNodeTextValue(n1);
                }
            }
        } catch ( Throwable e ) {
        }

        if ( version != null ) {
            return version;
        } else {
            return UNKNOWN_INFO;
        }
    }


    /**
     * returns the full file path of specified was log if
     * getWasHomeDir() is not null.  otherwise returns null.
     */
    private static File getWasLogFile(String pServerName, String pLogName) {
        if ( getWasHomeDir() == null ) {
            return null;
        } else {
            return new File(
                    getWasHomeDir(),
                    "logs" + File.separator + pServerName + File.separator + pLogName
            );
        }
    }

    private static String mWasSystemOutLogFile = null;

    /**
     * Specifies the log file to return when asked for the WAS
     * 'SystemOut.log' file.  if this value is null we attempt to
     * calculate a default location
     */
    public void setWasSystemOutLogFile(String pFile) {
        mWasSystemOutLogFile = pFile;
    }

    /**
     * returns the expected location of the WAS 'SystemOut.log' file if
     * running on WAS.  otherwise returns null.
     */
    public static String getWasSystemOutLogFile() {
        if ( getWasHomeDir() == null ) {
            return null;
        } else if ( mWasSystemOutLogFile != null && mWasSystemOutLogFile.trim().length() > 0 ) {
            return mWasSystemOutLogFile.trim();
        } else {
            File f = getWasLogFile(ServletUtil.getWebsphereServerName(), "SystemOut.log");
            if ( f == null || !f.exists() ) {
                f = getWasLogFile("server1", "SystemOut.log");
            }

            if ( f != null ) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    private static String mWasSystemErrLogFile = null;

    /**
     * Specifies the log file to return when asked for the WAS
     * 'SystemErr.log' file.  if this value is null we attempt to
     * calculate a default location
     */
    public void setWasSystemErrLogFile(String pFile) {
        mWasSystemErrLogFile = pFile;
    }

    /**
     * returns the expected location of the WAS 'SystemErr.log' file if
     * running on WAS.  otherwise returns null.
     */
    public static String getWasSystemErrLogFile() {
        if ( getWasHomeDir() == null ) {
            return null;
        } else if ( mWasSystemErrLogFile != null && mWasSystemErrLogFile.trim().length() > 0 ) {
            return mWasSystemErrLogFile.trim();
        } else {
            File f = getWasLogFile(ServletUtil.getWebsphereServerName(), "SystemErr.log");
            if ( f == null || !f.exists() ) {
                f = getWasLogFile("server1", "SystemErr.log");
            }

            if ( f != null ) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * returns the BEA home directory if running on BEA.  otherwise,
     * returns null
     */
    public static String getBeaHomeDir() {
        String homedir = System.getProperty("bea.home");
        if ( homedir != null ) {
            return homedir;
        }

        // sometimes (like on bea 8) 'bea.home' is not specified, so try
        // to determine bea.home base on another property...
        String startfile = System.getProperty("java.security.policy");
        if ( startfile != null ) {
            // the policy file is (hopefully) always located at a location like:
            // /root/to/bea/<weblogic>/server/lib/weblogic.policy
            // so we basically want to go up four levels from there...
            homedir = atg.core.io.FileUtils.getParent(startfile);
            // should now be in /root/to/bea/<weblogic>/server/lib
            homedir = atg.core.io.FileUtils.getParent(homedir);
            // should now be in /root/to/bea/<weblogic>/server
            homedir = atg.core.io.FileUtils.getParent(homedir);
            // should now be in /root/to/bea/<weblogic>
            homedir = atg.core.io.FileUtils.getParent(homedir);
            // should now be in /root/to/bea
        }

        return homedir;
    }

    private static String mBeaMyServerLogFile = null;

    /**
     * Specifies the log file to return when asked for the BEA
     * 'myserver.log' file.  if this value is null then a default value
     * will be calculated.
     */
    public static void setBeaMyServerLogFile(String pFile) {
        mBeaMyServerLogFile = pFile;
    }

    /**
     * returns the expected location of the BEA 'myserver.log' file if
     * running on BEA.  otherwise returns null.
     */
    public static String getBeaMyServerLogFile() {
        if ( getBeaHomeDir() == null ) {
            return null;
        } else if ( mBeaMyServerLogFile != null && mBeaMyServerLogFile.trim().length() > 0 ) {
            return mBeaMyServerLogFile.trim();
        } else {
            // try this default location....
            String name = "user_projects" + File.separator +
                          "mydomain" + File.separator +
                          "myserver" + File.separator +
                          "myserver.log";
            File log = new File(getBeaHomeDir(), name);

            if ( log.exists() ) {
                return log.getAbsolutePath();
            } else {
                // the default didn't work (as we shouldn't always expect it
                // to) so try this location...  'user.dir' typically points to
                // the domain dir: /path/to/bea/user_projects/mydomain
                // 'weblogic.Name' should be like : myserver
                name = System.getProperty("user.dir") + File.separator +
                       System.getProperty("weblogic.Name") + File.separator +
                       System.getProperty("weblogic.Name") + ".log";
                log = new File(name);
                if ( log.exists() ) {
                    return log.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * returns the BEA version number.  if not running against BEA, or if
     * the {BEA_HOME}/registry.xml file can not be found, or if an error occurs
     * parsing the file then returns UNKNOWN_INFO.
     */
    public static String getBeaVersion() {
        String version = null;
        try {
            File f = new File(new File(getBeaHomeDir()), "registry.xml");
            String[] children = { "host", "product", "release" };
            List<Node> nodes = XmlUtils.getNodes(f, false, children);
            if ( nodes != null ) {
                Iterator<Node> iter = nodes.iterator();
                // I expect there to only be one <host><product><release> node
                // so this iteration should really just loop over one node.
                while ( iter.hasNext() ) {
                    Node n = iter.next();
                    version = XmlUtils.getAttribute(n, "level", "0") + "." +
                              XmlUtils.getAttribute(n, "ServicePackLevel", "0") + "." +
                              XmlUtils.getAttribute(n, "PatchLevel", "0");
                }
            }
        } catch ( Throwable e ) {
        }

        if ( version != null ) {
            return version;
        } else {
            return UNKNOWN_INFO;
        }
    }

    // ---------------- JBOSS Utility Methods --------------------------
    // NOTE: Available JBoss System property names can be found in class
    // org.jboss.system.server.ServerConfig

    /**
     * Returns the JBOSS installation home directory, if Dynamo is
     * running on JBOSS.  Otherwise returns null.
     */
    public static File getJBossHomeDir() {
        String dir = System.getProperty("jboss.home.dir");
        if ( dir == null ) {
            return null;
        } else {
            return new File(dir);
        }
    }

    /**
     * Returns the JBOSS server home directory, if Dynamo is running on
     * JBOSS.  Otherwise returns null.
     */
    public static File getJBossServerHomeDir() {
        String dir = System.getProperty("jboss.server.home.dir");
        if ( dir == null ) {
            return null;
        } else {
            return new File(dir);
        }
    }

    /**
     * Returns the JBOSS server name, if Dynamo is running on JBOSS.
     * Otherwise returns null.
     */
    public static String getJBossServerName() {
        return System.getProperty("jboss.server.name");
    }

    /**
     * Returns the path to the JBOSS server log file, if it can be
     * found.  Otherwise returns null
     */
    public static String getJBossServerLog() {
        try {
            File log = new File(getJBossServerHomeDir(), "log/server.log");
            if ( log.exists() ) {
                return log.getAbsolutePath();
            }
        } catch ( Throwable t ) {
        }
        return null;
    }

    /**
     * Returns the version of JBOSS being used, if it can be
     * determined.  Otherwise returns UNKNOWN_INFO.  This method
     * expects to find a 'jar-versions.xml' file in the JBoss home
     * directory. It searches for the &lt;jar&gt; element whose 'name'
     * attribute is "jboss.jar", and determines the version based on
     * the value of the 'implVersion' attribute.
     */
    public static String getJBossVersion() {
        try {
            File versionFile = new File(getJBossHomeDir(), "jar-versions.xml");
            if ( !versionFile.exists() ) {
                log(
                        "jar-versions.xml file does not exist; "
                        + "unable to determine version info"
                );
                return UNKNOWN_INFO;
            }
            String[] children = { "jar" };
            Iterator<Node> nodes = XmlUtils.getNodes(versionFile, false, children).iterator();
            while ( nodes.hasNext() ) {
                try {
                    Node node = nodes.next();
                    String name = node.getAttributes().getNamedItem("name").getNodeValue();
                    log("Checking node: " + name);
                    if ( name.equals("jboss.jar") ) {
                        String ver = node.getAttributes()
                                         .getNamedItem("implVersion")
                                         .getNodeValue()
                                         .trim();
                        log("JBOSS version string: " + ver);
                        // implVersion is typically something like
                        // "4.0.1sp1 (build: CVSTag=JBoss_4_0_1_SP1 date=200502160314)"
                        // so, strip off the build information since we don't care about it
                        int idx = ver.indexOf(" (build:");
                        if ( idx != -1 ) {
                            ver = ver.substring(0, idx).trim();
                        }
                        return ver;
                    }
                } catch ( Throwable ti ) {
                }
            }
        } catch ( Throwable t ) {
        }
        return UNKNOWN_INFO;
    }

    private static atg.service.dynamo.Configuration DYN_CONFIG = null;

    /**
     * returns the Configuration component being used by Dynamo
     */
    public static atg.service.dynamo.Configuration getDynamoConfiguration() {
        if ( DYN_CONFIG == null ) {
            try {
                DYN_CONFIG = (atg.service.dynamo.Configuration) Nucleus.getGlobalNucleus()
                                                                       .resolveName(
                                                                               CONFIGURATION_COMPONENT
                                                                       );
            } catch ( Throwable t ) {
            }
        }
        return DYN_CONFIG;
    }

    /**
     * returns the session limit of the specified license component
     *
     * @param String  the component name of the license in question
     * @param boolean true if Nucleus should attempt to create the license
     *                component if it does not exist
     *
     * @return int the session limit for the license.  0 if the license does not
     *         resolve.
     */
    public static int getSessionLimit(String pLicense, boolean pResolve) {
        if ( pLicense == null ) {
            return 0;
        }
        LicenseImpl license = (LicenseImpl) Nucleus.getGlobalNucleus()
                                                   .resolveName(pLicense, pResolve);
        if ( license == null ) {
            return 0;
        }
        return license.getMaxSessions();
    }

    // ==================== EMAIL ======================

    /**
     * This method is used to send an email message and allows the user
     * to specify the return address.
     *
     * @return boolean true if the mail was sent successfully; otherwise false.
     */
    public static boolean sendEmailWithReturn(String pAddress,
                                              String pMsg,
                                              String pSubject,
                                              String pBodyEncoding,
                                              String pReturnAddress) {
        try {
            // create a Message with the given From and Subject
            Message msg = MimeMessageUtils.createMessage(pReturnAddress, pSubject);
            // set the To recipient
            MimeMessageUtils.setRecipient(msg, Message.RecipientType.TO, pAddress);

            // set the message content: multipart message + attachment
            if ( pBodyEncoding == null || pBodyEncoding.trim().length() == 0 ) {
                pBodyEncoding = "text/plain";
            }
            ContentPart[] content = { new ContentPart(pMsg, pBodyEncoding) };
            MimeMessageUtils.setContent(msg, content);

            // create the email event
            EmailEvent em = new EmailEvent(msg);

            // now send the event
            SMTPEmailSender sender = new SMTPEmailSender();
            sender.setEmailHandlerHostName(MAILHOST);
            sender.sendEmailEvent(em);
        } catch ( Exception e ) {
            log.info("Caught exception sending email: " + e.toString());
            return false;
        }
        return true;
    }

    /**
     * This method is used to send an email message; returns true if
     * everything went ok; otherwise, returns false
     */
    public static boolean sendEmail(String pAddress,
                                    String pMsg,
                                    String pSubject,
                                    String pBodyEncoding) {
        return sendEmailWithReturn(pAddress, pMsg, pSubject, pBodyEncoding, pAddress);
    }

    /**
     * This method is used to send an email message; returns true if
     * everything went ok; otherwise, returns false
     */
    public static boolean sendEmail(String pAddress, String pMsg, String pSubject) {
        return sendEmail(pAddress, pMsg, pSubject, "text/plain");
    }

    /**
     * This method is used to send the same email message to a vector
     * of recipients
     */
    public static void sendEmails(List<String> pAddresses,
                                  String pMsg,
                                  String pSubject,
                                  String pBodyEncoding) {
        // make sure addresses are valid
        if ( pAddresses == null || pAddresses.size() <= 0 ) {
            return;
        }

        // send emails
        Iterator<String> addresses = pAddresses.iterator();
        String address = null;
        while ( addresses.hasNext() ) {
            try {
                address = addresses.next();
                if ( address != null && address.trim().length() > 0 ) {
                    sendEmail(address.trim(), pMsg, pSubject, null, null, null, pBodyEncoding);
                }
            } catch ( Exception e ) {
            }
        }
    }

    /**
     * This method is used to send the same email message to a vector
     * of recipients.  It encodes the message body as "text/plain".
     */
    public static void sendEmails(List<String> pAddresses, String pMsg, String pSubject) {
        sendEmails(pAddresses, pMsg, pSubject, "text/plain");
    }

    /**
     * This method is used to send an email message that contains
     * several attachments.  This method is specifically designed to
     * accept a map of java.lang.Strings as content parts instead of
     * java.io.Files.  The key in the Map should be a String
     * representing the name that you would like to show for the
     * attached file.  The value in the Map should be a String
     * representing the contents of the attachment.
     */
    public static void sendEmail(String pAddress,
                                 String pMsg,
                                 String pSubject,
                                 Map<String, Object> pTextAttachments,
                                 Map<String, Object> pHTMLAttachments,
                                 File[] pFiles,
                                 String pBodyEncoding) {
        try {
            // make sure addresses are valid
            if ( pAddress == null || pAddress.trim().length() == 0 ) {
                return;
            }

            // create a Message with the given From and Subject
            Message msg = MimeMessageUtils.createMessage(pAddress, pSubject);

            // set the To recipient
            MimeMessageUtils.setRecipient(msg, Message.RecipientType.TO, pAddress);

            // create the MultiPart used to hold everything
            Multipart mp = new MimeMultipart();

            // set the message content: multipart message + attachment
            BodyPart guts = new MimeBodyPart();
            if ( pBodyEncoding == null || pBodyEncoding.trim().length() == 0 ) {
                pBodyEncoding = "text/plain";
            }
            guts.setContent(pMsg, pBodyEncoding);
            mp.addBodyPart(guts);

            // add the text attachments
            if ( pTextAttachments != null ) {
                Iterator<String> textkeys = pTextAttachments.keySet().iterator();
                while ( textkeys.hasNext() ) {
                    String key = textkeys.next();
                    Object val = pTextAttachments.get(key);
                    if ( val != null ) {
                        MimeBodyPart part = new MimeBodyPart();
                        part.setContent(val.toString(), "text/plain");
                        part.setDisposition(MimeBodyPart.ATTACHMENT);
                        part.setDescription(key);
                        part.setFileName(key);
                        mp.addBodyPart(part);
                    }
                }
            }

            // add the html attachments
            if ( pHTMLAttachments != null ) {
                Iterator<String> htmlkeys = pHTMLAttachments.keySet().iterator();
                while ( htmlkeys.hasNext() ) {
                    String key = htmlkeys.next();
                    Object val = pHTMLAttachments.get(key);
                    if ( val != null ) {
                        MimeBodyPart part = new MimeBodyPart();
                        part.setContent(val.toString(), "text/html");
                        part.setDisposition(MimeBodyPart.ATTACHMENT);
                        part.setDescription(key);
                        part.setFileName(key);
                        mp.addBodyPart(part);
                    }
                }
            }

            // add the File attachments
            if ( pFiles != null ) {
                for ( int i = 0; i < pFiles.length; i++ ) {
                    MimeBodyPart part = new MimeBodyPart();
                    part.setDataHandler(new DataHandler(new FileDataSource(pFiles[i])));
                    part.setFileName(pFiles[i].getName());
                    mp.addBodyPart(part);
                }
            }

            msg.setContent(mp);

            // create the email event
            EmailEvent em = new EmailEvent(msg);

            // now send the event
            SMTPEmailSender sender = new SMTPEmailSender();
            sender.setEmailHandlerHostName(MAILHOST);
            sender.sendEmailEvent(em);
        } catch ( Exception e ) {
            log.info("Caught exception sending email: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * This method is used to send an email message that contains
     * several attachments.  This method is specifically designed to
     * accept a map of java.lang.Strings as content parts instead of
     * java.io.Files.  The key in the Map should be a String
     * representing the name that you would like to show for the
     * attached file.  The value in the Map should be a String
     * representing the contents of the attachment.  If you wish to
     * attach java.io.Files, use the static methods found in
     * atg.service.email.MimeMessageUtils.
     */
    public static void sendEmail(String pAddress,
                                 String pMsg,
                                 String pSubject,
                                 Map<String, Object> pTextAttachments,
                                 Map<String, Object> pHTMLAttachments,
                                 String pBodyEncoding) {
        sendEmail(
                pAddress, pMsg, pSubject, pTextAttachments, pHTMLAttachments, null, pBodyEncoding
        );
    }

    /**
     * This method is used to send an email message that contains
     * several attachments.  This method is specifically designed to
     * accept a map of java.lang.Strings as content parts instead of
     * java.io.Files.  The key in the Map should be a String
     * representing the name that you would like to show for the
     * attached file.  The value in the Map should be a String
     * representing the contents of the attachment.  If you wish to
     * attach java.io.Files, use the static methods found in
     * atg.service.email.MimeMessageUtils.
     */
    public static void sendEmail(String pAddress,
                                 String pMsg,
                                 String pSubject,
                                 Map<String, Object> pTextAttachments,
                                 Map<String, Object> pHTMLAttachments) {
        sendEmail(pAddress, pMsg, pSubject, pTextAttachments, pHTMLAttachments, "text/plain");
    }


    /**
     * This method is used to send an email message that contains
     * several attachments.  This method is specifically designed to
     * accept a map of java.lang.Strings as content parts instead of
     * java.io.Files.  The key in the Map should be a String
     * representing the name that you would like to show for the
     * attached file.  The value in the Map should be a String
     * representing the contents of the attachment.
     */
    public static void sendEmails(List<String> pAddresses,
                                  String pMsg,
                                  String pSubject,
                                  Map<String, Object> pTextAttachments,
                                  Map<String, Object> pHTMLAttachments,
                                  File[] pFiles,
                                  String pBodyEncoding) {
        // make sure addresses are valid
        if ( pAddresses == null || pAddresses.size() <= 0 ) {
            return;
        }

        // send emails
        Iterator<String> addresses = pAddresses.iterator();
        String address = null;
        while ( addresses.hasNext() ) {
            try {
                address = addresses.next();
                if ( address != null && address.trim().length() > 0 ) {
                    sendEmail(
                            address.trim(),
                            pMsg,
                            pSubject,
                            pTextAttachments,
                            pHTMLAttachments,
                            pFiles,
                            pBodyEncoding
                    );
                }
            } catch ( Exception e ) {
            }
        }
    }

    /**
     * This method is used to send an email message that contains
     * several attachments to multiple recipients.  This method is
     * specifically designed to accept a map of java.lang.Strings as
     * content parts instead of java.io.Files.  The key in the Map
     * should be a String representing the name that you would like to
     * show for the attached file.  The value in the Map should be a
     * String representing the contents of the attachment.
     */
    public static void sendEmails(List<String> pAddresses,
                                  String pMsg,
                                  String pSubject,
                                  Map<String, Object> pTextAttachments,
                                  Map<String, Object> pHTMLAttachments,
                                  String pBodyEncoding) {
        sendEmails(
                pAddresses, pMsg, pSubject, pTextAttachments, pHTMLAttachments, null, pBodyEncoding
        );
    }

    /**
     * This method is used to send an email message that contains
     * several attachments to multiple recipients.  The message will
     * have it's main body part encoded as "text/plain".  <br>This
     * method is specifically designed to accept a map of
     * java.lang.Strings as content parts instead of java.io.Files.
     * The key in the Map should be a String representing the name that
     * you would like to show for the attached file.  The value in the
     * Map should be a String representing the contents of the
     * attachment.  If you wish to attach java.io.Files, use the static
     * methods found in atg.service.email.MimeMessageUtils.
     */
    public static void sendEmails(List<String> pAddresses,
                                  String pMsg,
                                  String pSubject,
                                  Map<String, Object> pTextAttachments,
                                  Map<String, Object> pHTMLAttachments) {
        sendEmails(pAddresses, pMsg, pSubject, pTextAttachments, pHTMLAttachments, "text/plain");
    }

    // ======================== EXCEPTIONS =====================

    /**
     * this method returns a String representation of an Exception's stacktrace
     */
    public static String getStackTrace(Throwable pException) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        pException.printStackTrace(ps);
        ps.flush();
        return bos.toString();
    }

    // ===================== URL ACCESS ========================

    /**
     * this method returns the contents of the page specified as the
     * URL.  the URL should be a fully qualified request string.  for
     * example, http://rygar.atg.com:8880/some/directory/page.jhtml
     * <p/>
     * If the boolean parameter is set to true then this method will
     * throw an Exception if an error occurs; otherwise it will simply
     * return the contents of the exception.
     *
     * @throws MalformedURLException if URL is malformed & pThrow is true
     * @throws IOException           if error happens while reading and pThrow is true
     */
    public static String accessURL(String pUrl, boolean pThrow)
            throws MalformedURLException, IOException {
        URL url = null;
        StringBuffer results = new StringBuffer();
        BufferedReader in = null;
        InputStreamReader isr = null;
        try {
            url = new URL(pUrl);

            isr = new InputStreamReader(url.openStream());
            in = new BufferedReader(isr);
            String line = null;
            while ( (line = in.readLine()) != null ) {
                results.append(line + "\n");
            }
            return results.toString();
        } catch ( MalformedURLException e ) {
            if ( pThrow ) {
                throw e;
            } else {
                results.append(
                        "\nEncountered an unexpected error while trying to retrieve the configuration info."
                        +
                        "\nWhen the url "
                        + url
                        + " was requested, this error was received: \n"
                        + getStackTrace(e)
                        + "\n"
                );
            }
        } catch ( IOException ioe ) {
            if ( pThrow ) {
                throw ioe;
            } else {
                results.append(
                        "\nEncountered an unexpected error while trying to retrieve the configuration info."
                        +
                        "\nWhen the url "
                        + url
                        + " was requested, this error was received: \n"
                        + getStackTrace(ioe)
                        + "\n"
                );
            }
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( Exception e ) {
                }
            }
            if ( isr != null ) {
                try {
                    isr.close();
                } catch ( Exception e ) {
                }
            }
        }
        return results.toString();
    }

    /**
     * this method returns the contents of the page specified as the URL.  the
     * URL should be a fully qualified request string.  for example,
     * http://rygar.atg.com:8880/some/directory/page.jhtml
     * <p/>
     * Unlike it's sister method with the boolean parameter, this method will
     * not throw an exception.
     */
    public static String accessURL(String pUrl) {
        try {
            return accessURL(pUrl, false);
        } catch ( Exception e ) {
            return "\nEncountered an unexpected error while trying to retrieve the configuration info."
                   +
                   "\nWhen the url "
                   + pUrl
                   + " was requested, this error was received: \n"
                   + getStackTrace(e)
                   + "\n";
        }
    }

    // ==================== File IO ============================

    /**
     * Writes the byte array into the specified file.
     *
     * @param File   pFile the file to write to
     * @param byte[] the bytes to write
     *
     * @throws IOException if an error occurred opening or reading the file.
     */
    public static void writeFileBytes(File pFile, byte[] pBytes)
            throws IOException {
        if ( pBytes == null ) {
            pBytes = new byte[0];
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pFile);
            fos.write(pBytes);
        } catch ( IOException e ) {
            throw e;
        } finally {
            try {
                if ( fos != null ) {
                    fos.close();
                }
            } catch ( IOException exc ) {
            }
        }
    }

    /**
     * converts a delimiter separated String of file names into an
     * array and expands all System property variables in the Strings.
     * it does not check whether resolved file paths exist.
     *
     * @param String delimited string of files to be converted to array.
     * @param String delimiter string used to separated files
     *
     * @return String[] array of expanded paths
     * @throws Exception if files can't be resolved properly
     */
    public static String[] convertFileArray(String pFiles, String pDelimiter)
            throws Exception {
        return convertFileArray(pFiles, pDelimiter, null);
    }

    /**
     * converts a delimiter separated String of file names into an array
     * and expands all variables in the Strings.  it does not check whether
     * resolved file paths exist.
     *
     * @param String     delimited string of files to be converted to array.
     * @param String     delimiter string used to separated files
     * @param Properties optional primary mapping of key/value pairs to
     *                   substitute into file paths whererever the syntax <tt>{...}</tt>
     *                   is found.  If parameter is null, or mapping not found, then
     *                   System.getProperties() is checked.
     *
     * @return String[] array of expanded paths
     * @throws Exception if files can't be resolved properly
     */
    public static String[] convertFileArray(String pFiles,
                                            String pDelimiter,
                                            Properties pPrimaryMapping)
            throws Exception {
        if ( pDelimiter == null ) {
            pDelimiter = "";
        }
        StringTokenizer st = new StringTokenizer(pFiles, pDelimiter);
        List<String> files = new LinkedList<String>();
        while ( st.hasMoreTokens() ) {
            files.add(expand(st.nextToken(), pPrimaryMapping));
        }
        return (String[]) files.toArray(new String[files.size()]);
    }

    /**
     * expands all System property variables specified in the supplied
     * String using curly braces syntax <tt>{...}</tt> and returns the
     * resulting String.
     *
     * @param String the string to expand.
     *
     * @throws Exception if a System property resolves to null or if
     *                   the enclosing braces are not properly matched.
     */
    public static String expand(String pString)
            throws Exception {
        return expand(pString, null);
    }

    /**
     * expands all property variables specified in the supplied String
     * using curly braces syntax <tt>{...}</tt> and returns the
     * resulting String.  Property names inside the curly braces can be
     * either a simple String referring to a Java System property, such
     * as "SystemProperty.X", or can be in AppModuleResource format,
     * such as
     * "appModuleResource?moduleID=MyModule&resource=my/resource/file".
     *
     * @param String     the string to expand.
     * @param Properties an optional primary key/value mapping to use
     *                   for System property substitutions.  If param is null, or if
     *                   mapping not found, then System.getProperties().getProperty(xxx)
     *                   is used.
     *
     * @return String the expanded string.
     * @throws Exception if a System or AppModuleResource property
     *                   resolves to null or if the enclosing braces are not properly
     *                   matched.
     */
    public static String expand(String pString, Properties pPrimaryMapping)
            throws Exception {
        int idx = pString.indexOf("{");
        while ( idx != -1 ) {
            int end = pString.indexOf("}");
            if ( end == -1 ) {
                throw new Exception("Unclosed braces in String " + pString);
            }
            String pname = pString.substring(idx + 1, end);
            String prop = null;
            if ( pPrimaryMapping != null ) {
                prop = pPrimaryMapping.getProperty(pname);
            }
            if ( prop == null ) {
                if ( pname.startsWith("appModuleResource?") ) {
                    prop = resolveAppModuleResourceReference(pname).getPath();
                }
                // atg.dynamo.root and atg.dynamo.home are resolved specially
                // because of BigEar
                else if ( pname.equals(ROOT_VAR) ) {
                    prop = getDynamoRootDir().getPath();
                } else if ( pname.equals(HOME_VAR) ) {
                    prop = getDynamoHomeDir().getPath();
                } else {
                    prop = System.getProperty(pname);
                }
            }
            if ( prop == null ) {
                throw new Exception(
                        "System property '"
                        + pString.substring(idx + 1, end)
                        + "' is null.  String "
                        + pString
                        + " can not be resolved."
                );
            }

            pString = pString.substring(0, idx) + prop + pString.substring(end + 1);
            idx = pString.indexOf("{");
        }
        return pString;
    }

    // ===================== atg dynamo info ===================================

    /**
     * product module corresponding to ATG Dynamo
     */
    public static String ATGDYNAMO_PRODUCT_MODULE = "DPS";

    /**
     * specifies the name of the ATG Dynamo product module that will be
     * loaded if Dynamo is being used.
     */
    public static void setAtgDynamoProductModule(String pModule) {
        ATGDYNAMO_PRODUCT_MODULE = pModule;
    }

    /**
     * returns the name of the ATG Dynamo product module that will be
     * loaded if Dynamo is being used.
     */
    public static String getAtgDynamoProductModule() {
        return ATGDYNAMO_PRODUCT_MODULE;
    }

    /**
     * returns an AppModule corresponding to the ATG Dynamo if that
     * product is loaded.  If it isn't loaded then returns null.
     */
    public static AppModule getAtgDynamoModule() {
        // get all modules that were started with dynamo
        Iterator<?> modules = getAppLauncher().getModules().iterator();

        while ( modules.hasNext() ) {
            AppModule module = (AppModule) modules.next();
            if ( module.getName().equals(getAtgDynamoProductModule()) ) {
                return module;
            }
        }

        return null;
    }

    // ==================== atg j2ee server info ==============================

    /**
     * product module corresponding to ATG's J2EE Server
     */
    public static String ATGJ2EESERVER_PRODUCT_MODULE = "J2EEServer";

    /**
     * specifies the name of the ATG J2EE Server product module that
     * will be loaded if ATG's J2EE Server is being used.
     */
    public static void setAtgJ2eeServerProductModule(String pModule) {
        ATGJ2EESERVER_PRODUCT_MODULE = pModule;
    }

    /**
     * returns the name of the ATG J2EE Server product module that will
     * be loaded if ATG's J2EE Server is being used.
     */
    public static String getAtgJ2eeServerProductModule() {
        return ATGJ2EESERVER_PRODUCT_MODULE;
    }

    /**
     * returns an AppModule corresponding to the ATG J2EE Server if
     * that product is loaded.  If it isn't loaded then returns null.
     */
    public static AppModule getAtgJ2eeServerModule() {
        // get all modules that were started with dynamo
        Iterator<?> modules = getAppLauncher().getModules().iterator();

        while ( modules.hasNext() ) {
            AppModule module = (AppModule) modules.next();
            if ( module.getName().equals(getAtgJ2eeServerProductModule()) ) {
                return module;
            }
        }

        return null;
    }

    // ==================== application info ============================

    /**
     * possible application product modules that may be installed
     */
    public static String[] APPLICATION_PRODUCT_MODULES = { "ACA", "ABTest", "DCS-SO", "CAF" };

    /**
     * specifies the names of possible application product modules that
     * may be installed in Dyanmo.  used to help report on which
     * application modules are running.
     */
    public void setApplicationProductModules(String[] pModules) {
        APPLICATION_PRODUCT_MODULES = pModules;
    }

    /**
     * returns the names of possible application product modules that
     * may be installed in Dyanmo.  used to help report on which
     * application modules are running.  NOTE: This method should not
     * be called.  It is only provided so we can specify application
     * modules in a .properties file.  Java classes should call method
     * getApplicationModules().
     */
    public String[] getApplicationProductModules() {
        return APPLICATION_PRODUCT_MODULES;
    }

    /**
     * returns an array of AppModule items corresponding to the
     * currently running application products.
     */
    public static AppModule[] getApplicationModules() {
        List<AppModule> apps = new LinkedList<AppModule>();

        // get all modules that were started with dynamo
        Iterator<?> modules = getAppLauncher().getModules().iterator();

        while ( modules.hasNext() ) {
            AppModule module = (AppModule) modules.next();
            for ( int i = 0; i < APPLICATION_PRODUCT_MODULES.length; i++ ) {
                // in order to work around bug 80207, we allow a colon ":" in
                // the specified module names.  if a colon exists, the name
                // before the colon is the name of the module that would be
                // started if the application is running.  the name after the
                // colon is the module containing the MANIFEST.MF file with
                // build info.  if there is no colon, assume the two modules
                // are the same.
                int idx = APPLICATION_PRODUCT_MODULES[i].indexOf(":");
                if ( idx == -1 ) {
                    // no colon...
                    if ( (APPLICATION_PRODUCT_MODULES[i]).equals(module.getName()) ) {
                        apps.add(module);
                    }
                } else {
                    if ( APPLICATION_PRODUCT_MODULES[i].substring(0, idx)
                                                       .equals(module.getName()) ) {
                        // NOTE: getAppLauncher().getModule(...) will return a
                        // module as long as it exists; the module does not need
                        // to be running.
                        try {
                            AppModule mod = getAppLauncher().getModule(
                                    APPLICATION_PRODUCT_MODULES[i].substring(idx + 1)
                            );
                            log.info("\nMod: " + mod);
                            if ( mod != null ) {
                                apps.add(mod);
                            } else {
                                throw new Exception(
                                        APPLICATION_PRODUCT_MODULES[i].substring(
                                                idx + 1
                                        ) + " not found."
                                );
                            }
                        } catch ( Exception ale ) {
                            log.info(
                                    "*** WARNING [atg.junit.nucleus.TestUtils] "
                                    + "Can not resolve module '"
                                    + APPLICATION_PRODUCT_MODULES[i].substring(idx + 1)
                                    + "'. "
                                    + ale.getMessage()
                            );
                        }
                    }
                }
            }
        }

        return (AppModule[]) apps.toArray(new AppModule[apps.size()]);
    }

    // =========== generic AppModule info retrieval methods ====================

    private static AppLauncher mAppLauncher = null;

    /**
     * Returns the AppLauncher used to load this class.
     */
    private static AppLauncher getAppLauncher() {
        if ( mAppLauncher == null ) {
            mAppLauncher = AppLauncher.getAppLauncher(TestUtils.class);
        }
        return mAppLauncher;
    }

    /**
     * Retrieves a File resource from a Dynamo Module.  Note that the
     * module does not need to be started, it simply has to be
     * installed in the Dynamo.  Returned file is <u>not</u> verified
     * to exist.
     *
     * @param String pModuleName the name of the Dynamo module to look in.  e.g.
     *               "SystemTests.JSPTest"
     * @param String pResourceURI the URI of the File to get from the module.  e.g. "mite.xml"
     *
     * @return File the requested file.
     */
    public static File getModuleResourceFile(String pModuleName, String pResourceURI) {
        return getAppLauncher().getAppModuleManager().getResourceFile(pModuleName, pResourceURI);
    }

    /**
     * Resolves an appModuleResource reference by parsing the string
     * into its constituent ModuleID and ResourceURI.
     *
     * @param String pReference The AppModuleResource reference to resolve.  Expected to be of
     *               format:
     *               <br><tt>appModuleResource?moduleID=<i>moduleID</i>&resourceURI=<i>some/URI</i></tt>
     *
     * @return File the referenced module resource.
     * @throws IllegalArgumentException if the specified reference does not have the proper
     *                                  structure.
     */
    public static File resolveAppModuleResourceReference(String pReference) {
        // there's probably a standard utility method in Dynamo to do this
        // resolution, but i can't find it...
        String moduleID = null;
        String resourceURI = null;
        String ref = pReference;
        try {
            int idx = ref.indexOf("moduleID=");       // locate moduleID delimiter
            if ( idx == -1 ) {
                throw new Exception();
            }
            ref = ref.substring(idx + 9);  // strip up to and including 'moduleID='
            idx = ref.indexOf("&resourceURI="); // get index of resourceURI delimiter
            moduleID = ref.substring(0, idx);        // extract moduleID
            resourceURI = ref.substring(idx + 13);  // extract resourceURI
        } catch ( Throwable t ) {
            throw new IllegalArgumentException(
                    "Can not resolve appModuleReference. "
                    + "Illegal reference syntax: "
                    + pReference
            );
        }
        return getModuleResourceFile(moduleID, resourceURI);
    }

    /**
     * Retrieves a piece of information from the MANIFEST of the
     * supplied AppModule.  Returns null if the specified information
     * can't be found.
     */
    public static String getManifestInfo(AppModule pModule, String pEntry) {
        return getManifestInfo(pModule.getManifest(), pEntry);
    }

    /**
     * Logs a message using Nucleus.logInfo() if Nucleus is available.
     * Otherwise it logs using log.info()
     *
     * @param pMessage
     */
    public static void log(String pMessage) {
        Nucleus n = Nucleus.getGlobalNucleus();
        if ( n != null ) {
            n.logInfo(pMessage);
        } else {
            log.info(new java.util.Date() + ":" + pMessage);
        }
    }

    /**
     * Retrieves a piece of information from the specified Manifest file.
     * Returns null if the specified information can't be found.
     */
    public static String getManifestInfo(Manifest pManifest, String pEntry) {
        // if manifest or entry key is null return null...
        if ( pManifest == null || pEntry == null ) {
            return null;
        }

        if ( pManifest.getMainAttributes() == null ) {
            return null;
        } else {
            return pManifest.getMainAttributes().getValue(pEntry);
        }
    }

    /**
     * Returns the ATG product version ("ATG-Version") of the specified module.
     * Returns UNKNOWN_INFO if the product version can't be determined.
     */
    public static String getAtgVersion(AppModule pModule) {
        String version = getManifestInfo(pModule, "ATG-Version");
        if ( version != null ) {
            return version;
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the ATG product build number ("ATG-Build") of the
     * specified module.  Returns UNKNOWN_INFO if the build number
     * can't be determined.
     */
    public static String getAtgBuildNumber(AppModule pModule) {
        String build = getManifestInfo(pModule, "ATG-Build");
        if ( build != null ) {
            return build;
        } else {
            return UNKNOWN_INFO;
        }
    }

    /**
     * Returns the ATG patch version ("ATG-Patch-Version") of the
     * specified module.  Returns null if the module's version can be
     * determined, but a patch version can't.  Returns UNKNOWN_INFO if
     * neither the product or patch version can be determined.
     */
    public static String getAtgPatchVersion(AppModule pModule) {
        String version = getManifestInfo(pModule, "ATG-Patch-Version");
        if ( version != null ) {
            return version;
        } else if ( getAtgVersion(pModule).equals(UNKNOWN_INFO) ) {
            return UNKNOWN_INFO;
        } else {
            return null;
        }
    }

    /**
     * Returns the ATG patch build number ("ATG-Patch-Build") of the
     * specified module.  Returns null if the module's build number can
     * be determined, but a patch build number can't.  Returns
     * UNKNOWN_INFO if neither the product or patch build number can be
     * determined.
     */
    public static String getAtgPatchBuildNumber(AppModule pModule) {
        String build = getManifestInfo(pModule, "ATG-Patch-Build");
        if ( build != null ) {
            return build;
        } else if ( getAtgBuildNumber(pModule).equals(UNKNOWN_INFO) ) {
            return UNKNOWN_INFO;
        } else {
            return null;
        }
    }

    /**
     * Returns the ATG full product version ("ATG-Version-Full") of the
     * specified module.  Returns UNKNOWN_INFO if the full product
     * version can't be determined.
     */
    public static String getAtgFullVersion(AppModule pModule) {
        String version = getManifestInfo(pModule, "ATG-Version-Full");
        if ( version != null ) {
            return version;
        } else {
            return UNKNOWN_INFO;
        }
    }

    // ==================== Dynamo Environment Information =====================

    // some methods called on DynamoEnv are not available in older
    // versions of the class, so use reflection to maintain backward
    // compatibility.

    private static DynamoEnv mDynamoEnv = null;

    private static DynamoEnv dynamoEnv() {
        if ( mDynamoEnv == null ) {
            try {
                mDynamoEnv = (DynamoEnv) DynamoEnv.class.newInstance();
            } catch ( Throwable t ) {
            }
        }
        return mDynamoEnv;
    }

    /**
     * Returns true if Dynamo is running as BigEar; otherwise returns false.
     */
    public static boolean isBigEar() {
        Boolean isBigEar = (Boolean) invokeMethod(
                dynamoEnv(), "isBigEar", null, null, Boolean.FALSE
        );
        return isBigEar.booleanValue();
    }

    /**
     * Returns true if Dynamo is running as BigEar in standalone mode;
     * otherwise returns false.
     */
    public static boolean isBigEarStandalone() {
        Boolean isStandalone = (Boolean) invokeMethod(
                dynamoEnv(), "getStandaloneMode", null, null, Boolean.FALSE
        );
        return isStandalone.booleanValue();
    }

    /**
     * Returns true is Dynamo is running with liveconfig enabled;
     * otherwise returns false.
     */
    public static boolean isLiveconfig() {
        // 'isLiveconfig' is a new method in Koko (pr 88105).  try it, but
        // if that doesn't work try the 'getProperty' method that was used
        // for ATG 7.0.  finally, if that doesn't work just examine System
        // properties as a last check.
        Boolean isliveconfig = (Boolean) invokeMethod(
                dynamoEnv(), "isLiveconfig", null, null, null
        );
        if ( isliveconfig == null ) {
            // that method didn't work, so try this method - which should
            // work in ATG 7
            String[] args = { "atg.dynamo.liveconfig" };
            String propval = (String) invokeMethod(
                    dynamoEnv(), "getProperty", new Class[] { String.class }, args, null
            );
            if ( propval != null ) {
                isliveconfig = Boolean.valueOf("on".equalsIgnoreCase(propval));
            } else {
                isliveconfig = Boolean.valueOf(
                        "on".equalsIgnoreCase(
                                System.getProperty(
                                        "atg.dynamo.liveconfig"
                                )
                        )
                );
            }
        }
        if ( isliveconfig != null ) {
            return isliveconfig.booleanValue();
        } else {
            return false;
        }
    }

    public static Object invokeMethod(Object pObj,
                                      String pMethodName,
                                      Class<?>[] pSignature,
                                      Object[] pParams,
                                      Object pDefault) {
        Object returnval = null;
        try {
            Method meth = pObj.getClass().getMethod(pMethodName, pSignature);
            returnval = meth.invoke(pObj, pParams);
            //if ( isLoggingDebug() ) logDebug("Method '" + pMethodName + "'
            //invoked - return value: " + returnval);
        } catch ( Throwable t ) {
            //if ( isLoggingDebug() ) logDebug("Method '" + pMethodName + "'
            //could not be invoked.", t);
            returnval = pDefault;
        }
        return returnval;
    }

} // end of class
