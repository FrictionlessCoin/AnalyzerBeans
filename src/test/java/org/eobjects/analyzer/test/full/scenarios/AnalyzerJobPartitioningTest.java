package org.eobjects.analyzer.test.full.scenarios;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.JaxbJobFactory;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.CrosstabResult;
import org.eobjects.analyzer.result.ValueDistributionResult;
import org.eobjects.analyzer.result.renderer.CrosstabTextRenderer;
import org.eobjects.analyzer.test.TestHelper;

public class AnalyzerJobPartitioningTest extends TestCase {

	public void testScenario() throws Exception {
		AnalyzerBeansConfiguration conf = TestHelper.createAnalyzerBeansConfiguration(TestHelper
				.createSampleDatabaseDatastore("my database"));

		AnalysisRunner runner = new AnalysisRunnerImpl(conf);

		AnalysisJobBuilder jobBuilder = new JaxbJobFactory(conf).create(new File(
				"src/test/resources/example-job-partitioning.xml"));

		AnalysisJob analysisJob = jobBuilder.toAnalysisJob();
		assertEquals(6, analysisJob.getAnalyzerJobs().size());

		AnalysisResultFuture resultFuture = runner.run(analysisJob);
		assertTrue(resultFuture.isSuccessful());

		List<AnalyzerResult> results = resultFuture.getResults();

		int vdResults = 0;
		List<CrosstabResult> saResults = new ArrayList<CrosstabResult>();

		for (AnalyzerResult analyzerResult : results) {
			if (analyzerResult instanceof ValueDistributionResult) {
				vdResults++;
			} else if (analyzerResult instanceof CrosstabResult) {
				saResults.add((CrosstabResult) analyzerResult);
			} else {
				fail("Unexpected result: " + analyzerResult);
			}
		}

		assertEquals(4, vdResults);
		assertEquals(2, saResults.size());

		final int dimensionIndex = saResults.get(0).getCrosstab().getDimensionIndex("Column");

		Collections.sort(saResults, new Comparator<CrosstabResult>() {
			@Override
			public int compare(CrosstabResult o1, CrosstabResult o2) {
				int count1 = o1.getCrosstab().getDimension(dimensionIndex).getCategoryCount();
				int count2 = o2.getCrosstab().getDimension(dimensionIndex).getCategoryCount();
				return count1 - count2;
			}
		});

		String[] resultLines1 = new CrosstabTextRenderer().render(saResults.get(0)).split("\n");
		assertEquals("                                      CUSTOMERNAME ", resultLines1[0]);
		assertEquals("Row count                                      122 ", resultLines1[1]);

		String[] resultLines2 = new CrosstabTextRenderer().render(saResults.get(1)).split("\n");
		assertEquals("                                      FIRSTNAME  LASTNAME     EMAIL ", resultLines2[0]);
		assertEquals("Row count                                    23        23        23 ", resultLines2[1]);
	}
}
