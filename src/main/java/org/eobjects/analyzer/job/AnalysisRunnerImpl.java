package org.eobjects.analyzer.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.SingleDataContextProvider;
import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.ConfiguredDescriptor;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.descriptors.JobListDescriptorProvider;
import org.eobjects.analyzer.job.concurrent.CompletionListener;
import org.eobjects.analyzer.job.concurrent.ConcurrencyProvider;
import org.eobjects.analyzer.job.concurrent.ScheduleTasksCompletionListener;
import org.eobjects.analyzer.job.concurrent.SingleThreadedConcurrencyProvider;
import org.eobjects.analyzer.job.concurrent.WaitableCompletionListener;
import org.eobjects.analyzer.job.tasks.AssignAndInitializeTask;
import org.eobjects.analyzer.job.tasks.CollectResultsAndCloseAnalyzerBeanTask;
import org.eobjects.analyzer.lifecycle.AnalyzerBeanInstance;
import org.eobjects.analyzer.lifecycle.AssignConfiguredCallback;
import org.eobjects.analyzer.lifecycle.AssignConfiguredRowProcessingCallback;
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
	private AnalyzerBeansConfiguration _configuration;
	private Queue<AnalyzerResult> _result;
	private Integer _rowProcessorAnalyzersCount;
	private WaitableCompletionListener _closeCompletionListener;

	public AnalysisRunnerImpl(AnalyzerBeansConfiguration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException("configuration cannot be null");
		}
		_configuration = configuration;
	}

	@Override
	public void addJob(AnalysisJob job) {
		_jobs.add(job);
		_rowProcessorAnalyzersCount = null;
	}

	@Override
	public void run(DataContext dataContext) {
		run(new SingleDataContextProvider(dataContext));
	}

	@Override
	public void run(DataContextProvider dataContextProvider) {
		if (logger.isInfoEnabled()) {
			logger.info("run(...) invoked.");
			logger.info("jobs: " + _jobs.size());
			for (AnalysisJob job : _jobs) {
				logger.info(" - " + job);
			}
		}

		DescriptorProvider descriptorProvider = _configuration
				.getDescriptorProvider();
		if (descriptorProvider == null) {
			descriptorProvider = new JobListDescriptorProvider(_jobs);
		}
		CollectionProvider collectionProvider = _configuration
				.getCollectionProvider();
		if (collectionProvider == null) {
			collectionProvider = new BerkeleyDbCollectionProvider();
		}
		ConcurrencyProvider concurrencyProvider = _configuration
				.getConcurrencyProvider();
		if (concurrencyProvider == null) {
			concurrencyProvider = new SingleThreadedConcurrencyProvider();
		}
		if (_result == null) {
			_result = new LinkedBlockingQueue<AnalyzerResult>();
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
		_rowProcessorAnalyzersCount = rowProcessors.size();

		// Add shared callbacks
		InitializeCallback initializeCallback = new InitializeCallback();
		ReturnResultsCallback returnResultsCallback = new ReturnResultsCallback(
				_result);
		CloseCallback closeCallback = new CloseCallback();

		Collection<Callable<?>> initializeAnalyzersTasks = new LinkedList<Callable<?>>();
		Collection<Callable<?>> runAnalyzersTasks = new LinkedList<Callable<?>>();
		Collection<Callable<?>> closeAnalyzersTasks = new LinkedList<Callable<?>>();

		// create the tasks for cleaning up after running the analyzers
		_closeCompletionListener = new WaitableCompletionListener(
				analyzerBeanInstances.size());
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			CollectResultsAndCloseAnalyzerBeanTask closeTask = new CollectResultsAndCloseAnalyzerBeanTask(
					_closeCompletionListener, analyzerBeanInstance);
			closeAnalyzersTasks.add(closeTask);
		}

		// create the tasks for running the analyzers
		int numRunTasks = rowProcessors.size() + analyzerBeanInstances.size();
		CompletionListener runCompletionListener = new ScheduleTasksCompletionListener(
				concurrencyProvider, numRunTasks, closeAnalyzersTasks);
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			Callable<Object> runTask = analyzerBeanInstance
					.createCallable(runCompletionListener);
			runAnalyzersTasks.add(runTask);
		}
		for (AnalysisRowProcessor analysisRowProcessor : rowProcessors.values()) {
			Callable<Object> runTask = analysisRowProcessor
					.createCallable(runCompletionListener);
			runAnalyzersTasks.add(runTask);
		}

		// create the tasks for initializing the analyzers
		CompletionListener initializeCompletionListener = new ScheduleTasksCompletionListener(
				concurrencyProvider, analyzerBeanInstances.size(),
				runAnalyzersTasks);

		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			Callable<Object> initializeTask = new AssignAndInitializeTask(
					initializeCompletionListener, analyzerBeanInstance,
					collectionProvider, dataContextProvider,
					initializeCallback, returnResultsCallback, closeCallback);
			initializeAnalyzersTasks.add(initializeTask);
		}

		// begin!
		new ScheduleTasksCompletionListener(concurrencyProvider, 1,
				initializeAnalyzersTasks).onComplete();

		logger.info("run(...) returning.");
	}

	@Override
	public List<AnalyzerResult> getResults() {
		while (!isDone()) {
			try {
				logger.debug("_closeCompletionListener.await()");
				_closeCompletionListener.await();
			} catch (Exception e) {
				logger.error("Unexpected error while retreiving results", e);
			}
		}
		return new ArrayList<AnalyzerResult>(_result);
	}

	@Override
	public boolean isDone() {
		return _closeCompletionListener.isDone();
	}

	public Integer getRowProcessorCount() {
		return _rowProcessorAnalyzersCount;
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
					analyzerBeanInstance.getAssignConfiguredCallbacks().add(
							assignConfiguredCallback);

					// Add a callback for executing the run(...) method
					analyzerBeanInstance.getRunCallbacks().add(
							new RunRowProcessorsCallback(rowProcessor));
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
}
