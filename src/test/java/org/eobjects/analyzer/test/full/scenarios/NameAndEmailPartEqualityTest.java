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
package org.eobjects.analyzer.test.full.scenarios;

import java.util.List;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.filter.ValidationCategory;
import org.eobjects.analyzer.beans.script.JavaScriptFilter;
import org.eobjects.analyzer.beans.standardize.EmailStandardizerTransformer;
import org.eobjects.analyzer.beans.standardize.NameStandardizerTransformer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.MutableInputColumn;
import org.eobjects.analyzer.descriptors.ClasspathScanDescriptorProvider;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.FilterJobBuilder;
import org.eobjects.analyzer.job.builder.RowProcessingAnalyzerJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.analyzer.job.concurrent.SingleThreadedTaskRunner;
import org.eobjects.analyzer.job.concurrent.TaskRunner;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.reference.ReferenceDataCatalog;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.StringAnalyzerResult;
import org.eobjects.analyzer.result.ValueDistributionResult;
import org.eobjects.analyzer.storage.StorageProvider;
import org.eobjects.analyzer.test.TestHelper;

import org.eobjects.metamodel.schema.Column;
import org.eobjects.metamodel.schema.Schema;
import org.eobjects.metamodel.schema.Table;

public class NameAndEmailPartEqualityTest extends TestCase {

	public void testScenario() throws Throwable {
		TaskRunner taskRunner = new SingleThreadedTaskRunner();
		DescriptorProvider descriptorProvider = new ClasspathScanDescriptorProvider(taskRunner).scanPackage(
				"org.eobjects.analyzer.beans", true);
		StorageProvider storageProvider = TestHelper.createStorageProvider();
		DatastoreCatalog datastoreCatalog = TestHelper.createDatastoreCatalog();
		ReferenceDataCatalog referenceDataCatalog = TestHelper.createReferenceDataCatalog();
		AnalyzerBeansConfiguration configuration = new AnalyzerBeansConfigurationImpl(datastoreCatalog,
				referenceDataCatalog, descriptorProvider, taskRunner, storageProvider);

		AnalysisRunner runner = new AnalysisRunnerImpl(configuration);

		CsvDatastore ds = new CsvDatastore("data.csv", "src/test/resources/NameAndEmailPartEqualityTest-data.csv");

		AnalysisJobBuilder analysisJobBuilder = new AnalysisJobBuilder(configuration);
		analysisJobBuilder.setDatastore(ds);

		DataContextProvider dcp = ds.getDataContextProvider();
		Schema schema = dcp.getDataContext().getDefaultSchema();
		Table table = schema.getTables()[0];
		assertNotNull(table);

		Column nameColumn = table.getColumnByName("name");
		Column emailColumn = table.getColumnByName("email");

		analysisJobBuilder.addSourceColumns(nameColumn, emailColumn);

		TransformerJobBuilder<NameStandardizerTransformer> nameTransformerJobBuilder = analysisJobBuilder
				.addTransformer(NameStandardizerTransformer.class);
		nameTransformerJobBuilder.addInputColumn(analysisJobBuilder.getSourceColumnByName("name"));
		nameTransformerJobBuilder.setConfiguredProperty("Name patterns", NameStandardizerTransformer.DEFAULT_PATTERNS);

		assertTrue(nameTransformerJobBuilder.isConfigured());

		final List<MutableInputColumn<?>> nameColumns = nameTransformerJobBuilder.getOutputColumns();
		assertEquals(4, nameColumns.size());
		assertEquals("Firstname", nameColumns.get(0).getName());
		assertEquals("Lastname", nameColumns.get(1).getName());
		assertEquals("Middlename", nameColumns.get(2).getName());
		assertEquals("Titulation", nameColumns.get(3).getName());

		TransformerJobBuilder<EmailStandardizerTransformer> emailTransformerJobBuilder = analysisJobBuilder
				.addTransformer(EmailStandardizerTransformer.class);
		emailTransformerJobBuilder.addInputColumn(analysisJobBuilder.getSourceColumnByName("email"));

		assertTrue(emailTransformerJobBuilder.isConfigured());

		@SuppressWarnings("unchecked")
		final MutableInputColumn<String> usernameColumn = (MutableInputColumn<String>) emailTransformerJobBuilder
				.getOutputColumnByName("Username");
		assertNotNull(usernameColumn);

		assertTrue(analysisJobBuilder.addRowProcessingAnalyzer(StringAnalyzer.class).addInputColumns(nameColumns)
				.addInputColumns(emailTransformerJobBuilder.getOutputColumns()).isConfigured());

		for (InputColumn<?> inputColumn : nameColumns) {
			RowProcessingAnalyzerJobBuilder<ValueDistributionAnalyzer> analyzerJobBuilder = analysisJobBuilder
					.addRowProcessingAnalyzer(ValueDistributionAnalyzer.class);
			analyzerJobBuilder.addInputColumn(inputColumn);
			analyzerJobBuilder.setConfiguredProperty("Record unique values", false);
			analyzerJobBuilder.setConfiguredProperty("Top n most frequent values", 1000);
			analyzerJobBuilder.setConfiguredProperty("Bottom n most frequent values", 1000);
			assertTrue(analyzerJobBuilder.isConfigured());
		}

		FilterJobBuilder<JavaScriptFilter, ValidationCategory> fjb = analysisJobBuilder.addFilter(JavaScriptFilter.class);
		fjb.addInputColumn(nameTransformerJobBuilder.getOutputColumnByName("Firstname"));
		fjb.addInputColumn(usernameColumn);
		fjb.setConfiguredProperty("Source code", "values[0] == values[1];");

		assertTrue(fjb.isConfigured());

		analysisJobBuilder.addRowProcessingAnalyzer(StringAnalyzer.class)
				.addInputColumn(analysisJobBuilder.getSourceColumnByName("email"))
				.setRequirement(fjb, ValidationCategory.VALID);
		analysisJobBuilder.addRowProcessingAnalyzer(StringAnalyzer.class)
				.addInputColumn(analysisJobBuilder.getSourceColumnByName("email"))
				.setRequirement(fjb, ValidationCategory.INVALID);

		AnalysisResultFuture resultFuture = runner.run(analysisJobBuilder.toAnalysisJob());

		dcp.close();

		if (!resultFuture.isSuccessful()) {
			List<Throwable> errors = resultFuture.getErrors();
			throw errors.get(0);
		}

		List<AnalyzerResult> results = resultFuture.getResults();

		assertEquals(7, results.size());

		ValueDistributionResult vdResult = (ValueDistributionResult) results.get(1);
		assertEquals("Firstname", vdResult.getColumnName());
		assertEquals(0, vdResult.getNullCount());
		assertEquals(2, vdResult.getUniqueCount());
		assertEquals("ValueCountList[[[barack->4]]]", vdResult.getTopValues().toString());

		vdResult = (ValueDistributionResult) results.get(2);
		assertEquals("Lastname", vdResult.getColumnName());
		assertEquals(0, vdResult.getNullCount());
		assertEquals(0, vdResult.getUniqueCount());
		assertEquals("ValueCountList[[[obama->4], [doe->2]]]", vdResult.getTopValues().toString());

		vdResult = (ValueDistributionResult) results.get(3);
		assertEquals("Middlename", vdResult.getColumnName());
		assertEquals(4, vdResult.getNullCount());
		assertEquals(0, vdResult.getUniqueCount());
		assertEquals("ValueCountList[[[hussein->2]]]", vdResult.getTopValues().toString());
		
		vdResult = (ValueDistributionResult) results.get(4);
		assertEquals("Titulation", vdResult.getColumnName());
		assertEquals(6, vdResult.getNullCount());
		assertEquals(0, vdResult.getUniqueCount());
		assertEquals("ValueCountList[[]]", vdResult.getTopValues().toString());

		StringAnalyzerResult stringAnalyzerResult = (StringAnalyzerResult) results.get(5);
		assertEquals(1, stringAnalyzerResult.getColumns().length);
		assertEquals("4", stringAnalyzerResult.getCrosstab().where("Column", "email").where("Measures", "Row count").get().toString());
		
		stringAnalyzerResult = (StringAnalyzerResult) results.get(6);
		assertEquals(1, stringAnalyzerResult.getColumns().length);
		assertEquals("2", stringAnalyzerResult.getCrosstab().where("Column", "email").where("Measures", "Row count").get().toString());
	}
}
