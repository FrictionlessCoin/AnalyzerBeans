package org.eobjects.analyzer.job.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.StringAnalyzer;
import org.eobjects.analyzer.beans.convert.ConvertToStringTransformer;
import org.eobjects.analyzer.beans.filter.NotNullFilter;
import org.eobjects.analyzer.beans.filter.SingleWordFilter;
import org.eobjects.analyzer.beans.filter.ValidationCategory;
import org.eobjects.analyzer.beans.mock.TransformerMock;
import org.eobjects.analyzer.beans.transform.WhitespaceTrimmerTransformer;
import org.eobjects.analyzer.data.MetaModelInputColumn;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.BeanJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.FilterJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.analyzer.lifecycle.AnalyzerBeanInstance;
import org.eobjects.analyzer.lifecycle.FilterBeanInstance;
import org.eobjects.analyzer.lifecycle.TransformerBeanInstance;
import org.eobjects.analyzer.test.MockDataContextProvider;
import org.eobjects.analyzer.test.TestHelper;

import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.ColumnType;
import dk.eobjects.metamodel.schema.MutableColumn;

public class RowProcessingPublisherTest extends TestCase {

	public void testCreateProcessOrderedConsumerListNoConsumers() throws Exception {
		List<RowProcessingConsumer> consumerList = RowProcessingPublisher
				.createProcessOrderedConsumerList(new ArrayList<RowProcessingConsumer>());
		assertTrue(consumerList.isEmpty());
	}

	public void testCreateProcessOrderedConsumerListWithFilterDependencies() throws Exception {
		AnalysisJobBuilder ajb = new AnalysisJobBuilder(TestHelper.createAnalyzerBeansConfiguration());
		ajb.setDataContextProvider(new MockDataContextProvider());
		Column physicalColumn = new MutableColumn("foo", ColumnType.VARCHAR);
		ajb.addSourceColumn(physicalColumn);
		MetaModelInputColumn inputColumn = ajb.getSourceColumns().get(0);

		// 1: add a not-null filter
		FilterJobBuilder<NotNullFilter, ValidationCategory> fjb1 = ajb.addFilter(NotNullFilter.class);
		fjb1.addInputColumn(inputColumn);

		// 2: trim (depends on not-null)
		TransformerJobBuilder<WhitespaceTrimmerTransformer> tjb1 = ajb.addTransformer(WhitespaceTrimmerTransformer.class);
		tjb1.addInputColumn(inputColumn);
		tjb1.setRequirement(fjb1, ValidationCategory.VALID);

		// 3: trim again, just to examplify (depends on first trim output)
		TransformerJobBuilder<WhitespaceTrimmerTransformer> tjb2 = ajb.addTransformer(WhitespaceTrimmerTransformer.class);
		tjb2.addInputColumn(tjb1.getOutputColumns().get(0));

		// 4: add a single word filter (depends on second trim)
		FilterJobBuilder<SingleWordFilter, ValidationCategory> fjb2 = ajb.addFilter(SingleWordFilter.class);
		fjb2.addInputColumn(tjb2.getOutputColumns().get(0));

		// 5 and 6: Analyze VALID and INVALID output of single-word filter
		// separately (the order of these two are not deterministic because of
		// the shuffle)
		ajb.addRowProcessingAnalyzer(StringAnalyzer.class).addInputColumn(inputColumn)
				.setRequirement(fjb2, ValidationCategory.VALID);
		ajb.addRowProcessingAnalyzer(StringAnalyzer.class).addInputColumn(inputColumn)
				.setRequirement(fjb2, ValidationCategory.INVALID);

		assertTrue(ajb.isConfigured());

		List<RowProcessingConsumer> consumers = getConsumers(ajb.toAnalysisJob());

		assertEquals(6, consumers.size());

		consumers = RowProcessingPublisher.createProcessOrderedConsumerList(consumers);

		assertEquals("ImmutableFilterJob[filter=Not null]", consumers.get(0).getBeanJob().toString());
		assertEquals("ImmutableTransformerJob[transformer=Whitespace trimmer]", consumers.get(1).getBeanJob().toString());
		assertEquals("ImmutableTransformerJob[transformer=Whitespace trimmer]", consumers.get(2).getBeanJob().toString());
		assertEquals("ImmutableFilterJob[filter=Single word]", consumers.get(3).getBeanJob().toString());
	}

	private List<RowProcessingConsumer> getConsumers(AnalysisJob analysisJob) {
		List<RowProcessingConsumer> consumers = new ArrayList<RowProcessingConsumer>();

		AnalysisListener listener = null;

		for (AnalyzerJob analyzerJob : analysisJob.getAnalyzerJobs()) {
			RowProcessingConsumer consumer = new AnalyzerConsumer(analysisJob, new AnalyzerBeanInstance(
					analyzerJob.getDescriptor()), analyzerJob, analyzerJob.getInput(), listener);
			consumers.add(consumer);
		}
		for (TransformerJob transformerJob : analysisJob.getTransformerJobs()) {
			RowProcessingConsumer consumer = new TransformerConsumer(analysisJob, new TransformerBeanInstance(
					transformerJob.getDescriptor()), transformerJob, transformerJob.getInput(), listener);
			consumers.add(consumer);
		}
		for (FilterJob filterJob : analysisJob.getFilterJobs()) {
			FilterConsumer consumer = new FilterConsumer(analysisJob, new FilterBeanInstance(filterJob.getDescriptor()),
					filterJob, filterJob.getInput(), listener);
			consumers.add(consumer);
		}

		// shuffle the list (it should work regardless of the initial sort
		// order)
		Collections.shuffle(consumers);

		return consumers;
	}

	public void testCreateProcessOrderedConsumerListChainedTransformers() throws Exception {
		AnalysisJobBuilder ajb = new AnalysisJobBuilder(TestHelper.createAnalyzerBeansConfiguration());
		Column physicalColumn = new MutableColumn("foo", ColumnType.VARCHAR);
		ajb.addSourceColumn(physicalColumn);

		TransformerJobBuilder<TransformerMock> tjb1 = ajb.addTransformer(TransformerMock.class).addInputColumn(
				ajb.getSourceColumns().get(0));
		TransformerJobBuilder<TransformerMock> tjb2 = ajb.addTransformer(TransformerMock.class).addInputColumn(
				tjb1.getOutputColumns().get(0));
		TransformerJobBuilder<ConvertToStringTransformer> tjb3 = ajb.addTransformer(ConvertToStringTransformer.class)
				.addInputColumn(tjb2.getOutputColumns().get(0));

		ajb.addRowProcessingAnalyzer(StringAnalyzer.class).addInputColumn(ajb.getSourceColumns().get(0));
		ajb.addRowProcessingAnalyzer(StringAnalyzer.class).addInputColumn(tjb3.getOutputColumns().get(0));

		ajb.setDataContextProvider(new MockDataContextProvider());

		assertTrue(ajb.isConfigured());
		AnalysisJob analysisJob = ajb.toAnalysisJob();

		List<RowProcessingConsumer> consumers = getConsumers(analysisJob);

		consumers = RowProcessingPublisher.createProcessOrderedConsumerList(consumers);

		assertEquals(5, consumers.size());

		List<TransformerJob> transformerJobs = new ArrayList<TransformerJob>(analysisJob.getTransformerJobs());
		List<AnalyzerJob> analyzerJobs = new ArrayList<AnalyzerJob>(analysisJob.getAnalyzerJobs());

		// create a list that represents the expected dependent sequence
		Queue<BeanJob<?>> jobDependencies = new LinkedList<BeanJob<?>>();
		jobDependencies.add(transformerJobs.get(0));
		jobDependencies.add(transformerJobs.get(1));
		jobDependencies.add(transformerJobs.get(2));
		jobDependencies.add(analyzerJobs.get(1));

		int jobDependenciesFound = 0;
		boolean analyzerJob1found = false;

		BeanJob<?> nextJobDependency = jobDependencies.poll();
		for (RowProcessingConsumer rowProcessingConsumer : consumers) {
			BeanJob<?> job = rowProcessingConsumer.getBeanJob();
			if (job == nextJobDependency) {
				nextJobDependency = jobDependencies.poll();
				jobDependenciesFound++;
			} else if (job == analyzerJobs.get(0)) {
				assertFalse(analyzerJob1found);
				analyzerJob1found = true;
			} else {
				fail("The consumers sort order is wrong! Found: " + job + " but expected: " + nextJobDependency);
			}
		}

		assertTrue(analyzerJob1found);
		assertEquals(4, jobDependenciesFound);
	}
}
