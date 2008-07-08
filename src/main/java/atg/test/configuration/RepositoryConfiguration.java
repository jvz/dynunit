/**
 * 
 */
package atg.test.configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import atg.adapter.gsa.InitializingGSA;
import atg.adapter.gsa.event.GSAEventServer;
import atg.dtm.TransactionDemarcationLogging;
import atg.dtm.TransactionManagerImpl;
import atg.dtm.UserTransactionImpl;
import atg.service.idgen.SQLIdGenerator;
import atg.service.jdbc.FakeXADataSource;
import atg.service.jdbc.MonitoredDataSource;
import atg.test.util.FileUtil;

/**
 * <i>This class is a merger of atg.test.util.DBUtils and
 * atg.adapter.gsa.GSATestUtils. The result will hopefully be a class that just
 * has the bare minimums needed for testing against an existing and/or in-memory
 * database.</i>
 * <p>
 * This class will created all properties files needed for repository based
 * tests.
 * </p>
 * 
 * @author robert
 * 
 */
public final class RepositoryConfiguration {

	// TODO-1 []: re-add versioned repository support?
	// TODO-2 []: better/more uniform way of handling properties file creation

	protected String isDebug = Boolean.FALSE.toString();

	protected final Map<String, String> settings = new HashMap<String, String>();

	private static Logger log = Logger.getLogger(RepositoryConfiguration.class);

	public void setDebug(final boolean isDebug) {
		this.isDebug = Boolean.toString(isDebug);
	}

	public RepositoryConfiguration() {
		super();
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	public void createPropertiesByConfigurationLocation(final File root)
			throws IOException {
		this.createTransactionManager(root);
		this.createUserTransaction(root);
		this.createIdGenerator(root);
		this.createIdSpaces(root);
		this.createSQLRepositoryEventServer(root);
		this.createJtdDataSource(root);

		log.info("Created repository configuration fileset");
	}

	/**
	 * 
	 * @param root
	 * @param jdbcSettings
	 * @throws IOException
	 */
	public void createFakeXADataSource(final File root,
			Map<String, String> jdbcSettings) throws IOException {

		// TODO: Something expects the url property name in upper case... still
		// have
		// to investigate.
		jdbcSettings.put("URL", jdbcSettings.get("url"));

		// remove the lower case url key/value pair so the generated
		// FakeXADataSource.properties only contains the upper case URL
		// key/value.
		jdbcSettings.remove("url");

		jdbcSettings.put("transactionManager",
				"/atg/dynamo/transaction/TransactionManager");

		FileUtil.createPropertyFile("FakeXADataSource", new File(root
				.getAbsolutePath()
				+ "/atg/dynamo/service/jdbc"), FakeXADataSource.class,
				jdbcSettings);

		// restore the settings state (re-add url and remove URL)
		jdbcSettings.put("url", jdbcSettings.get("URL"));
		jdbcSettings.remove("URL");
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createJtdDataSource(final File root) throws IOException {
		this.settings.clear();
		settings.put("dataSource", "/atg/dynamo/service/jdbc/FakeXADataSource");
		settings.put("transactionManager",
				"/atg/dynamo/transaction/TransactionManager");
		settings.put("min", "10");
		settings.put("max", "20");
		settings.put("blocking", "true");
		settings.put("loggingSQLWarning", isDebug);
		settings.put("loggingSQLInfo", isDebug);
		settings.put("loggingSQLDebug", isDebug);

		FileUtil.createPropertyFile("JTDataSource", new File(root
				.getAbsolutePath()
				+ "/atg/dynamo/service/jdbc"), MonitoredDataSource.class,
				settings);
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createIdGenerator(final File root) throws IOException {
		this.settings.clear();
		settings.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");
		settings.put("transactionManager",
				"/atg/dynamo/transaction/TransactionManager");
		settings.put("XMLToolsFactory",
				"/atg/dynamo/service/xml/XMLToolsFactory");
		FileUtil.createPropertyFile("IdGenerator", new File(root
				.getAbsolutePath()
				+ "/atg/dynamo/service/"), SQLIdGenerator.class, settings);
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createIdSpaces(final File root) throws IOException {
		final String idspaces = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE id-spaces SYSTEM \"http://www.atg.com/dtds/idgen/idgenerator_1.0.dtd\"><id-spaces><id-space name=\"__default__\" seed=\"1\" batch-size=\"100000\"/><id-space name=\"jms_msg_ids\" seed=\"0\" batch-size=\"10000\"/><id-space name=\"layer\" seed=\"0\" batch-size=\"100\"/></id-spaces>";
		final File idspacesFile = new File(root.getAbsolutePath()
				+ "/atg/dynamo/service/idspaces.xml");

		idspacesFile.delete();
		idspacesFile.getParentFile().mkdirs();
		idspacesFile.createNewFile();
		FileWriter out = new FileWriter(idspacesFile);
		out.write(idspaces);
		out.write("\n");
		out.flush();
		out.close();
	}

	/**
	 * 
	 * @param root
	 * @param repositoryPath
	 * @param droptables
	 *            <code>true</code> then existing tables will be dropped after
	 *            the test run, if <code>false</code> then leave the existing
	 *            tables alone
	 * @param createTables
	 *            if set to <code>true</code> all non existing tables needed for
	 *            the current test run will be created, if set to
	 *            <code>false</code> this class expects all needed tables for
	 *            this test run are already created
	 * @param definitionFiles
	 * @throws IOException
	 */
	public void createRepositoryConfiguration(final File root,
			final String repositoryPath, final boolean droptables,
			final boolean createTables, final String... definitionFiles)
			throws IOException {

		this.settings.clear();

		final StringBuilder defFiles = new StringBuilder();
		for (int i = 0; i < definitionFiles.length; i++) {
			defFiles.append("/" + definitionFiles[i]);
		}
		settings.put("definitionFiles", defFiles.toString());

		settings.put("XMLToolsFactory",
				"/atg/dynamo/service/xml/XMLToolsFactory");
		settings.put("transactionManager",
				"/atg/dynamo/transaction/TransactionManager");
		settings.put("idGenerator", "/atg/dynamo/service/IdGenerator");
		settings.put("dataSource", "/atg/dynamo/service/jdbc/JTDataSource");

		settings.put("lockManager", "/atg/dynamo/service/ClientLockManager");
		settings.put("idspaces", "/atg/dynamo/service/idspaces.xml");
		settings.put("groupContainerPath", "/atg/registry/RepositoryGroups");
		settings.put("restartingAfterTableCreation", "false");
		settings.put("createTables", Boolean.toString(createTables));
		settings.put("loggingError", "true");
		settings.put("loggingDebug", isDebug);
		settings.put("loggingCreateTables", isDebug);
		settings.put("debugLevel", "7");

		// InitializingGSA specific properties
		settings.put("dropTablesIfExist", Boolean.toString(droptables));
		settings.put("dropTablesAtShutdown", Boolean.toString(droptables));
		settings.put("stripReferences", "true");
		final int endIndex = repositoryPath.lastIndexOf("/");
		final String repositoryDir = repositoryPath.substring(0, endIndex);
		final String repositoryName = repositoryPath.substring(endIndex + 1,
				repositoryPath.length());
		final File newRoot = new File(root, repositoryDir);
		newRoot.mkdirs();
		FileUtil.createPropertyFile(repositoryName, newRoot,
				InitializingGSA.class, settings);
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createSQLRepositoryEventServer(final File root)
			throws IOException {
		this.settings.clear();
		settings.put("handlerCount", "0");
		FileUtil.createPropertyFile("SQLRepositoryEventServer", new File(root
				.getAbsolutePath()
				+ "/atg/dynamo/server"), GSAEventServer.class, settings);
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createTransactionManager(final File root) throws IOException {
		this.settings.clear();
		settings.put("loggingDebug", isDebug);
		final File newRoot = new File(root, "/atg/dynamo/transaction");
		newRoot.mkdirs();
		FileUtil.createPropertyFile("TransactionDemarcationLogging", newRoot,
				TransactionDemarcationLogging.class, settings);

		FileUtil.createPropertyFile("TransactionManager", newRoot,
				TransactionManagerImpl.class, settings);
	}

	/**
	 * 
	 * @param root
	 * @throws IOException
	 */
	private void createUserTransaction(final File root) throws IOException {
		this.settings.clear();
		settings.put("transactionManager",
				"/atg/dynamo/transaction/TransactionManager");
		FileUtil
				.createPropertyFile("UserTransaction", new File(root,
						"/atg/dynamo/transaction"), UserTransactionImpl.class,
						settings);
	}

}
