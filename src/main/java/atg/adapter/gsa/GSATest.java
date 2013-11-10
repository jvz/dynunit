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

package atg.adapter.gsa;

import atg.beans.DynamicPropertyDescriptor;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.servlet.NucleusServlet;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryPropertyDescriptor;
import atg.service.idgen.IdGenerator;
import atg.service.idgen.IdGeneratorException;
import atg.test.util.DBUtils;
import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.PropertyEditor;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

// TODO: port to JUnit4
/**
 * A basic GSA test which is expected to be extended than used directly.
 * Has several utility methods.
 *
 * @author adwivedi
 */
public class GSATest
        extends TestCase {

    private final transient Random random = new Random();

    private static Logger log = LogManager.getLogger();

    private HashMap<String, File> mConfigDir = new HashMap<String, File>();

    /**
     *
     */
    public GSATest() {
        super();
        // TODO Auto-generated constructor stub
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp()
            throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown()
            throws Exception {
        super.tearDown();
    }

    /**
     * Constructor for GSATest.
     *
     * @param arg0
     */
    public GSATest(String arg0) {
        super(arg0);
    }

    public File getConfigpath() {
        return getConfigpath(null);
    }

    /**
     * Returns the configpath for tests
     *
     * @return
     */
    File getConfigpath(String pConfigDirectory) {
        if ( mConfigDir.get(pConfigDirectory) == null ) {
            String configdirname = "config";
            String packageName = StringUtils.replace(
                    this.getClass().getPackage().getName(), '.', "/"
            );
            if ( pConfigDirectory != null ) {
                configdirname = pConfigDirectory;
            }

            String configFolder = packageName + "/data/" + configdirname;

            URL dataURL = this.getClass().getClassLoader().getResource(configFolder);
            // Mkdir
            if ( dataURL == null ) {
                URL root = this.getClass().getClassLoader().getResource(packageName);

                File f = new File(root.getFile());
                File f2 = new File(f, "/data/" + configdirname);
                f2.mkdirs();
                dataURL = this.getClass().getClassLoader().getResource(configFolder);
            }

            mConfigDir.put(pConfigDirectory, new File(dataURL.getFile()));
        }
        System.setProperty(
                "atg.configpath", ((File) mConfigDir.get(pConfigDirectory)).getAbsolutePath()
        );
        return (File) mConfigDir.get(pConfigDirectory);
    }

    /**
     * Create a repository in the given configpath using the given repository definitions (Absolute
     * paths)
     * connecting to the db whose properties are specified in pDBProperties @see DBUtils
     * Method pMethodName is invoked with the GSARepository passed to it as a parameter.
     *
     * @param pConfigPathWhereToCreateTheRepository
     *
     * @param definitionFiles
     * @param pDBProperties
     * @param pMethodName
     *
     * @throws Exception
     * @throws Exception
     */
    void setUpAndTest(File pConfigPathWhereToCreateTheRepository,
                      String[] definitionFiles,
                      Properties pDBProperties,
                      String pMethodName)
            throws Exception {
        String repositoryComponentPath = "/" + getName() + "Repository";
        GSATestUtils.getGSATestUtils().initializeMinimalConfigpath(
                pConfigPathWhereToCreateTheRepository,
                repositoryComponentPath,
                definitionFiles,
                pDBProperties,
                null,
                null,
                null,
                true
        );
        Nucleus n = startNucleus(pConfigPathWhereToCreateTheRepository);
        GSARepository r = (GSARepository) n.resolveName(repositoryComponentPath);
        try {
            getClass().getMethod(pMethodName, new Class[] { GSARepository.class })
                    .invoke(this, new Object[] { r });
        } catch ( NoSuchMethodError e ) {
            throw new AssertionError(
                    "Please declare a method with name "
                    + pMethodName
                    + " in your class. It must take an atg.adapter.gsa.GSARepository as the only parameter."
            );
        } finally {
            // if it were null a NPE would have occurred at the earlier dereference
            //if(n != null)
            n.stopService();
        }
    }


    /**
     * Createa a file using reasonable defaults.
     * Your definition file should exist in the same package as the test and should be
     * names <test_name>Repository.xml. Configpath is assumed to be what is returned
     *
     * @param pMethodName
     *
     * @throws Exception
     * @throws Exception
     */
    protected void setUpAndTest(String pMethodName)
            throws Exception {
        File configPathWhereToCreateTheRepository = getConfigpath(null);
        String packageName = StringUtils.replace(
                this.getClass().getPackage().getName(), '.', "/"
        );
        String fileName = packageName + "/" + getName() + "Repository.xml";
        URL defaultDefinitionFile = getClass().getResource("/" + fileName);
        if ( defaultDefinitionFile == null ) {
            throw new AssertionError(
                    "DUDE, I need a file called : " + fileName + " to start a GSA repository from. "
            );
        }
        String[] definitionFiles = new String[] { fileName };
        Properties DBProperties = DBUtils.getHSQLDBInMemoryDBConnection();
        setUpAndTest(
                configPathWhereToCreateTheRepository, definitionFiles, DBProperties, pMethodName
        );
    }

    /**
     * Starts Nucleus using the given config directory
     *
     * @param configpath
     *
     * @return
     */
    public static Nucleus startNucleus(File configpath) {

        return startNucleus(configpath.getAbsolutePath());
    }

    /**
     * Starts Nucleus given an array of configpath entries
     *
     * @param configpathStr
     *
     * @return
     */
    public static Nucleus startNucleus(String configpathStr) {
        System.setProperty("atg.dynamo.license.read", "true");
        System.setProperty("atg.license.read", "true");
        NucleusServlet.addNamingFactoriesAndProtocolHandlers();
        return Nucleus.startNucleus(new String[] { configpathStr });
    }

    /**
     * @param props
     *
     * @return
     * @throws Exception
     * @throws SQLException
     */
    protected DBUtils initDB(Properties props)
            throws Exception, SQLException {
        return new DBUtils(
                props.getProperty("URL"),
                props.getProperty("driver"),
                props.getProperty("user"),
                props.getProperty("password")
        );
    }

    /**
     * A Dummy test so smokestack won't report this
     * class as a failure.
     * It expects that all *Test.class files have
     * at least one test.
     */
    public final void testDummy() {

    }

    /**
     * @param pGSARepository
     * @param descName
     *
     * @return
     * @throws RepositoryException
     */
    protected MutableRepositoryItem createDummyItem(GSARepository pGSARepository,
                                                    String descName,
                                                    String pID)
            throws RepositoryException {
        GSAItemDescriptor descriptor = (GSAItemDescriptor) pGSARepository.getItemDescriptor(descName);
        MutableRepositoryItem item = null;
        boolean compoundPrimaryKey = descriptor.getPrimaryTable().getIdColumnCount() > 1;
        if ( pID == null || pID.trim().length() == 0 ) {
            if ( compoundPrimaryKey ) {
                item = pGSARepository.createItem(
                        getNewCompoundId(pGSARepository, descriptor), descName
                );
            } else {
                item = pGSARepository.createItem(descName);
            }
        } else {
            item = pGSARepository.createItem(pID, descName);
        }
        RepositoryPropertyDescriptor[] propDescriptors = (RepositoryPropertyDescriptor[]) descriptor
                .getPropertyDescriptors();
        for ( int j = 0; j < propDescriptors.length; j++ ) {
            RepositoryPropertyDescriptor propertyDescriptor = propDescriptors[j];
            if ( propertyDescriptor.isWritable()
                 && !propertyDescriptor.isIdProperty()
                 && propertyDescriptor.isRequired() ) {
                if ( propertyDescriptor.isCollectionOrMap() ) {
                } else {

                    Object dummyPropertyValue = generateDummyValue(propertyDescriptor);
                    if ( dummyPropertyValue != null ) {
                        item.setPropertyValue(
                                propertyDescriptor.getName(), dummyPropertyValue
                        );
                    }
                }
            }
        }
        return item;
    }

    /**
     * Get a id suitable for creating items of this type. We use out
     * <code>Repository</code>'s <code>IdGenerator</code>.
     *
     * @return a new id, which is unique across all items in this
     *         repository with this item descriptor.
     * @throws RepositoryException if there is trouble creating the id
     */
    GSAId getNewCompoundId(GSARepository r, GSAItemDescriptor desc)
            throws RepositoryException {
        // make sure we have a repository
        if ( r == null ) {
            return null;
        }

        // get the generator to use
        IdGenerator gen = r.getIdGenerator();
        if ( gen == null ) {
            return null;
        }

        Class<?>[] types = desc.getIdTypes();
        String[] idSpaceNames = desc.getIdSpaceNames();
        Object[] newId = new Object[types.length];

        if ( idSpaceNames.length != types.length ) {
            throw new RepositoryException("No ID SPACES ! " + desc.getItemDescriptorName());
        }

        // generate an id in our id space and return it
        try {
            for ( int i = 0; i < types.length; i++ ) {
                if ( types[i] == String.class ) {
                    if ( i > 0 ) {
                        newId[i] = "dummyIdPart";
                    } else {
                        newId[i] = gen.generateStringId(idSpaceNames[i]);
                    }
                } else {
                    long val = gen.generateLongId(idSpaceNames[i]);
                    if ( types[i] == Long.class ) {
                        newId[i] = Long.valueOf(val);
                    } else if ( types[i] == Float.class ) {
                        newId[i] = Float.valueOf((float) val);
                    } else if ( types[i] == Double.class ) {
                        newId[i] = Double.valueOf((float) val);
                    } else if ( types[i] == java.sql.Timestamp.class ) {
                        newId[i] = new java.sql.Timestamp(val);
                    } else if ( types[i] == java.util.Date.class ) {
                        newId[i] = new java.util.Date(val);
                    } else {
                        newId[i] = Integer.valueOf((int) val);
                    }
                }
            }
        } catch ( IdGeneratorException ie ) {
            throw new RepositoryException(ie);
        }

        return desc.generateGSAId(newId);
    }

    @SuppressWarnings("unchecked")
    Object generateDummyValue(RepositoryPropertyDescriptor propertyDescriptor) {
        if ( getEnumeratedValues(propertyDescriptor) != null ) {
            return null;// ignore enums for now.
        }

        if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.String.class
        ) ) {
            return generateString();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Integer.class
        ) ) {
            return generateInteger();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Boolean.class
        ) ) {
            return generateBoolean();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Byte.class
        ) ) {
            return generateByte();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Short.class
        ) ) {
            return generateShort();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Long.class
        ) ) {
            return generateLong();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Float.class
        ) ) {
            return generateFloat();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.lang.Double.class
        ) ) {
            return generateDouble();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                (new byte[0]).getClass()
        ) )//BINARY
        {
            return null;
            //                    return generateBinary();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.sql.Timestamp.class
        ) ) {
            return generateTimestamp();
        } else if ( propertyDescriptor.getPropertyType().isAssignableFrom(
                java.sql.Date.class
        ) ) {
            return generateDate();
        }
        return null;

    }

    /**
     * Returns the set of enumerated values, or null if there are none
     */
    String[] getEnumeratedValues(DynamicPropertyDescriptor pDescriptor) {
        if ( pDescriptor == null ) {
            return null;
        }
        PropertyEditor pe = getPropertyEditor(pDescriptor);
        String[] ret = (pe == null) ? null : pe.getTags();

        // make sure it's not just a boolean value
        Class<?> type = pDescriptor.getPropertyType();
        if ( (type == Boolean.class || type == Boolean.TYPE)
             && ret != null
             && ret.length == 2
             && (("true".equals(ret[0]) && "false".equals(ret[1])) || ("false".equals(ret[0])
                                                                       && "true".equals(ret[1]))) ) {
            return null;
        } else {
            return ret;
        }
    }

    /**
     * Returns an instance of the property editor, null if there is no
     * property editor
     */
    PropertyEditor getPropertyEditor(DynamicPropertyDescriptor pDescriptor) {
        if ( pDescriptor == null ) {
            return null;
        }
        Class<?> peclass = pDescriptor.getPropertyEditorClass();
        if ( peclass == null ) {
            return pDescriptor.getUIPropertyEditor();
        } else {
            Object peinst = null;
            try {
                peinst = peclass.newInstance();
            } catch ( InstantiationException e ) {
                log.catching(Level.ERROR, e);
            } catch ( IllegalAccessException e ) {
                log.catching(Level.ERROR, e);
            }
            if ( peinst instanceof PropertyEditor ) {
                return (PropertyEditor) peinst;
            } else {
                return null;
            }
        }
    }

    /**
     * @return
     */
    private Object generateDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * @return
     */
    private Object generateTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
    //  /**
    //   * @return
    //   */
    //  private Object generateBinary() {
    //    byte[] bytes = new byte[100];
    //    Random random = new Random();
    //    random.nextBytes(bytes);
    //    return bytes;
    //  }

    /**
     * @return
     */
    private Object generateDouble() {
        return new Double(random.nextDouble());
    }

    /**
     * @return
     */
    private Object generateInteger() {
        return Integer.valueOf(random.nextInt(32768));
    }

    /**
     * @return
     */
    private Object generateFloat() {
        return new Float(random.nextFloat());
    }

    /**
     * @return
     */
    private Object generateLong() {
        return Long.valueOf(random.nextInt(32278));
    }

    /**
     * @return
     */
    private Object generateShort() {
        return Short.valueOf((short) (random.nextInt(100)));
    }

    /**
     * @return
     */
    private Object generateByte() {
        byte[] bytes = new byte[1];
        random.nextBytes(bytes);
        return Byte.valueOf(bytes[0]);
    }

    /**
     * @return
     */
    private Object generateBoolean() {
        return Boolean.valueOf(random.nextBoolean());
    }

    /**
     * @return
     */
    private Object generateString() {

        return "DUMMY STRING " + generateInteger();
    }


}
