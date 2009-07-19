package org.eobjects.analyzer.job;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.AnnotationScanner;
import org.eobjects.analyzer.descriptors.ConfiguredDescriptor;
import org.eobjects.analyzer.lifecycle.AnalyzerBeanInstance;
import org.eobjects.analyzer.lifecycle.AssignConfiguredCallback;
import org.eobjects.analyzer.lifecycle.AssignConfiguredRowProcessingCallback;
import org.eobjects.analyzer.lifecycle.AssignProvidedCallback;
import org.eobjects.analyzer.lifecycle.CloseCallback;
import org.eobjects.analyzer.lifecycle.InitializeCallback;
import org.eobjects.analyzer.lifecycle.ProvidedCollectionHandler;
import org.eobjects.analyzer.lifecycle.ReturnResultsCallback;
import org.eobjects.analyzer.lifecycle.RunExplorerCallback;
import org.eobjects.analyzer.lifecycle.RunRowProcessorsCallback;
import org.eobjects.analyzer.result.AnalysisResult;
import org.eobjects.analyzer.result.AnalysisResultImpl;
import org.eobjects.analyzer.util.SchemaNavigator;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.MetaModelHelper;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

public class AnalysisRunner {

	private List<AnalysisJob> jobs = new LinkedList<AnalysisJob>();
	private ProvidedCollectionHandler collectionProvider;
	private AnnotationScanner scanner;
	private AnalysisResultImpl result;
	private Integer rowProcessorCount;

	public void addJob(AnalysisJob job) {
		jobs.add(job);
		rowProcessorCount = null;
	}

	public AnalysisResult run(DataContext dataContext) {
		if (scanner == null) {
			scanner = new AnnotationScanner();
		}
		if (collectionProvider == null) {
			collectionProvider = new ProvidedCollectionHandler();
		}
		if (result == null) {
			result = new AnalysisResultImpl();
		}
		SchemaNavigator schemaNavigator = new SchemaNavigator(dataContext);
		Map<Class<?>, AnalyzerBeanDescriptor> descriptors = scanner
				.getDescriptors();
		List<AnalysisJob> explorerJobs = new LinkedList<AnalysisJob>();
		List<AnalysisJob> rowProcessingJobs = new LinkedList<AnalysisJob>();

		categorizeJobs(descriptors, explorerJobs, rowProcessingJobs);

		// Instantiate beans and set specific lifecycle-callbacks
		RunExplorerCallback runExplorerCallback = new RunExplorerCallback(
				dataContext);
		List<AnalyzerBeanInstance> analyzerBeanInstances = new LinkedList<AnalyzerBeanInstance>();
		for (AnalysisJob job : explorerJobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptors.get(analyzerClass);
			AnalyzerBeanInstance analyzer = instantiateAnalyzerBean(descriptor);
			analyzer.getRunCallbacks().add(runExplorerCallback);
			analyzer.getAssignConfiguredCallbacks().add(
					new AssignConfiguredCallback(job, schemaNavigator));
			analyzerBeanInstances.add(analyzer);
		}
		Map<Table, AnalysisRowProcessor> rowProcessors = new HashMap<Table, AnalysisRowProcessor>();
		for (AnalysisJob job : rowProcessingJobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptors.get(analyzerClass);
			initRowProcessingBeans(schemaNavigator, job, descriptor,
					analyzerBeanInstances, rowProcessors);
		}
		rowProcessorCount = rowProcessors.size();

		// Add shared callbacks
		InitializeCallback initializeCallback = new InitializeCallback();
		ReturnResultsCallback returnResultsCallback = new ReturnResultsCallback(result);
		CloseCallback closeCallback = new CloseCallback();
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			AssignProvidedCallback assignProvidedCallback = new AssignProvidedCallback(
					analyzerBeanInstance, collectionProvider);

			analyzerBeanInstance.getAssignProvidedCallbacks().add(
					assignProvidedCallback);
			analyzerBeanInstance.getInitializeCallbacks().add(
					initializeCallback);
			analyzerBeanInstance.getReturnResultsCallbacks().add(
					returnResultsCallback);
			analyzerBeanInstance.getCloseCallbacks().add(closeCallback);
		}

		// TODO: This is a very simple execution mechanism just for prototyping
		// purposes.
		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			analyzerBeanInstance.assignConfigured();
			analyzerBeanInstance.assignProvided();
			analyzerBeanInstance.initialize();
			analyzerBeanInstance.run();
		}

		for (AnalysisRowProcessor rowProcessor : rowProcessors.values()) {
			rowProcessor.run(dataContext);
		}

		for (AnalyzerBeanInstance analyzerBeanInstance : analyzerBeanInstances) {
			analyzerBeanInstance.returnResults();
			analyzerBeanInstance.close();
		}
		
		return result;
	}
	
	public AnalysisResult getResult() {
		return result;
	}
	
	public Integer getRowProcessorCount() {
		return rowProcessorCount;
	}

	private void initRowProcessingBeans(SchemaNavigator schemaNavigator,
			AnalysisJob job, AnalyzerBeanDescriptor descriptor,
			List<AnalyzerBeanInstance> analyzerBeanInstances,
			Map<Table, AnalysisRowProcessor> rowProcessors) {
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
					Column[] columns = schemaNavigator
							.convertToColumns(columnNames);
					Table[] tables = MetaModelHelper.getTables(columns);

					for (Table table : tables) {
						AnalysisRowProcessor rowProcessor = rowProcessors
								.get(table);
						if (rowProcessor == null) {
							rowProcessor = new AnalysisRowProcessor();
							rowProcessors.put(table, rowProcessor);
						}

						Column[] columnsForAnalyzer = MetaModelHelper
								.getTableColumns(table, columns);
						rowProcessor.addColumns(columnsForAnalyzer);

						AnalyzerBeanInstance analyzerBeanInstance = instantiateAnalyzerBean(descriptor);
						analyzerBeanInstances.add(analyzerBeanInstance);

						// Add a callback for assigning @Configured properties
						AssignConfiguredRowProcessingCallback assignConfiguredCallback = new AssignConfiguredRowProcessingCallback(
								job, schemaNavigator, columnsForAnalyzer);
						analyzerBeanInstance.getAssignConfiguredCallbacks()
								.add(assignConfiguredCallback);

						// Add a callback for executing the @Run method
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
	 */
	private void categorizeJobs(
			Map<Class<?>, AnalyzerBeanDescriptor> descriptors,
			List<AnalysisJob> explorerJobs, List<AnalysisJob> rowProcessingJobs) {
		for (AnalysisJob job : jobs) {
			Class<?> analyzerClass = job.getAnalyzerClass();
			AnalyzerBeanDescriptor descriptor = descriptors.get(analyzerClass);
			if (descriptor == null) {
				descriptor = new AnalyzerBeanDescriptor(analyzerClass);
				scanner.putDescriptor(analyzerClass, descriptor);
			}
			if (descriptor.isExploringExecutionType()) {
				explorerJobs.add(job);
			} else if (descriptor.isRowProcessingExecutionType()) {
				rowProcessingJobs.add(job);
			} else {
				throw new UnsupportedOperationException(
						"Analysis execution type "
								+ descriptor.getExecutionType()
								+ " is not supported by AnalysisRunner");
			}
		}
	}
}
