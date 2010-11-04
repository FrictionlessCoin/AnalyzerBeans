package org.eobjects.analyzer.job.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.JdbcDatastore;
import org.eobjects.analyzer.connection.OdbDatastore;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.BeanConfiguration;
import org.eobjects.analyzer.job.ComponentJob;
import org.eobjects.analyzer.job.ConfigurableBeanJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.FilterOutcome;
import org.eobjects.analyzer.job.InputColumnSourceJob;
import org.eobjects.analyzer.job.MergedOutcomeJob;
import org.eobjects.analyzer.job.Outcome;
import org.eobjects.analyzer.job.OutcomeSourceJob;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.job.concurrent.NestedTaskListener;
import org.eobjects.analyzer.job.concurrent.ScheduleTasksTaskListener;
import org.eobjects.analyzer.job.concurrent.TaskListener;
import org.eobjects.analyzer.job.concurrent.TaskRunnable;
import org.eobjects.analyzer.job.concurrent.TaskRunner;
import org.eobjects.analyzer.job.tasks.AssignCallbacksAndInitializeTask;
import org.eobjects.analyzer.job.tasks.CloseBeanTask;
import org.eobjects.analyzer.job.tasks.CollectResultsAndCloseAnalyzerBeanTask;
import org.eobjects.analyzer.job.tasks.ConsumeRowTask;
import org.eobjects.analyzer.job.tasks.RunRowProcessingPublisherTask;
import org.eobjects.analyzer.job.tasks.Task;
import org.eobjects.analyzer.lifecycle.AnalyzerBeanInstance;
import org.eobjects.analyzer.lifecycle.AnalyzerLifeCycleCallback;
import org.eobjects.analyzer.lifecycle.AssignConfiguredCallback;
import org.eobjects.analyzer.lifecycle.CloseCallback;
import org.eobjects.analyzer.lifecycle.FilterBeanInstance;
import org.eobjects.analyzer.lifecycle.InitializeCallback;
import org.eobjects.analyzer.lifecycle.LifeCycleCallback;
import org.eobjects.analyzer.lifecycle.ReturnResultsCallback;
import org.eobjects.analyzer.lifecycle.TransformerBeanInstance;
import org.eobjects.analyzer.reference.Function;
import org.eobjects.analyzer.storage.RowAnnotationFactory;
import org.eobjects.analyzer.storage.StorageProvider;
import org.eobjects.analyzer.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.data.DataSet;
import dk.eobjects.metamodel.data.Row;
import dk.eobjects.metamodel.query.Query;
import dk.eobjects.metamodel.query.SelectItem;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

public final class RowProcessingPublisher {

	private final static Logger logger = LoggerFactory.getLogger(RowProcessingPublisher.class);

	private final Set<Column> _physicalColumns = new HashSet<Column>();
	private final List<RowProcessingConsumer> _consumers = new ArrayList<RowProcessingConsumer>();
	private final AnalysisJob _job;
	private final StorageProvider _storageProvider;
	private final Table _table;
	private final TaskRunner _taskRunner;
	private final AnalysisListener _analysisListener;

	public RowProcessingPublisher(AnalysisJob job, StorageProvider storageProvider, Table table, TaskRunner taskRunner,
			AnalysisListener analysisListener) {
		if (job == null) {
			throw new IllegalArgumentException("AnalysisJob cannot be null");
		}
		if (storageProvider == null) {
			throw new IllegalArgumentException("CollectionProvider cannot be null");
		}
		if (table == null) {
			throw new IllegalArgumentException("Table cannot be null");
		}
		if (taskRunner == null) {
			throw new IllegalArgumentException("TaskRunner cannot be null");
		}
		if (analysisListener == null) {
			throw new IllegalArgumentException("AnalysisListener cannot be null");
		}
		_job = job;
		_storageProvider = storageProvider;
		_table = table;
		_taskRunner = taskRunner;
		_analysisListener = analysisListener;
	}

	public void addPhysicalColumns(Column... columns) {
		for (Column column : columns) {
			if (!_table.equals(column.getTable())) {
				throw new IllegalArgumentException("Column does not pertain to the correct table. Expected table: " + _table
						+ ", actual table: " + column.getTable());
			}
			_physicalColumns.add(column);
		}
	}

	public void run() {
		for (RowProcessingConsumer rowProcessingConsumer : _consumers) {
			if (rowProcessingConsumer instanceof AnalyzerConsumer) {
				AnalyzerJob analyzerJob = ((AnalyzerConsumer) rowProcessingConsumer).getComponentJob();
				_analysisListener.analyzerBegin(_job, analyzerJob);
			}
		}

		final DataContext dataContext = _job.getDataContextProvider().getDataContext();

		int expectedRows = -1;
		Query countQuery = dataContext.query().from(_table).selectCount().toQuery();
		DataSet countDataSet = dataContext.executeQuery(countQuery);
		if (countDataSet.next()) {
			Number count = (Number) countDataSet.getRow().getValue(0);
			if (count != null) {
				expectedRows = count.intValue();
			}
		}

		_analysisListener.rowProcessingBegin(_job, _table, expectedRows);

		final Column[] columnArray = _physicalColumns.toArray(new Column[_physicalColumns.size()]);

		final Query q = dataContext.query().from(_table).select(columnArray).toQuery();

		SelectItem countAllItem = null;
		if (useGroupByOptimization()) {
			logger.info("Using GROUP BY optimization");
			q.groupBy(columnArray);
			countAllItem = SelectItem.getCountAllItem();
			q.select(countAllItem);
		}

		logger.debug("Employing query for row processing: {}", q);

		final Iterable<RowProcessingConsumer> consumers = createProcessOrderedConsumerList(_consumers);
		if (logger.isDebugEnabled()) {
			logger.debug("Row processing order ({} consumers):", _consumers.size());
			int i = 1;
			for (RowProcessingConsumer rowProcessingConsumer : consumers) {
				logger.debug(" {}) {}", i, rowProcessingConsumer);
				i++;
			}
		}

		// TODO: Needs to delegate errors downstream
		final RowConsumerTaskListener taskListener = new RowConsumerTaskListener(_job, _analysisListener);
		final AtomicInteger rowNumber = new AtomicInteger(0);
		final DataSet dataSet = dataContext.executeQuery(q);

		// represents the distinct count of rows as well as the number of tasks
		// to execute
		int numTasks = 0;

		while (dataSet.next()) {
			if (taskListener.isErrornous()) {
				break;
			}
			Row metaModelRow = dataSet.getRow();
			ConsumeRowTask task = new ConsumeRowTask(consumers, _table, metaModelRow, countAllItem, rowNumber, _job,
					_analysisListener);
			_taskRunner.run(task, taskListener);
			numTasks++;
		}

		taskListener.awaitTasks(numTasks);

		dataSet.close();

		if (!taskListener.isErrornous()) {
			_analysisListener.rowProcessingSuccess(_job, _table);
		}
	}

	private boolean useGroupByOptimization() {
		if (_physicalColumns.size() > 10) {
			logger.info("Skipping GROUP BY optimization because of the high column amount");
			return false;
		}
		Datastore datastore = _job.getDataContextProvider().getDatastore();
		if (datastore == null) {
			logger.info("Skipping GROUP BY optimization because no datastore is attached to the DataContextProvider");
			return false;
		}

		if (datastore instanceof JdbcDatastore || datastore instanceof OdbDatastore) {
			return true;
		}

		return false;
	}

	protected static List<RowProcessingConsumer> createProcessOrderedConsumerList(
			Collection<? extends RowProcessingConsumer> consumers) {
		List<RowProcessingConsumer> result = new LinkedList<RowProcessingConsumer>();

		Collection<RowProcessingConsumer> remainingConsumers = new LinkedList<RowProcessingConsumer>(consumers);
		Set<InputColumn<?>> availableVirtualColumns = new HashSet<InputColumn<?>>();
		Set<Outcome> availableOutcomes = new HashSet<Outcome>();

		while (!remainingConsumers.isEmpty()) {
			boolean changed = false;
			for (Iterator<RowProcessingConsumer> it = remainingConsumers.iterator(); it.hasNext();) {
				RowProcessingConsumer consumer = it.next();

				boolean accepted = true;

				// make sure that any dependent filter outcome is evaluated
				// before this component
				accepted = consumer.satisfiedForFlowOrdering(availableOutcomes);

				// make sure that all the required colums are present
				if (accepted) {
					InputColumn<?>[] requiredInput = consumer.getRequiredInput();
					for (InputColumn<?> inputColumn : requiredInput) {
						if (!inputColumn.isPhysicalColumn()) {
							if (!availableVirtualColumns.contains(inputColumn)) {
								accepted = false;
								break;
							}
						}
					}
				}

				if (accepted) {
					result.add(consumer);
					it.remove();
					changed = true;

					ComponentJob componentJob = consumer.getComponentJob();

					if (componentJob instanceof InputColumnSourceJob) {
						InputColumn<?>[] output = ((InputColumnSourceJob) componentJob).getOutput();
						for (InputColumn<?> col : output) {
							availableVirtualColumns.add(col);
						}
					}

					if (componentJob instanceof OutcomeSourceJob) {
						Outcome[] outcomes = ((OutcomeSourceJob) componentJob).getOutcomes();
						for (Outcome outcome : outcomes) {
							availableOutcomes.add(outcome);
						}
					}
				}
			}

			if (!changed) {
				// should never happen, but if a bug enters the
				// algorithm this exception will quickly expose it
				throw new IllegalStateException("Could not detect next consumer in processing order");
			}
		}

		return result;
	}

	public void addRowProcessingAnalyzerBean(AnalyzerBeanInstance analyzerBeanInstance, AnalyzerJob analyzerJob,
			InputColumn<?>[] inputColumns) {
		addConsumer(new AnalyzerConsumer(_job, analyzerBeanInstance, analyzerJob, inputColumns, _analysisListener));
	}

	public void addTransformerBean(TransformerBeanInstance transformerBeanInstance, TransformerJob transformerJob,
			InputColumn<?>[] inputColumns) {
		addConsumer(new TransformerConsumer(_job, transformerBeanInstance, transformerJob, inputColumns, _analysisListener));
	}

	public void addFilterBean(FilterBeanInstance filterBeanInstance, FilterJob filterJob, InputColumn<?>[] inputColumns) {
		addConsumer(new FilterConsumer(_job, filterBeanInstance, filterJob, inputColumns, _analysisListener));
	}

	public void addMergedOutcomeJob(MergedOutcomeJob mergedOutcomeJob) {
		addConsumer(new MergedOutcomeConsumer(mergedOutcomeJob));
	}

	public boolean containsOutcome(Outcome prerequisiteOutcome) {
		for (RowProcessingConsumer consumer : _consumers) {
			if (consumer instanceof FilterConsumer) {
				FilterJob fj = (FilterJob) consumer.getComponentJob();
				FilterOutcome[] outcomes = fj.getOutcomes();
				for (FilterOutcome filterOutcome : outcomes) {
					if (filterOutcome.satisfiesRequirement(prerequisiteOutcome)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void addConsumer(RowProcessingConsumer consumer) {
		_consumers.add(consumer);
	}

	public List<TaskRunnable> createInitialTasks(TaskRunner taskRunner, Queue<AnalyzerJobResult> resultQueue,
			TaskListener rowProcessorPublishersTaskListener) {

		List<RowProcessingConsumer> configurableConsumers = CollectionUtils.filter(_consumers,
				new Function<RowProcessingConsumer, Boolean>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Boolean run(RowProcessingConsumer input) throws Exception {
						return input.getComponentJob() instanceof ConfigurableBeanJob<?>;
					}
				});
		int numConfigurableConsumers = configurableConsumers.size();

		TaskListener closeTaskListener = new NestedTaskListener("row processor consumers", numConfigurableConsumers,
				rowProcessorPublishersTaskListener);

		List<TaskRunnable> closeTasks = new ArrayList<TaskRunnable>(numConfigurableConsumers);
		for (RowProcessingConsumer consumer : configurableConsumers) {
			Task closeTask = createCloseTask(consumer, resultQueue);
			closeTasks.add(new TaskRunnable(closeTask, closeTaskListener));
		}

		TaskListener runCompletionListener = new ScheduleTasksTaskListener("run row processing", taskRunner, 1, closeTasks);

		Collection<TaskRunnable> runTasks = new ArrayList<TaskRunnable>(1);
		RunRowProcessingPublisherTask runTask = new RunRowProcessingPublisherTask(this);
		runTasks.add(new TaskRunnable(runTask, runCompletionListener));

		TaskListener initTaskListener = new ScheduleTasksTaskListener("initialize row consumers", taskRunner,
				numConfigurableConsumers, runTasks);

		// Ticket #459: The RowAnnotationFactory should be shared by all
		// components within the same RowProcessingPublisher
		RowAnnotationFactory rowAnnotationFactory = _storageProvider.createRowAnnotationFactory();

		List<TaskRunnable> initTasks = new ArrayList<TaskRunnable>(numConfigurableConsumers);
		for (RowProcessingConsumer consumer : configurableConsumers) {
			initTasks.add(createInitTask(consumer, rowAnnotationFactory, initTaskListener, resultQueue));
		}
		return initTasks;
	}

	private Task createCloseTask(RowProcessingConsumer consumer, Queue<AnalyzerJobResult> resultQueue) {
		if (consumer instanceof TransformerConsumer || consumer instanceof FilterConsumer) {
			return new CloseBeanTask(consumer.getBeanInstance());
		} else if (consumer instanceof AnalyzerConsumer) {
			return new CollectResultsAndCloseAnalyzerBeanTask(((AnalyzerBeanInstance) consumer.getBeanInstance()));
		} else {
			throw new IllegalStateException("Unknown consumer type: " + consumer);
		}
	}

	private TaskRunnable createInitTask(RowProcessingConsumer consumer, RowAnnotationFactory rowAnnotationFactory,
			TaskListener listener, Queue<AnalyzerJobResult> resultQueue) {
		ComponentJob componentJob = consumer.getComponentJob();
		BeanConfiguration configuration = ((ConfigurableBeanJob<?>) componentJob).getConfiguration();

		LifeCycleCallback assignConfiguredCallback = new AssignConfiguredCallback(configuration);
		LifeCycleCallback initializeCallback = new InitializeCallback();
		LifeCycleCallback closeCallback = new CloseCallback();

		DataContextProvider dataContextProvider = _job.getDataContextProvider();

		Task task;
		if (consumer instanceof TransformerConsumer) {
			TransformerConsumer transformerConsumer = (TransformerConsumer) consumer;
			TransformerBeanInstance transformerBeanInstance = transformerConsumer.getBeanInstance();

			task = new AssignCallbacksAndInitializeTask(transformerBeanInstance, _storageProvider, rowAnnotationFactory,
					dataContextProvider, assignConfiguredCallback, initializeCallback, closeCallback);
		} else if (consumer instanceof FilterConsumer) {
			FilterConsumer filterConsumer = (FilterConsumer) consumer;
			FilterBeanInstance filterBeanInstance = filterConsumer.getBeanInstance();

			task = new AssignCallbacksAndInitializeTask(filterBeanInstance, _storageProvider, rowAnnotationFactory,
					dataContextProvider, assignConfiguredCallback, initializeCallback, closeCallback);
		} else if (consumer instanceof AnalyzerConsumer) {
			AnalyzerConsumer analyzerConsumer = (AnalyzerConsumer) consumer;
			AnalyzerBeanInstance analyzerBeanInstance = analyzerConsumer.getBeanInstance();
			AnalyzerLifeCycleCallback returnResultsCallback = new ReturnResultsCallback(_job,
					analyzerConsumer.getComponentJob(), resultQueue, _analysisListener);

			task = new AssignCallbacksAndInitializeTask(analyzerBeanInstance, _storageProvider, rowAnnotationFactory,
					dataContextProvider, assignConfiguredCallback, initializeCallback, null, returnResultsCallback,
					closeCallback);
		} else {
			throw new IllegalStateException("Unknown consumer type: " + consumer);
		}
		return new TaskRunnable(task, listener);
	}

	@Override
	public String toString() {
		return "RowProcessingPublisher[table=" + _table.getQualifiedLabel() + ", consumers=" + _consumers.size() + "]";
	}
}
