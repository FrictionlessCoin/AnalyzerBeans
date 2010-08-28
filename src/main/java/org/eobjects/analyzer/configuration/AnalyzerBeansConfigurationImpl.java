package org.eobjects.analyzer.configuration;

import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.job.concurrent.TaskRunner;
import org.eobjects.analyzer.lifecycle.CollectionProvider;
import org.eobjects.analyzer.reference.ReferenceDataCatalog;

public final class AnalyzerBeansConfigurationImpl implements
		AnalyzerBeansConfiguration {

	private static final long serialVersionUID = 1L;

	private final transient DescriptorProvider _descriptorProvider;
	private final transient CollectionProvider _collectionProvider;
	private final transient TaskRunner _taskRunner;
	private final DatastoreCatalog _datastoreCatalog;
	private final ReferenceDataCatalog _referenceDataCatalog;

	public AnalyzerBeansConfigurationImpl(DatastoreCatalog datastoreCatalog,
			ReferenceDataCatalog referenceDataCatalog,
			DescriptorProvider descriptorProvider, TaskRunner taskRunner,
			CollectionProvider collectionProvider) {
		if (datastoreCatalog == null) {
			throw new IllegalArgumentException(
					"datastoreCatalog cannot be null");
		}
		if (referenceDataCatalog == null) {
			throw new IllegalArgumentException(
					"referenceDataCatalog cannot be null");
		}
		if (descriptorProvider == null) {
			throw new IllegalArgumentException(
					"descriptorProvider cannot be null");
		}
		if (taskRunner == null) {
			throw new IllegalArgumentException("taskRunner cannot be null");
		}
		if (collectionProvider == null) {
			throw new IllegalArgumentException(
					"collectionProvider cannot be null");
		}
		_datastoreCatalog = datastoreCatalog;
		_referenceDataCatalog = referenceDataCatalog;
		_descriptorProvider = descriptorProvider;
		_taskRunner = taskRunner;
		_collectionProvider = collectionProvider;
	}

	@Override
	public DatastoreCatalog getDatastoreCatalog() {
		return _datastoreCatalog;
	}

	@Override
	public ReferenceDataCatalog getReferenceDataCatalog() {
		return _referenceDataCatalog;
	}

	@Override
	public DescriptorProvider getDescriptorProvider() {
		return _descriptorProvider;
	}

	@Override
	public CollectionProvider getCollectionProvider() {
		return _collectionProvider;
	}

	@Override
	public TaskRunner getTaskRunner() {
		return _taskRunner;
	}

}
