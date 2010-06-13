package org.eobjects.analyzer.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.SingleDataContextProvider;
import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.ConfiguredDescriptor;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.descriptors.JobListDescriptorProvider;
import org.eobjects.analyzer.lifecycle.AnalyzerBeanInstance;
import org.eobjects.analyzer.lifecycle.AssignConfiguredCallback;
import org.eobjects.analyzer.lifecycle.AssignConfiguredRowProcessingCallback;
import org.eobjects.analyzer.lifecycle.AssignProvidedCallback;
import org.eobjects.analyzer.lifecycle.BerkeleyDbCollectionProvider;
import org.eobjects.analyzer.lifecycle.CloseCallback;
import org.eobjects.analyzer.lifecycle.CollectionProvider;
import org.eobjects.analyzer.lifecycle.InitializeCallback;
import org.eobjects.analyzer.lifecycle.ReturnResultsCallback;
import org.eobjects.analyzer.lifecycle.RunExplorerCallback;
import org.eobjects.analyzer.lifecycle.RunRowProcessorsCallback;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.MetaModelHelper;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

public class AnalysisRunnerImpl implements AnalysisRunner {

	private static final Logger logger = LoggerFactory
			.getLogger(AnalysisRunnerImpl.class);

	private List<AnalysisJob> _jobs = new LinkedList<AnalysisJob>();
	private CollectionProvider _collectionProvider;
	private DescriptorProvider _descriptorProvider;
	private List<AnalyzerResult> _result;
	private Integer _rowProcessorCount;
	private ConcurrencyProvider _concurrencyProvider;
	private Future<Collection<Future<?>>> _finalFuture;

	public AnalysisRunnerImpl() {
	}

	public AnalysisRunnerImpl(DescriptorProvider descriptorProvider,
			ConcurrencyProvider concurrencyProvider,
			CollectionProvider collectionProvider) {
		_descriptorProvider = descriptorProvider;
		_concurrencyProvider = concurrencyProvider;
		_collectionProvider = collectionProvider;
	}

	@Override
	public void addJob(AnalysisJob job) {
		_jobs.add(job);
		_rowProcessorCount = null;
	}

	@Override
	public void run(DataContext dataContext) {
		run(new SingleDataContextProvider(dataContext));
	}

	@Override
	public void run(DataContextProvider dataContextProvider) {
		logger.info("run(...) invoked.");
		logger.info("jobs: " + _jobs.size());
		for (AnalysisJob job : _jobs) {
			logger.info(" - " + job);
		}
		DescriptorProvider descriptorProvider = getDescriptorProvider();
		if (descriptorProvider == null) {
			descriptorProvider = new JobListDescriptorProvider(_jobs);
		}
		CollectionProvider collectionProvider = getCollectionProvider();
		if (collectionProvider == null) {
			collectionProvider = new BerkeleyDbCollectionProvider();
		}
		ConcurrencyProvider concurrencyProvider = getConcurrencyProvider();
		if (concurrencyProvider == null) {
			concurrencyProvider = new SingleThreadedConcurrencyProvider();
		}
		if (_result == null) {
			_result = new ArrayList<AnalyzerResult>();
		}
		List<AnalysisJob> explorerJobs = new LinkedList<AnalysisJob>();
		List<AnalysisJob> rowProcessingJobs = new LinkedList<AnalysisJob>();

		categorizeJobs(descriptorProvider, explorerJobs, rowProcessingJobs);

		// Instantiate beans and set specific lifecycle-callbacks
		RunExplorerCallback runExplorerCallback = new RunExplorerCallback(
				dataContextProvider);
		List<AnalyzerBeanInstance> analyzerBeanInstances = new LinkedList<AnalyzerBeanInstance>();
		for (AnalysisJob job : explorerJobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptorProvider
					.getDescriptorForClass(analyzerClass);
			AnalyzerBeanInstance analyzer = instantiateAnalyzerBean(descriptor);
			analyzer.getRunCallbacks().add(runExplorerCallback);
			analyzer.getAssignConfiguredCallbacks().add(
					new AssignConfiguredCallback(job, dataContextProvider
							.getSchemaNavigator()));
			analyzerBeanInstances.add(analyzer);
		}
		Map<Table, AnalysisRowProcessor> rowProcessors = new HashMap<Table, AnalysisRowProcessor>();
		for (AnalysisJob job : rowProcessingJobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptorProvider
					.getDescriptorForClass(analyzerClass);
			initRowProcessingBeans(job, descriptor, analyzerBeanInstances,
					rowProcessors, dataContextProvider);
		}
		_rowProcessorCount = rowProcessors.size();

		// Add shared callbacks
		InitializeCallback initializeCallback = new InitializeCallback();
		ReturnResultsCallback returnResultsCallback = new ReturnResultsCallback(
				_result);
		CloseCallback closeCallback = new CloseCallback();
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			AssignProvidedCallback assignProvidedCallback = new AssignProvidedCallback(
					analyzerBeanInstance, collectionProvider,
					dataContextProvider);

			analyzerBeanInstance.getAssignProvidedCallbacks().add(
					assignProvidedCallback);
			analyzerBeanInstance.getInitializeCallbacks().add(
					initializeCallback);
			analyzerBeanInstance.getReturnResultsCallbacks().add(
					returnResultsCallback);
			analyzerBeanInstance.getCloseCallbacks().add(closeCallback);
		}

		Collection<Callable<?>> assignCallables = new LinkedList<Callable<?>>();
		Collection<Callable<?>> runCallables = new LinkedList<Callable<?>>();
		List<Callable<?>> collectResultsAndCloseAnalyzersCallables = new LinkedList<Callable<?>>();

		logger
				.info("Scheduling tasks for assinging @Configured and @Provided properties, and running @Initialize methods");
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			assignCallables.add(Executors
					.callable(new AssignAndInitializeRunnable(
							analyzerBeanInstance)));
		}

		logger.info("Scheduling tasks for exploring analyzers");
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			runCallables.add(Executors.callable(analyzerBeanInstance));
		}

		logger.info("Scheduling tasks for row processing analyzers");
		for (AnalysisRowProcessor analysisRowProcessor : rowProcessors.values()) {
			runCallables.add(Executors.callable(analysisRowProcessor));
		}

		logger
				.info("Scheduling tasks for retreiving results and closing beans");
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			collectResultsAndCloseAnalyzersCallables.add(Executors
					.callable(new CollectResultsAndCloseAnalyzerBeanRunnable(
							analyzerBeanInstance)));
		}

		Future<Collection<Future<?>>> assignFutures = concurrencyProvider
				.schedule(new SynchronizeCallable("schedule initial tasks",
						concurrencyProvider, assignCallables));

		Future<Collection<Future<?>>> runFutures = concurrencyProvider
				.schedule(new SynchronizeCallable(
						"assign properties and initialize",
						concurrencyProvider, assignFutures, runCallables));

		Future<Collection<Future<?>>> collectResultsFuture = concurrencyProvider
				.schedule(new SynchronizeCallable("run analyzers",
						concurrencyProvider, runFutures,
						collectResultsAndCloseAnalyzersCallables));

		_finalFuture = concurrencyProvider.schedule(new SynchronizeCallable(
				"collect results", concurrencyProvider, collectResultsFuture,
				new EmptyCallable<Boolean>(true)));
	}

	@Override
	public List<AnalyzerResult> getResults() {
		while (!isDone()) {
			try {
				_finalFuture.get();
			} catch (Exception e) {
				logger.error("Unexpected error while retreiving results", e);
			}
		}
		return Collections.unmodifiableList(_result);
	}

	@Override
	public boolean isDone() {
		if (_finalFuture == null) {
			return false;
		}
		return _finalFuture.isDone();
	}

	public Integer getRowProcessorCount() {
		return _rowProcessorCount;
	}

	private void initRowProcessingBeans(AnalysisJob job,
			AnalyzerBeanDescriptor descriptor,
			List<AnalyzerBeanInstance> analyzerBeanInstances,
			Map<Table, AnalysisRowProcessor> rowProcessors,
			DataContextProvider dataContextProvider) {
		try {

			Map<String, String[]> columnProperties = job.getColumnProperties();
			Set<Entry<String, String[]>> columnPropertyEntries = columnProperties
					.entrySet();
			for (Entry<String, String[]> entry : columnPropertyEntries) {

				String configuredName = entry.getKey();
				ConfiguredDescriptor configuredDescriptor = descriptor
						.getConfiguredDescriptor(configuredName);
				if (configuredDescriptor == null) {
					throw new IllegalStateException(
							"Analyzer class '"
									+ descriptor.getAnalyzerClass()
									+ "' does not specify @Configured field or method with name: "
									+ configuredName);
				}

				if (configuredDescriptor.isArray()) {
					// For each @Configured column-array we will instantiate one
					// bean per represented table

					String[] columnNames = entry.getValue();
					Column[] columns = dataContextProvider.getSchemaNavigator()
							.convertToColumns(columnNames);
					Table[] tables = MetaModelHelper.getTables(columns);

					for (Table table : tables) {
						AnalysisRowProcessor rowProcessor = rowProcessors
								.get(table);
						if (rowProcessor == null) {
							rowProcessor = new AnalysisRowProcessor(
									dataContextProvider);
							rowProcessors.put(table, rowProcessor);
						}

						Column[] columnsForAnalyzer = MetaModelHelper
								.getTableColumns(table, columns);
						rowProcessor.addColumns(columnsForAnalyzer);

						AnalyzerBeanInstance analyzerBeanInstance = instantiateAnalyzerBean(descriptor);
						analyzerBeanInstances.add(analyzerBeanInstance);

						// Add a callback for assigning @Configured properties
						AssignConfiguredRowProcessingCallback assignConfiguredCallback = new AssignConfiguredRowProcessingCallback(
								job, dataContextProvider.getSchemaNavigator(),
								columnsForAnalyzer);
						analyzerBeanInstance.getAssignConfiguredCallbacks()
								.add(assignConfiguredCallback);

						// Add a callback for executing the run(...) method
						analyzerBeanInstance.getRunCallbacks().add(
								new RunRowProcessorsCallback(rowProcessor));
					}
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not initialize analyzer based on job: " + job, e);
		}
	}

	private AnalyzerBeanInstance instantiateAnalyzerBean(
			AnalyzerBeanDescriptor descriptor) {
		try {
			Object analyzerBean = descriptor.getAnalyzerClass().newInstance();
			return new AnalyzerBeanInstance(analyzerBean, descriptor);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not instantiate analyzer bean type: "
							+ descriptor.getAnalyzerClass(), e);
		}
	}

	/**
	 * Categorize jobs corresponding to their execution type
	 * 
	 * TODO: Perhaps we can optimize a little bit if there are any beans that
	 * implement BOTH ExploringAnalyzer and RowProcessingAnalyzer. In such cases
	 * the RowProcessing execution should be used if more analyzers require the
	 * same data and the Exploring execution if not.
	 */
	private void categorizeJobs(DescriptorProvider descriptorProvider,
			List<AnalysisJob> explorerJobs, List<AnalysisJob> rowProcessingJobs)
			throws IllegalStateException {
		for (AnalysisJob job : _jobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptorProvider
					.getDescriptorForClass(analyzerClass);
			if (descriptor == null) {
				throw new IllegalStateException("No descriptor found for "
						+ analyzerClass);
			}
			if (descriptor.isExploringAnalyzer()) {
				explorerJobs.add(job);
			} else if (descriptor.isRowProcessingAnalyzer()) {
				rowProcessingJobs.add(job);
			}
		}
	}

	public CollectionProvider getCollectionProvider() {
		return _collectionProvider;
	}

	public void setCollectionProvider(CollectionProvider collectionProvider) {
		_collectionProvider = collectionProvider;
	}

	public DescriptorProvider getDescriptorProvider() {
		return _descriptorProvider;
	}

	public void setDescriptorProvider(DescriptorProvider descriptorProvider) {
		_descriptorProvider = descriptorProvider;
	}

	public ConcurrencyProvider getConcurrencyProvider() {
		return _concurrencyProvider;
	}

	public void setConcurrencyProvider(ConcurrencyProvider concurrencyProvider) {
		_concurrencyProvider = concurrencyProvider;
	}
}
