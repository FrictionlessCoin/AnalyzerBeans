package org.eobjects.analyzer.job.builder;

import java.util.LinkedList;
import java.util.List;

import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.data.DataTypeFamily;
import org.eobjects.analyzer.data.MutableInputColumn;
import org.eobjects.analyzer.data.TransformedInputColumn;
import org.eobjects.analyzer.descriptors.TransformerBeanDescriptor;
import org.eobjects.analyzer.job.IdGenerator;
import org.eobjects.analyzer.job.ImmutableBeanConfiguration;
import org.eobjects.analyzer.job.ImmutableTransformerJob;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.lifecycle.AssignConfiguredCallback;
import org.eobjects.analyzer.lifecycle.AssignProvidedCallback;
import org.eobjects.analyzer.lifecycle.InMemoryCollectionProvider;
import org.eobjects.analyzer.lifecycle.InitializeCallback;
import org.eobjects.analyzer.lifecycle.LifeCycleCallback;
import org.eobjects.analyzer.lifecycle.LifeCycleState;
import org.eobjects.analyzer.lifecycle.TransformerBeanInstance;

/**
 * @author Kasper Sørensen
 * 
 * @param <T>
 *            the transformer type being configured
 */
public final class TransformerJobBuilder<T extends Transformer<?>> extends
		AbstractBeanWithInputColumnsBuilder<TransformerBeanDescriptor<T>, T, TransformerJobBuilder<T>> {

	private final LinkedList<MutableInputColumn<?>> _outputColumns = new LinkedList<MutableInputColumn<?>>();
	private final IdGenerator _idGenerator;
	private final List<TransformerChangeListener> _transformerChangeListeners;

	public TransformerJobBuilder(TransformerBeanDescriptor<T> descriptor, IdGenerator idGenerator,
			List<TransformerChangeListener> transformerChangeListeners) {
		super(descriptor, TransformerJobBuilder.class);
		_idGenerator = idGenerator;
		_transformerChangeListeners = transformerChangeListeners;
	}

	public List<MutableInputColumn<?>> getOutputColumns() {
		TransformerBeanInstance transformerBeanInstance = new TransformerBeanInstance(getDescriptor());

		// mimic the configuration of a real transformer bean instance
		LifeCycleCallback callback = new AssignConfiguredCallback(new ImmutableBeanConfiguration(getConfiguredProperties()));
		callback.onEvent(LifeCycleState.ASSIGN_CONFIGURED, transformerBeanInstance.getBean(), getDescriptor());

		callback = new AssignProvidedCallback(transformerBeanInstance, new InMemoryCollectionProvider(), null);
		callback.onEvent(LifeCycleState.ASSIGN_PROVIDED, transformerBeanInstance.getBean(), getDescriptor());

		callback = new InitializeCallback();
		callback.onEvent(LifeCycleState.INITIALIZE, transformerBeanInstance.getBean(), getDescriptor());

		OutputColumns outputColumns = transformerBeanInstance.getBean().getOutputColumns();
		if (outputColumns == null) {
			throw new IllegalStateException("getOutputColumns() returned null on transformer: "
					+ transformerBeanInstance.getBean());
		}
		int expectedCols = outputColumns.getColumnCount();
		int existingCols = _outputColumns.size();
		if (expectedCols != existingCols) {
			int colDiff = expectedCols - existingCols;
			if (colDiff > 0) {
				for (int i = 0; i < colDiff; i++) {
					int nextIndex = _outputColumns.size();
					String name = outputColumns.getColumnName(nextIndex);
					if (name == null) {
						name = getDescriptor().getDisplayName();
					}
					DataTypeFamily type = getDescriptor().getOutputDataTypeFamily();
					_outputColumns.add(new TransformedInputColumn<Object>(name, type, _idGenerator));
				}
			} else if (colDiff < 0) {
				for (int i = 0; i < Math.abs(colDiff); i++) {
					// remove from the tail
					_outputColumns.removeLast();
				}
			}

			// notify listeners
			for (TransformerChangeListener listener : _transformerChangeListeners) {
				listener.onOutputChanged(this, _outputColumns);
			}
		}

		return _outputColumns;
	}

	public TransformerJob toTransformerJob() throws IllegalStateException {
		if (!isConfigured()) {
			throw new IllegalStateException("Transformer job is not correctly configured");
		}

		return new ImmutableTransformerJob(getDescriptor(), new ImmutableBeanConfiguration(getConfiguredProperties()),
				getOutputColumns(), getRequirement());
	}

	@Override
	public String toString() {
		return "TransformerJobBuilder[transformer=" + getDescriptor().getDisplayName() + ",inputColumns="
				+ getInputColumns() + "]";
	}

	public MutableInputColumn<?> getOutputColumnByName(String name) {
		if (name != null) {
			List<MutableInputColumn<?>> outputColumns = getOutputColumns();
			for (MutableInputColumn<?> inputColumn : outputColumns) {
				if (name.equals(inputColumn.getName())) {
					return inputColumn;
				}
			}
		}
		return null;
	}
	
	@Override
	protected void onConfigurationChanged() {
		// trigger getOutputColumns which will notify consumers in the case of output changes
		getOutputColumns();
	}
}
