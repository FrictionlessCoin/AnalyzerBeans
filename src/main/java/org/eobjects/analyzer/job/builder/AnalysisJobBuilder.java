package org.eobjects.analyzer.job.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eobjects.analyzer.beans.api.ExploringAnalyzer;
import org.eobjects.analyzer.beans.api.Filter;
import org.eobjects.analyzer.beans.api.RowProcessingAnalyzer;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.data.DataTypeFamily;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.MetaModelInputColumn;
import org.eobjects.analyzer.data.MutableInputColumn;
import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.FilterBeanDescriptor;
import org.eobjects.analyzer.descriptors.TransformerBeanDescriptor;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.IdGenerator;
import org.eobjects.analyzer.job.ImmutableAnalysisJob;
import org.eobjects.analyzer.job.PrefixedIdGenerator;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.util.SchemaNavigator;

import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

/**
 * Main entry to the Job Builder API. Use this class to build jobs either
 * programmatically, while parsing a marshalled job-representation (such as an
 * XML job definition) or for making an end-user able to build a job in a UI.
 * 
 * The AnalysisJobBuilder supports a wide variety of listeners to make it
 * possible to be informed of changes to the state and dependencies between the
 * components/beans that defines the job.
 * 
 * @author Kasper Sørensen
 */
public class AnalysisJobBuilder {

	private final AnalyzerBeansConfiguration _configuration;
	private final IdGenerator _transformedColumnIdGenerator = new PrefixedIdGenerator("trans");

	// the configurable components
	private DataContextProvider _dataContextProvider;
	private final List<MetaModelInputColumn> _sourceColumns = new ArrayList<MetaModelInputColumn>();
	private final List<FilterJobBuilder<?, ?>> _filterJobBuilders = new ArrayList<FilterJobBuilder<?, ?>>();
	private final List<TransformerJobBuilder<?>> _transformerJobBuilders = new ArrayList<TransformerJobBuilder<?>>();
	private final List<AnalyzerJobBuilder<?>> _analyzerJobBuilders = new ArrayList<AnalyzerJobBuilder<?>>();

	// listeners, typically for UI that uses the builders
	private final List<SourceColumnChangeListener> _sourceColumnListeners = new LinkedList<SourceColumnChangeListener>();
	private final List<AnalyzerChangeListener> _analyzerChangeListeners = new LinkedList<AnalyzerChangeListener>();
	private final List<TransformerChangeListener> _transformerChangeListeners = new LinkedList<TransformerChangeListener>();
	private final List<FilterChangeListener> _filterChangeListeners = new LinkedList<FilterChangeListener>();

	public AnalysisJobBuilder(AnalyzerBeansConfiguration configuration) {
		_configuration = configuration;
	}

	public AnalysisJobBuilder setDatastore(String datastoreName) {
		Datastore datastore = _configuration.getDatastoreCatalog().getDatastore(datastoreName);
		if (datastore == null) {
			throw new IllegalArgumentException("No such datastore: " + datastoreName);
		}
		return setDatastore(datastore);
	}

	public AnalysisJobBuilder setDatastore(Datastore datastore) {
		if (datastore == null) {
			throw new IllegalArgumentException("Datastore cannot be null");
		}
		DataContextProvider dataContextProvider = datastore.getDataContextProvider();
		return setDataContextProvider(dataContextProvider);
	}

	public AnalysisJobBuilder setDataContextProvider(DataContextProvider dataContextProvider) {
		if (dataContextProvider == null) {
			throw new IllegalArgumentException("DataContextProvider cannot be null");
		}
		_dataContextProvider = dataContextProvider;
		return this;
	}

	public DataContextProvider getDataContextProvider() {
		return _dataContextProvider;
	}

	public AnalysisJobBuilder addSourceColumn(Column column) {
		MetaModelInputColumn inputColumn = new MetaModelInputColumn(column);
		return addSourceColumn(inputColumn);
	}

	public AnalysisJobBuilder addSourceColumn(MetaModelInputColumn inputColumn) {
		if (!_sourceColumns.contains(inputColumn)) {
			_sourceColumns.add(inputColumn);

			List<SourceColumnChangeListener> listeners = new ArrayList<SourceColumnChangeListener>(_sourceColumnListeners);
			for (SourceColumnChangeListener listener : listeners) {
				listener.onAdd(inputColumn);
			}
		}
		return this;
	}

	public AnalysisJobBuilder addSourceColumns(Column... columns) {
		for (Column column : columns) {
			addSourceColumn(column);
		}
		return this;
	}

	public AnalysisJobBuilder addSourceColumns(MetaModelInputColumn... inputColumns) {
		for (MetaModelInputColumn metaModelInputColumn : inputColumns) {
			addSourceColumn(metaModelInputColumn);
		}
		return this;
	}

	public AnalysisJobBuilder addSourceColumns(String... columnNames) {
		if (_dataContextProvider == null) {
			throw new IllegalStateException(
					"Cannot add source columns by name when no Datastore or DataContextProvider has been set");
		}
		SchemaNavigator schemaNavigator = _dataContextProvider.getSchemaNavigator();
		Column[] columns = new Column[columnNames.length];
		for (int i = 0; i < columns.length; i++) {
			String columnName = columnNames[i];
			Column column = schemaNavigator.convertToColumn(columnName);
			if (column == null) {
				throw new IllegalArgumentException("No such column: " + columnName);
			}
			columns[i] = column;
		}
		return addSourceColumns(columns);
	}

	public AnalysisJobBuilder removeSourceColumn(Column column) {
		MetaModelInputColumn inputColumn = new MetaModelInputColumn(column);
		return removeSourceColumn(inputColumn);
	}

	public AnalysisJobBuilder removeSourceColumn(MetaModelInputColumn inputColumn) {
		_sourceColumns.remove(inputColumn);
		List<SourceColumnChangeListener> listeners = new ArrayList<SourceColumnChangeListener>(_sourceColumnListeners);
		for (SourceColumnChangeListener listener : listeners) {
			listener.onRemove(inputColumn);
		}
		return this;
	}

	public boolean containsSourceColumn(Column column) {
		for (MetaModelInputColumn sourceColumn : _sourceColumns) {
			if (sourceColumn.getPhysicalColumn().equals(column)) {
				return true;
			}
		}
		return false;
	}

	public List<MetaModelInputColumn> getSourceColumns() {
		return Collections.unmodifiableList(_sourceColumns);
	}

	public <T extends Transformer<?>> TransformerJobBuilder<T> addTransformer(Class<T> transformerClass) {
		TransformerBeanDescriptor<T> descriptor = _configuration.getDescriptorProvider()
				.getTransformerBeanDescriptorForClass(transformerClass);
		if (descriptor == null) {
			throw new IllegalArgumentException("No descriptor found for: " + transformerClass);
		}
		return addTransformer(descriptor);
	}

	public List<TransformerJobBuilder<?>> getTransformerJobBuilders() {
		return Collections.unmodifiableList(_transformerJobBuilders);
	}

	public <T extends Transformer<?>> TransformerJobBuilder<T> addTransformer(TransformerBeanDescriptor<T> descriptor) {
		TransformerJobBuilder<T> tjb = new TransformerJobBuilder<T>(descriptor, _transformedColumnIdGenerator,
				_transformerChangeListeners);
		_transformerJobBuilders.add(tjb);

		// make a copy since some of the listeners may add additional listeners
		// which will otherwise cause ConcurrentModificationExceptions
		List<TransformerChangeListener> listeners = new ArrayList<TransformerChangeListener>(_transformerChangeListeners);
		for (TransformerChangeListener listener : listeners) {
			listener.onAdd(tjb);
		}
		return tjb;
	}

	public AnalysisJobBuilder removeTransformer(TransformerJobBuilder<?> tjb) {
		_transformerJobBuilders.remove(tjb);

		// make a copy since some of the listeners may add additional listeners
		// which will otherwise cause ConcurrentModificationExceptions
		List<TransformerChangeListener> listeners = new ArrayList<TransformerChangeListener>(_transformerChangeListeners);
		for (TransformerChangeListener listener : listeners) {
			listener.onOutputChanged(tjb, new LinkedList<MutableInputColumn<?>>());
			listener.onRemove(tjb);
		}
		return this;
	}

	public <F extends Filter<C>, C extends Enum<C>> FilterJobBuilder<F, C> addFilter(Class<F> filterClass) {
		FilterBeanDescriptor<F, C> descriptor = _configuration.getDescriptorProvider().getFilterBeanDescriptorForClass(
				filterClass);
		if (descriptor == null) {
			throw new IllegalArgumentException("No descriptor found for: " + filterClass);
		}
		return addFilter(descriptor);
	}

	public <F extends Filter<C>, C extends Enum<C>> FilterJobBuilder<F, C> addFilter(FilterBeanDescriptor<F, C> descriptor) {
		FilterJobBuilder<F, C> fjb = new FilterJobBuilder<F, C>(descriptor);
		_filterJobBuilders.add(fjb);

		List<FilterChangeListener> listeners = new ArrayList<FilterChangeListener>(_filterChangeListeners);
		for (FilterChangeListener listener : listeners) {
			listener.onAdd(fjb);
		}
		return fjb;
	}

	public AnalysisJobBuilder removeFilter(FilterJobBuilder<?, ?> fjb) {
		_filterJobBuilders.remove(fjb);

		List<FilterChangeListener> listeners = new ArrayList<FilterChangeListener>(_filterChangeListeners);
		for (FilterChangeListener listener : listeners) {
			listener.onRemove(fjb);
		}
		return this;
	}

	public List<AnalyzerJobBuilder<?>> getAnalyzerJobBuilders() {
		return Collections.unmodifiableList(_analyzerJobBuilders);
	}

	public List<FilterJobBuilder<?, ?>> getFilterJobBuilders() {
		return Collections.unmodifiableList(_filterJobBuilders);
	}

	public <A extends ExploringAnalyzer<?>> ExploringAnalyzerJobBuilder<A> addExploringAnalyzer(Class<A> analyzerClass) {
		AnalyzerBeanDescriptor<A> descriptor = _configuration.getDescriptorProvider().getAnalyzerBeanDescriptorForClass(
				analyzerClass);
		if (descriptor == null) {
			throw new IllegalArgumentException("No descriptor found for: " + analyzerClass);
		}
		ExploringAnalyzerJobBuilder<A> analyzerJobBuilder = new ExploringAnalyzerJobBuilder<A>(descriptor);
		_analyzerJobBuilders.add(analyzerJobBuilder);

		// make a copy since some of the listeners may add additional listeners
		// which will otherwise cause ConcurrentModificationExceptions
		List<AnalyzerChangeListener> listeners = new ArrayList<AnalyzerChangeListener>(_analyzerChangeListeners);
		for (AnalyzerChangeListener listener : listeners) {
			listener.onAdd(analyzerJobBuilder);
		}
		return analyzerJobBuilder;
	}

	public <A extends RowProcessingAnalyzer<?>> RowProcessingAnalyzerJobBuilder<A> addRowProcessingAnalyzer(
			Class<A> analyzerClass) {
		AnalyzerBeanDescriptor<A> descriptor = _configuration.getDescriptorProvider().getAnalyzerBeanDescriptorForClass(
				analyzerClass);
		if (descriptor == null) {
			throw new IllegalArgumentException("No descriptor found for: " + analyzerClass);
		}
		RowProcessingAnalyzerJobBuilder<A> analyzerJobBuilder = new RowProcessingAnalyzerJobBuilder<A>(this, descriptor);
		_analyzerJobBuilders.add(analyzerJobBuilder);

		// make a copy since some of the listeners may add additional listeners
		// which will otherwise cause ConcurrentModificationExceptions
		List<AnalyzerChangeListener> listeners = new ArrayList<AnalyzerChangeListener>(_analyzerChangeListeners);
		for (AnalyzerChangeListener listener : listeners) {
			listener.onAdd(analyzerJobBuilder);
		}
		return analyzerJobBuilder;
	}

	public AnalysisJobBuilder removeAnalyzer(RowProcessingAnalyzerJobBuilder<?> ajb) {
		_analyzerJobBuilders.remove(ajb);
		for (AnalyzerChangeListener listener : _analyzerChangeListeners) {
			listener.onRemove(ajb);
		}
		return this;
	}

	public Collection<InputColumn<?>> getAvailableInputColumns(DataTypeFamily dataTypeFamily) {
		if (dataTypeFamily == null) {
			dataTypeFamily = DataTypeFamily.UNDEFINED;
		}

		List<InputColumn<?>> result = new ArrayList<InputColumn<?>>();
		List<MetaModelInputColumn> sourceColumns = getSourceColumns();
		for (MetaModelInputColumn sourceColumn : sourceColumns) {
			if (dataTypeFamily == DataTypeFamily.UNDEFINED || sourceColumn.getDataTypeFamily() == dataTypeFamily) {
				result.add(sourceColumn);
			}
		}

		for (TransformerJobBuilder<?> transformerJobBuilder : _transformerJobBuilders) {
			List<MutableInputColumn<?>> outputColumns = transformerJobBuilder.getOutputColumns();
			for (MutableInputColumn<?> outputColumn : outputColumns) {
				if (dataTypeFamily == DataTypeFamily.UNDEFINED || outputColumn.getDataTypeFamily() == dataTypeFamily) {
					result.add(outputColumn);
				}
			}
		}

		return result;
	}

	/**
	 * Used to verify whether or not the builder's configuration is valid and
	 * all properties are satisfied.
	 * 
	 * @param throwException
	 *            whether or not an exception should be thrown in case of
	 *            invalid configuration. Typically an exception message will
	 *            contain more detailed information about the cause of the
	 *            validation error, whereas a boolean contains no details.
	 * @return true if the analysis job builder is correctly configured
	 * @throws IllegalStateException
	 */
	public boolean isConfigured(final boolean throwException) throws IllegalStateException {
		if (_dataContextProvider == null) {
			if (throwException) {
				throw new IllegalStateException("No DataContextProvider set");
			}
			return false;
		}

		if (_analyzerJobBuilders.isEmpty()) {
			if (throwException) {
				throw new IllegalStateException("No Analyzers in job");
			}
			return false;
		}

		for (FilterJobBuilder<?, ?> fjb : _filterJobBuilders) {
			if (!fjb.isConfigured(throwException)) {
				return false;
			}
		}

		for (TransformerJobBuilder<?> tjb : _transformerJobBuilders) {
			if (!tjb.isConfigured(throwException)) {
				return false;
			}
		}

		for (AnalyzerJobBuilder<?> ajb : _analyzerJobBuilders) {
			if (!ajb.isConfigured(throwException)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Used to verify whether or not the builder's configuration is valid and
	 * all properties are satisfied.
	 * 
	 * @return true if the analysis job builder is correctly configured
	 */
	public boolean isConfigured() {
		return isConfigured(false);
	}

	public AnalysisJob toAnalysisJob() throws IllegalStateException {
		if (!isConfigured(true)) {
			throw new IllegalStateException("Analysis job is not correctly configured");
		}

		Collection<FilterJob> filterJobs = new LinkedList<FilterJob>();
		for (FilterJobBuilder<?, ?> fjb : _filterJobBuilders) {
			try {
				FilterJob filterJob = fjb.toFilterJob();
				filterJobs.add(filterJob);
			} catch (IllegalStateException e) {
				throw new IllegalStateException("Could not create filter job from builder: " + fjb + ", (" + e.getMessage()
						+ ")", e);
			}
		}

		Collection<TransformerJob> transformerJobs = new LinkedList<TransformerJob>();
		for (TransformerJobBuilder<?> tjb : _transformerJobBuilders) {
			try {
				TransformerJob transformerJob = tjb.toTransformerJob();
				transformerJobs.add(transformerJob);
			} catch (IllegalStateException e) {
				throw new IllegalStateException("Could not create transformer job from builder: " + tjb + ", ("
						+ e.getMessage() + ")", e);
			}
		}

		Collection<AnalyzerJob> analyzerJobs = new LinkedList<AnalyzerJob>();
		for (AnalyzerJobBuilder<?> ajb : _analyzerJobBuilders) {
			try {
				AnalyzerJob[] analyzerJob = ajb.toAnalyzerJobs();
				for (AnalyzerJob job : analyzerJob) {
					analyzerJobs.add(job);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Could not create analyzer job from builder: " + ajb + ", ("
						+ e.getMessage() + ")", e);
			}
		}

		return new ImmutableAnalysisJob(_dataContextProvider, _sourceColumns, filterJobs, transformerJobs, analyzerJobs);
	}

	public InputColumn<?> getSourceColumnByName(String name) {
		if (name != null) {
			for (MetaModelInputColumn inputColumn : _sourceColumns) {
				if (name.equals(inputColumn.getName())) {
					return inputColumn;
				}
			}
		}
		return null;
	}

	/**
	 * Convenience method to get all input columns (both source or from
	 * transformers) that comply to a given data type family.
	 * 
	 * @param dataTypeFamily
	 * @return
	 */
	public List<InputColumn<?>> getInputColumns(DataTypeFamily dataTypeFamily) {
		if (dataTypeFamily == null) {
			throw new IllegalArgumentException("dataTypeFamily cannot be null. Use " + DataTypeFamily.UNDEFINED
					+ " for all input columns");
		}
		List<InputColumn<?>> inputColumns = new ArrayList<InputColumn<?>>();
		List<MetaModelInputColumn> sourceColumns = getSourceColumns();
		for (MetaModelInputColumn col : sourceColumns) {
			if (dataTypeFamily == DataTypeFamily.UNDEFINED || col.getDataTypeFamily() == dataTypeFamily) {
				inputColumns.add(col);
			}
		}

		List<TransformerJobBuilder<?>> transformerJobBuilders = getTransformerJobBuilders();
		for (TransformerJobBuilder<?> transformerJobBuilder : transformerJobBuilders) {
			List<MutableInputColumn<?>> outputColumns = transformerJobBuilder.getOutputColumns();
			for (MutableInputColumn<?> col : outputColumns) {
				if (dataTypeFamily == DataTypeFamily.UNDEFINED || col.getDataTypeFamily() == dataTypeFamily) {
					inputColumns.add(col);
				}
			}
		}

		return inputColumns;
	}

	protected Table getOriginatingTable(InputColumn<?> inputColumn) {
		if (inputColumn.isPhysicalColumn()) {
			return inputColumn.getPhysicalColumn().getTable();
		}
		for (TransformerJobBuilder<?> tjb : _transformerJobBuilders) {
			if (tjb.getOutputColumns().contains(inputColumn)) {
				List<InputColumn<?>> inputColumns = tjb.getInputColumns();
				assert !inputColumns.isEmpty();

				return getOriginatingTable(inputColumns.get(0));
			}
		}
		throw new IllegalStateException("Could not find originating table for column: " + inputColumn);
	}

	protected Table getOriginatingTable(AbstractBeanWithInputColumnsBuilder<?, ?, ?> beanJobBuilder) {
		List<InputColumn<?>> inputColumns = beanJobBuilder.getInputColumns();
		if (inputColumns.isEmpty()) {
			return null;
		} else {
			return getOriginatingTable(inputColumns.get(0));
		}
	}

	public List<AbstractBeanWithInputColumnsBuilder<?, ?, ?>> getAvailableUnfilteredBeans(
			FilterJobBuilder<?, ?> filterJobBuilder) {
		List<AbstractBeanWithInputColumnsBuilder<?, ?, ?>> result = new ArrayList<AbstractBeanWithInputColumnsBuilder<?, ?, ?>>();
		if (filterJobBuilder.isConfigured()) {
			final Table requiredTable = getOriginatingTable(filterJobBuilder);

			for (FilterJobBuilder<?, ?> fjb : _filterJobBuilders) {
				if (fjb != filterJobBuilder) {
					if (fjb.getRequirement() == null) {
						Table foundTable = getOriginatingTable(fjb);
						if (requiredTable == null || requiredTable.equals(foundTable)) {
							result.add(fjb);
						}
					}
				}
			}

			for (TransformerJobBuilder<?> tjb : _transformerJobBuilders) {
				if (tjb.getRequirement() == null) {
					Table foundTable = getOriginatingTable(tjb);
					if (requiredTable == null || requiredTable.equals(foundTable)) {
						result.add(tjb);
					}
				}
			}

			for (AnalyzerJobBuilder<?> ajb : _analyzerJobBuilders) {
				if (ajb instanceof RowProcessingAnalyzerJobBuilder<?>) {
					RowProcessingAnalyzerJobBuilder<?> rpajb = (RowProcessingAnalyzerJobBuilder<?>) ajb;
					if (rpajb.getRequirement() == null) {
						Table foundTable = getOriginatingTable(rpajb);
						if (requiredTable == null || requiredTable.equals(foundTable)) {
							result.add(rpajb);
						}
					}
				}
			}
		}
		return result;
	}

	public List<SourceColumnChangeListener> getSourceColumnListeners() {
		return _sourceColumnListeners;
	}

	public List<AnalyzerChangeListener> getAnalyzerChangeListeners() {
		return _analyzerChangeListeners;
	}

	public List<TransformerChangeListener> getTransformerChangeListeners() {
		return _transformerChangeListeners;
	}

	public List<FilterChangeListener> getFilterChangeListeners() {
		return _filterChangeListeners;
	}

}