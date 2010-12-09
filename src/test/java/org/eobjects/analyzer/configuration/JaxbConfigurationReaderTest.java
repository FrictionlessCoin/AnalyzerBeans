/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.configuration;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.job.concurrent.SingleThreadedTaskRunner;
import org.eobjects.analyzer.reference.Dictionary;
import org.eobjects.analyzer.reference.ReferenceDataCatalog;
import org.eobjects.analyzer.reference.StringPattern;
import org.eobjects.analyzer.reference.SynonymCatalog;
import org.junit.Assert;

import dk.eobjects.metamodel.DataContext;

public class JaxbConfigurationReaderTest extends TestCase {

	private final JaxbConfigurationReader reader = new JaxbConfigurationReader();
	private DatastoreCatalog _datastoreCatalog;

	public void testValidConfiguration() throws Exception {
		AnalyzerBeansConfiguration configuration = reader.create(new File(
				"src/test/resources/example-configuration-valid.xml"));

		DatastoreCatalog datastoreCatalog = getDataStoreCatalog(configuration);
		assertEquals("[mydb_jndi, persons_csv, composite_datastore, my database]",
				Arrays.toString(datastoreCatalog.getDatastoreNames()));

		assertTrue(configuration.getTaskRunner() instanceof SingleThreadedTaskRunner);
	}

	public void testAllDatastoreTypes() throws Exception {
		DatastoreCatalog datastoreCatalog = getDataStoreCatalog(getConfiguration());
		String[] datastoreNames = datastoreCatalog.getDatastoreNames();
		assertEquals(
				"[my_jdbc_connection, my_dbase, my_csv, my_custom, my_odb, my_jdbc_datasource, my_excel_2003, my_composite, my_access]",
				Arrays.toString(datastoreNames));

		for (String name : datastoreNames) {
			// test that all connections, except the JNDI-based on will work
			if (!"my_jdbc_datasource".equals(name)) {
				Datastore datastore = datastoreCatalog.getDatastore(name);
				DataContext dc = datastore.getDataContextProvider().getDataContext();
				assertNotNull(dc);
			}
		}

		Datastore compositeDatastore = datastoreCatalog.getDatastore("my_composite");
		DataContextProvider dcp = compositeDatastore.getDataContextProvider();
		DataContext dataContext = dcp.getDataContext();
		String[] schemaNames = dataContext.getSchemaNames();
		assertEquals("[INFORMATION_SCHEMA, PUBLIC, Spreadsheet2003.xls, developers.mdb, employees.csv, information_schema]",
				Arrays.toString(schemaNames));
		dcp.close();
	}

	private AnalyzerBeansConfiguration getConfiguration() {
		AnalyzerBeansConfiguration configuration = reader.create(new File(
				"src/test/resources/example-configuration-all-datastore-types.xml"));
		return configuration;
	}

	private DatastoreCatalog getDataStoreCatalog(AnalyzerBeansConfiguration configuration) {
		_datastoreCatalog = configuration.getDatastoreCatalog();
		return _datastoreCatalog;
	}

	public void testReferenceDataCatalog() throws Exception {
		ReferenceDataCatalog referenceDataCatalog = getConfigurationFromXMLFile().getReferenceDataCatalog();
		String[] dictionaryNames = referenceDataCatalog.getDictionaryNames();
		assertEquals("[datastore_dict, textfile_dict, valuelist_dict, custom_dict]", Arrays.toString(dictionaryNames));

		Dictionary d = referenceDataCatalog.getDictionary("datastore_dict");
		assertTrue(d.containsValue("Patterson"));
		assertTrue(d.containsValue("Murphy"));
		assertFalse(d.containsValue("Gates"));

		d = referenceDataCatalog.getDictionary("textfile_dict");
		assertTrue(d.containsValue("Patterson"));
		assertFalse(d.containsValue("Murphy"));
		assertTrue(d.containsValue("Gates"));

		d = referenceDataCatalog.getDictionary("valuelist_dict");
		assertFalse(d.containsValue("Patterson"));
		assertFalse(d.containsValue("Murphy"));
		assertTrue(d.containsValue("greetings"));

		d = referenceDataCatalog.getDictionary("custom_dict");
		assertFalse(d.containsValue("Patterson"));
		assertFalse(d.containsValue("Murphy"));
		assertFalse(d.containsValue("Gates"));
		assertTrue(d.containsValue("value0"));
		assertTrue(d.containsValue("value1"));
		assertTrue(d.containsValue("value2"));
		assertTrue(d.containsValue("value3"));
		assertTrue(d.containsValue("value4"));
		assertFalse(d.containsValue("value5"));

		String[] synonymCatalogNames = referenceDataCatalog.getSynonymCatalogNames();
		assertEquals("[textfile_syn, custom_syn]", Arrays.toString(synonymCatalogNames));

		SynonymCatalog s = referenceDataCatalog.getSynonymCatalog("textfile_syn");
		assertEquals("DNK", s.getMasterTerm("Denmark"));
		assertEquals("DNK", s.getMasterTerm("Danmark"));
		assertEquals("DNK", s.getMasterTerm("DK"));
		assertEquals("ALB", s.getMasterTerm("Albania"));
		assertEquals(null, s.getMasterTerm("Netherlands"));

		s = referenceDataCatalog.getSynonymCatalog("custom_syn");
		assertEquals("DNK", s.getMasterTerm("Denmark"));
		assertEquals("DNK", s.getMasterTerm("Danmark"));
		assertEquals(null, s.getMasterTerm("DK"));
		assertEquals(null, s.getMasterTerm("Albania"));
		assertEquals("NLD", s.getMasterTerm("Netherlands"));

		String[] stringPatternNames = referenceDataCatalog.getStringPatternNames();
		assertEquals("[regex danish email, simple email]", Arrays.toString(stringPatternNames));

		StringPattern pattern = referenceDataCatalog.getStringPattern("regex danish email");
		assertEquals("RegexStringPattern[name=regex danish email,expression=[a-z]+@[a-z]+\\.dk]", pattern.toString());
		assertTrue(pattern.matches("kasper@eobjects.dk"));
		assertFalse(pattern.matches("kasper@eobjects.org"));
		assertFalse(pattern.matches(" kasper@eobjects.dk"));

		pattern = referenceDataCatalog.getStringPattern("simple email");
		assertEquals("SimpleStringPattern[name=simple email,expression=aaaa@aaaaa.aa]", pattern.toString());
		assertTrue(pattern.matches("kasper@eobjects.dk"));
		assertTrue(pattern.matches("kasper@eobjects.org"));
		assertFalse(pattern.matches(" kasper@eobjects.dk"));
	}

	public void testCustomDictionaryWithInjectedDatastore() {
		AnalyzerBeansConfiguration configuration = getConfigurationFromXMLFile();
		ReferenceDataCatalog referenceDataCatalog = configuration.getReferenceDataCatalog();
		SampleCustomDictionary sampleCustomDictionary = (SampleCustomDictionary) referenceDataCatalog
				.getDictionary("custom_dict");
		Assert.assertEquals("my_jdbc_connection", sampleCustomDictionary.datastore.getName());
	}

	private AnalyzerBeansConfiguration getConfigurationFromXMLFile() {
		AnalyzerBeansConfiguration configuration = reader.create(new File(
				"src/test/resources/example-configuration-all-reference-data-types.xml"));
		return configuration;
	}
}
