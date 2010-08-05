package org.eobjects.analyzer.descriptors;

import org.eobjects.analyzer.annotations.AnalyzerBean;
import org.eobjects.analyzer.beans.ExploringAnalyzer;
import org.eobjects.analyzer.beans.RowProcessingAnalyzer;
import org.eobjects.analyzer.util.ReflectionUtils;

public final class AnnotationBasedAnalyzerBeanDescriptor extends
		AbstractBeanDescriptor implements AnalyzerBeanDescriptor {

	private final String _displayName;
	private final boolean _exploringAnalyzer;
	private final boolean _rowProcessingAnalyzer;

	public AnnotationBasedAnalyzerBeanDescriptor(Class<?> analyzerClass)
			throws DescriptorException {
		super(analyzerClass, ReflectionUtils.is(analyzerClass,
				RowProcessingAnalyzer.class));

		_rowProcessingAnalyzer = ReflectionUtils.is(analyzerClass,
				RowProcessingAnalyzer.class);
		_exploringAnalyzer = ReflectionUtils.is(analyzerClass,
				ExploringAnalyzer.class);

		if (!_rowProcessingAnalyzer && !_exploringAnalyzer) {
			throw new DescriptorException(analyzerClass
					+ " does not implement either "
					+ RowProcessingAnalyzer.class.getName() + " or "
					+ ExploringAnalyzer.class.getName());
		}

		AnalyzerBean analyzerAnnotation = analyzerClass
				.getAnnotation(AnalyzerBean.class);
		if (analyzerAnnotation == null) {
			throw new DescriptorException(analyzerClass
					+ " doesn't implement the AnalyzerBean annotation");
		}

		String displayName = analyzerAnnotation.value();
		if (displayName == null || displayName.trim().length() == 0) {
			displayName = ReflectionUtils.explodeCamelCase(
					analyzerClass.getSimpleName(), false);
		}
		_displayName = displayName;
	}

	@Override
	public String getDisplayName() {
		return _displayName;
	}

	@Override
	public boolean isExploringAnalyzer() {
		return _exploringAnalyzer;
	}

	@Override
	public boolean isRowProcessingAnalyzer() {
		return _rowProcessingAnalyzer;
	}

	@Override
	public ConfiguredPropertyDescriptor getConfiguredPropertyForInput() {
		if (isRowProcessingAnalyzer()) {
			return super.getConfiguredPropertyForInput();
		}
		return null;
	}
}
