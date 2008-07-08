/**
 * Copyright 2008 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import atg.adapter.gsa.GSARepository;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.test.AtgDustCase;

/**
 * 
 * Example test case to illustrate the usage of {@link AtgDustCase} built-in
 * database functionalities using on the fly created db's (bases on hsql
 * in-memory db) or against external existing databases.
 * 
 * <br/><br/>Based on {@link AtgDustCase}
 * 
 * 
 * @author robert
 * 
 */
public class SongsRepositoryTest extends AtgDustCase {

	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(SongsRepositoryTest.class);

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// make sure all needed files are at the configuration location.
		// "target/test-classes/config" is then promoted to the configuration
		// staging directory (that location is a maven controlled build location
		// and
		// therefore not part of the checked in source tree).
		copyConfigurationFiles(new String[] { "src/test/resources/config"
				.replace("/", File.separator) }, "target/test-classes/config"
				.replace("/", File.separator), ".svn");

		// Eventually set this one to 'true' to get more debug logging in your
		// console from your nucleus based components.
		setDebug(false);

	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Runs a test against an in-memory HSQL database
	 * 
	 * @throws Exception
	 */
	public void testWithInMemoryDb() throws Exception {

		// The actual test is quite generic. The only difference is the way the
		// repository is prepared by the prepareRepositoryTest method

		prepareRepository("/GettingStarted/SongsRepository",
				"/GettingStarted/songs.xml");

		songsRepositoryTest();
	}

	/**
	 * Example test with existing Database. This test is disabled by default
	 * (set to false/or not set in the env.properties) because the MySQL JDBC
	 * drivers (and the env.properties is configured to use mysql) are not
	 * included in the atg dust package.
	 * 
	 * To make use of this test, install a mysql-connector-java (mysql jdbc
	 * driver) into your .m2/repository, un-comment the mysql dependency in the
	 * pom.xml. Test data can be found in
	 * src/test/resources/config/GettingStarted/songs-data.xml.
	 * 
	 * 
	 * @throws Exception
	 */
	public void testWithExistingDb() throws Exception {

		Properties properties = new Properties();
		properties
				.load(new FileInputStream("src/test/resources/env.properties"));

		// a mechanism to disable/enable the repository test against an existing
		// database
		if (properties.getProperty("enabled") == null
				|| properties.getProperty("enabled").equalsIgnoreCase("false")) {
			return;
		}

		// The actual test is quite generic. The only difference is the way the
		// repository is prepared by the prepareRepositoryTest method

		prepareRepository("/GettingStarted/SongsRepository", properties, false,
				false, "/GettingStarted/songs.xml");

		songsRepositoryTest();
	}

	private void songsRepositoryTest() throws TransactionDemarcationException,
			RepositoryException, IOException {
		GSARepository songsRepository = (GSARepository) resolveNucleusComponent("/GettingStarted/SongsRepository");
		assertNotNull(songsRepository);

		final TransactionDemarcation td = new TransactionDemarcation();
		assertNotNull(td);

		try {
			// Start a new transaction
			td.begin(songsRepository.getTransactionManager());
			// Create a new artist
			MutableRepositoryItem artist = songsRepository.createItem("artist");
			artist.setPropertyValue("name", "joe");
			// Persist to the repository
			songsRepository.addItem(artist);
			// Try to get it back from the repository
			String id = artist.getRepositoryId();
			RepositoryItem retrievedArtist = songsRepository.getItem(id,
					"artist");

			assertEquals(artist, retrievedArtist);
		} finally {
			// End the transaction, roll-back to restore original database state
			td.end(true);
		}
	}

}