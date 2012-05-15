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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer;
import org.eobjects.analyzer.beans.valuedist.ValueDistributionResult;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreConnection;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.concurrent.MultiThreadedTaskRunner;
import org.eobjects.analyzer.job.concurrent.TaskRunner;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.AnnotatedRowsResult;
import org.eobjects.analyzer.result.Crosstab;
import org.eobjects.analyzer.result.CrosstabNavigator;
import org.eobjects.analyzer.result.CrosstabResult;
import org.eobjects.analyzer.result.ResultProducer;
import org.eobjects.analyzer.result.renderer.CrosstabTextRenderer;
import org.eobjects.analyzer.test.TestHelper;
import org.eobjects.metamodel.DataContext;
import org.eobjects.metamodel.schema.Column;
import org.eobjects.metamodel.schema.Table;

public class ValueDistributionAndStringAnalysisTest extends TestCase {

	public void testScenario() throws Exception {
		TaskRunner taskRunner = new MultiThreadedTaskRunner(5);

		AnalyzerBeansConfiguration configuration = new AnalyzerBeansConfigurationImpl().replace(taskRunner);

		AnalysisRunner runner = new AnalysisRunnerImpl(configuration);

		Datastore datastore = TestHelper.createSampleDatabaseDatastore("ds");
		DatastoreConnection con = datastore.openConnection();
		DataContext dc = con.getDataContext();

		AnalysisJobBuilder analysisJobBuilder = new AnalysisJobBuilder(configuration);
		analysisJobBuilder.setDatastoreConnection(con);

		Table table = dc.getDefaultSchema().getTableByName("EMPLOYEES");
		assertNotNull(table);

		Column[] columns = table.getColumns();

		analysisJobBuilder.addSourceColumns(columns);

		for (InputColumn<?> inputColumn : analysisJobBuilder.getSourceColumns()) {
			AnalyzerJobBuilder<ValueDistributionAnalyzer> valueDistribuitionJobBuilder = analysisJobBuilder
					.addAnalyzer(ValueDistributionAnalyzer.class);
			valueDistribuitionJobBuilder.addInputColumn(inputColumn);
			valueDistribuitionJobBuilder.setConfiguredProperty("Record unique values", false);
			valueDistribuitionJobBuilder.setConfiguredProperty("Top n most frequent values", null);
			valueDistribuitionJobBuilder.setConfiguredProperty("Bottom n most frequent values", null);
		}

		AnalyzerJobBuilder<StringAnalyzer> stringAnalyzerJob = analysisJobBuilder.addAnalyzer(StringAnalyzer.class);
		stringAnalyzerJob.addInputColumns(analysisJobBuilder.getAvailableInputColumns(String.class));

		AnalysisJob analysisJob = analysisJobBuilder.toAnalysisJob();

		AnalysisResultFuture resultFuture = runner.run(analysisJob);

		assertFalse(resultFuture.isDone());

		List<AnalyzerResult> results = resultFuture.getResults();

		assertTrue(resultFuture.isDone());

		// expect 1 result for each column (the value distributions) and 1
		// result for the string analyzer
		assertEquals(table.getColumnCount() + 1, results.size());

		int stringAnalyzerResults = 0;
		int valueDistributionResults = 0;

		CrosstabResult stringAnalyzerResult = (CrosstabResult) resultFuture.getResult(stringAnalyzerJob.toAnalyzerJob());

		for (AnalyzerResult result : results) {
			if (result instanceof CrosstabResult) {
				stringAnalyzerResults++;

				assertTrue(result instanceof CrosstabResult);
				CrosstabResult cr = (CrosstabResult) result;
				Crosstab<?> crosstab = cr.getCrosstab();
				assertEquals("[Column, Measures]", Arrays.toString(crosstab.getDimensionNames()));
				assertEquals("[LASTNAME, FIRSTNAME, EXTENSION, EMAIL, OFFICECODE, JOBTITLE]", crosstab.getDimension(0)
						.getCategories().toString());
				assertEquals(
						"[Row count, Null count, Entirely uppercase count, Entirely lowercase count, Total char count, Max chars, Min chars, Avg chars, Max white spaces, Min white spaces, Avg white spaces, Uppercase chars, Uppercase chars (excl. first letters), Lowercase chars, Digit chars, Diacritic chars, Non-letter chars, Word count, Max words, Min words]",
						crosstab.getDimension(1).getCategories().toString());
				CrosstabNavigator<?> nav = crosstab.navigate();
				nav.where("Column", "EMAIL");
				nav.where("Measures", "Total char count");
				assertEquals("655", nav.get().toString());
			} else {
				assertTrue(result instanceof ValueDistributionResult);

				valueDistributionResults++;
			}
		}

		assertEquals(1, stringAnalyzerResults);
		assertEquals(8, valueDistributionResults);

		ValueDistributionResult jobTitleResult = null;
		ValueDistributionResult lastnameResult = null;
		for (AnalyzerResult result : results) {
			if (result instanceof ValueDistributionResult) {
				ValueDistributionResult vdResult = (ValueDistributionResult) result;
				if ("JOBTITLE".equals(vdResult.getColumnName())) {
					jobTitleResult = vdResult;
				} else if ("LASTNAME".equals(vdResult.getColumnName())) {
					lastnameResult = vdResult;
				}
			}
		}

		assertNotNull(jobTitleResult);
		assertNotNull(lastnameResult);

		assertEquals("Patterson", lastnameResult.getSingleValueDistributionResult().getTopValues().getValueCounts().get(0)
				.getValue());
		assertEquals(3, lastnameResult.getSingleValueDistributionResult().getTopValues().getValueCounts().get(0).getCount());
		assertEquals(16, lastnameResult.getSingleValueDistributionResult().getUniqueCount());
		assertEquals(0, lastnameResult.getSingleValueDistributionResult().getNullCount());

		assertEquals("Sales Rep", jobTitleResult.getSingleValueDistributionResult().getTopValues().getValueCounts().get(0)
				.getValue());

		String[] resultLines = new CrosstabTextRenderer().render(stringAnalyzerResult).split("\n");
		assertEquals(
				"                                        LASTNAME  FIRSTNAME  EXTENSION      EMAIL OFFICECODE   JOBTITLE ",
				resultLines[0]);
		assertEquals(
				"Uppercase chars (excl. first letters)          0          1          0          0          0         39 ",
				resultLines[13]);
		assertEquals(
				"Diacritic chars                                0          0          0          0          0          0 ",
				resultLines[16]);

		// do some drill-to-detail on the StringAnalyzerResult
		Crosstab<?> crosstab = stringAnalyzerResult.getCrosstab();

		ResultProducer resultProducer = crosstab.where("Column", "FIRSTNAME")
				.where("Measures", "Uppercase chars (excl. first letters)").explore();
		assertNotNull(resultProducer);

		AnnotatedRowsResult arr = (AnnotatedRowsResult) resultProducer.getResult();
		InputRow[] rows = arr.getRows();
		assertEquals(1, rows.length);
		assertEquals("Foon Yue", rows[0].getValue(analysisJobBuilder.getSourceColumnByName("FIRSTNAME")).toString());

		resultProducer = crosstab.where("Column", "FIRSTNAME").where("Measures", "Diacritic chars").explore();
		assertNull(resultProducer);

		con.close();
		taskRunner.shutdown();
	}
}