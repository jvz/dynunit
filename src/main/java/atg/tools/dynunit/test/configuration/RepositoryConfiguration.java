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

import atg.adapter.gsa.event.GSAEventServer;
import atg.dtm.TransactionDemarcationLogging;
import atg.dtm.TransactionManagerImpl;
import atg.dtm.UserTransactionImpl;
import atg.service.idgen.SQLIdGenerator;
import atg.service.jdbc.FakeXADataSource;
import atg.service.jdbc.MonitoredDataSource;
import atg.tools.dynunit.adapter.gsa.InitializingGSA;
import atg.tools.dynunit.test.util.FileUtil;
import atg.tools.dynunit.util.ComponentUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <i>This class is a merger of atg.tools.dynunit.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.</i>
 * <p>
 * This class will created all properties files needed for repository based
 * tests.
 * </p>
 *
 * @author robert
 */
public final class RepositoryConfiguration
        extends ConfigurationProvider {

    // TODO: re-add versioned repository support?
    // TODO: better/more uniform way of handling properties file creation

    private static final Logger logger = LogManager.getLogger();

    private static final String TX_MANAGER = "/atg/dynamo/transaction/TransactionManager";
    private static final String JT_DATA_SOURCE = "/atg/dynamo/service/jdbc/JTDataSource";
    private static final String XA_DATA_SOURCE = "/atg/dynamo/service/jdbc/FakeXADataSource";
    private static final String XML_TOOLS_FACTORY = "/atg/dynamo/service/xml/XMLToolsFactory";

    private File atgDynamo;
    private File atgTransaction;
    private File atgService;
    private File atgJdbc;
    private File atgServer;

    @Deprecated
    private final Map<String, String> settings = new HashMap<String, String>();

    private String debug() {
        return Boolean.toString(isDebug());
    }

    private void initPaths() {
        if (atgDynamo == null) {
            atgDynamo = new File(getRoot(), "atg" + File.separator + "dynamo");
            atgTransaction = new File(atgDynamo, "transaction");
            atgService = new File(atgDynamo, "service");
            atgJdbc = new File(atgService, "jdbc");
            atgServer = new File(atgDynamo, "server");
        }
    }

    public void createPropertiesByConfigurationLocation()
            throws IOException {
        logger.entry();
        initPaths();
        createTransactionManager();
        createUserTransaction();
        createIdGenerator();
        createIdSpaces();
        createSQLRepositoryEventServer();
        createJTDataSource();

        logger.info("Created repository configuration fileset");
        logger.exit();
    }

    public void createFakeXADataSource(@NotNull final Properties properties)
            throws IOException {
        logger.entry(properties);
        initPaths();
        properties.setProperty("transactionManager", TX_MANAGER);
        ComponentUtil.newComponent(atgJdbc, FakeXADataSource.class, properties);
        logger.exit();
    }

    public void createRepository(final String repositoryComponent,
                                 final boolean dropTables,
                                 final boolean createTables,
                                 final String... definitionFiles)
            throws IOException {
        logger.entry(repositoryComponent, dropTables, createTables, definitionFiles);
        final Properties properties = new Properties();
        properties.setProperty("definitionFiles", StringUtils.join(definitionFiles, ','));
        properties.setProperty("XMLToolsFactory", XML_TOOLS_FACTORY);
        properties.setProperty("transactionManager", TX_MANAGER);
        properties.setProperty("idGenerator", "/atg/dynamo/service/IdGenerator");
        properties.setProperty("dataSource", JT_DATA_SOURCE);
        properties.setProperty("lockManager", "/atg/dynamo/service/ClientLockManager");
        properties.setProperty("idspaces", "/atg/dynamo/service/idspaces.xml");
        properties.setProperty("groupContainerPath", "/atg/registry/RepositoryGroups");
        properties.setProperty("restartingAfterTableCreation", "false");
        properties.setProperty("createTables", Boolean.toString(createTables));
        properties.setProperty("loggingDebug", debug());
        properties.setProperty("loggingCreateTables", debug());
        properties.setProperty("debugLevel", "7");

        // InitializingGSA-specific
        properties.setProperty("dropTablesIfExist", Boolean.toString(dropTables));
        properties.setProperty("dropTablesAtShutdown", Boolean.toString(dropTables));
        properties.setProperty("stripReferences", "true");

        final int lastSlash = repositoryComponent.lastIndexOf('/');
        final String repositoryDirectoryName = repositoryComponent.substring(0, lastSlash)
                                                                  .replace('/', File.separatorChar);
        final String repositoryName = repositoryComponent.substring(lastSlash + 1);
        final File repositoryDirectory = new File(getRoot(), repositoryDirectoryName);
        ComponentUtil.newComponent(repositoryDirectory, repositoryName, InitializingGSA.class, properties);
        logger.exit();
    }

    private void createTransactionManager()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("loggingDebug", debug());
        ComponentUtil.newComponent(atgTransaction, TransactionDemarcationLogging.class, properties);
        ComponentUtil.newComponent(atgTransaction, TransactionManagerImpl.class, properties);
        logger.exit();
    }

    private void createUserTransaction()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("transactionManager", TX_MANAGER);
        ComponentUtil.newComponent(atgTransaction, UserTransactionImpl.class, properties);
        logger.exit();
    }

    private void createIdGenerator()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("dataSource", JT_DATA_SOURCE);
        properties.setProperty("transactionManager", TX_MANAGER);
        properties.setProperty("XMLToolsFactory", XML_TOOLS_FACTORY);
        ComponentUtil.newComponent(atgService, "IdGenerator", SQLIdGenerator.class, properties);
        logger.exit();
    }

    private void createIdSpaces()
            throws IOException {
        logger.entry();
        FileUtils.copyFileToDirectory(getIdSpacesTemplate(), atgService);
        logger.exit();
    }

    // FIXME: load this file better
    private File getIdSpacesTemplate() {
        logger.entry();
        return logger.exit(new File("src/main/resources/atg/tools/dynunit/idspaces.xml"));
    }

    private void createSQLRepositoryEventServer()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("handlerCount", "0");
        ComponentUtil.newComponent(atgServer, "SQLRepositoryEventServer", GSAEventServer.class, properties);
        logger.exit();
    }

    private void createJTDataSource()
            throws IOException {
        logger.entry();
        final Properties properties = new Properties();
        properties.setProperty("dataSource", XA_DATA_SOURCE);
        properties.setProperty("transactionManager", TX_MANAGER);
        properties.setProperty("min", "10");
        properties.setProperty("max", "20");
        properties.setProperty("blocking", "true");
        properties.setProperty("loggingSQLWarning", debug());
        properties.setProperty("loggingSQLInfo", debug());
        properties.setProperty("loggingSQLDebug", debug());
        ComponentUtil.newComponent(atgJdbc, "JTDataSource", MonitoredDataSource.class, properties);
        logger.exit();
    }

    @Deprecated
    public void createFakeXADataSource(@NotNull final File root, Map<String, String> jdbcSettings)
            throws IOException {

        // TODO: Something expects the url property name in upper case... still
        // have
        // to investigate.
        jdbcSettings.put("URL", jdbcSettings.get("url"));

        // remove the lower case url key/value pair so the generated
        // FakeXADataSource.properties only contains the upper case URL
        // key/value.
        jdbcSettings.remove("url");

        jdbcSettings.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );

        FileUtil.createPropertyFile(
                "FakeXADataSource", new File(
                root.getAbsolutePath() + "/atg/dynamo/service/jdbc"
        ), FakeXADataSource.class, jdbcSettings
        );

        // restore the settings state (re-add url and remove URL)
        jdbcSettings.put("url", jdbcSettings.get("URL"));
        jdbcSettings.remove("URL");
    }

    /**
     * @param root
     * @param repositoryPath
     * @param droptables
     *         <code>true</code> then existing tables will be dropped after
     *         the test run, if <code>false</code> then leave the existing
     *         tables alone
     * @param createTables
     *         if set to <code>true</code> all non existing tables needed for
     *         the current test run will be created, if set to
     *         <code>false</code> this class expects all needed tables for
     *         this test run are already created
     * @param definitionFiles
     *
     * @throws IOException
     */
    @Deprecated
    public void createRepositoryConfiguration(final File root,
                                              final String repositoryPath,
                                              final boolean droptables,
                                              final boolean createTables,
                                              final String... definitionFiles)
            throws IOException {

        this.settings.clear();

        final StringBuilder defFiles = new StringBuilder();
        for (String definitionFile : definitionFiles) {
            defFiles.append("/").append(definitionFile);
        }
        settings.put("definitionFiles", defFiles.toString());

        settings.put(
                "XMLToolsFactory", "/atg/dynamo/service/xml/XMLToolsFactory"
        );
        settings.put(
                "transactionManager", "/atg/dynamo/transaction/TransactionManager"
        );
        settings.put("idGenerator", "/atg/dynamo/service/IdGenerator");
        settings.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");

        settings.put("lockManager", "/atg/dynamo/service/ClientLockManager");
        settings.put("idspaces", "/atg/dynamo/service/idspaces.xml");
        settings.put("groupContainerPath", "/atg/registry/RepositoryGroups");
        settings.put("restartingAfterTableCreation", "false");
        settings.put("createTables", Boolean.toString(createTables));
        settings.put("loggingError", "true");
        settings.put("loggingDebug", debug());
        settings.put("loggingCreateTables", debug());
        settings.put("debugLevel", "7");

        // InitializingGSA specific properties
        settings.put("dropTablesIfExist", Boolean.toString(droptables));
        settings.put("dropTablesAtShutdown", Boolean.toString(droptables));
        settings.put("stripReferences", "true");
        final int endIndex = repositoryPath.lastIndexOf("/");
        final String repositoryDir = repositoryPath.substring(0, endIndex);
        final String repositoryName = repositoryPath.substring(
                endIndex + 1, repositoryPath.length()
        );
        final File newRoot = new File(root, repositoryDir);
        newRoot.mkdirs();
        FileUtil.createPropertyFile(
                repositoryName, newRoot, InitializingGSA.class, settings
        );
    }

}
