package org.eobjects.analyzer.descriptors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eobjects.analyzer.beans.Analyzer;
import org.eobjects.analyzer.beans.Transformer;
import org.eobjects.analyzer.result.renderer.Renderer;

public class LazyDescriptorProvider implements DescriptorProvider {

	private Map<Class<? extends Analyzer<?>>, AnalyzerBeanDescriptor<?>> _analyzerBeanDescriptors = new HashMap<Class<? extends Analyzer<?>>, AnalyzerBeanDescriptor<?>>();
	private Map<Class<? extends Transformer<?>>, TransformerBeanDescriptor<?>> _transformerBeanDescriptors = new HashMap<Class<? extends Transformer<?>>, TransformerBeanDescriptor<?>>();
	private Map<Class<? extends Renderer<?, ?>>, RendererBeanDescriptor> _rendererBeanDescriptors = new HashMap<Class<? extends Renderer<?, ?>>, RendererBeanDescriptor>();

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Analyzer<?>> AnalyzerBeanDescriptor<A> getAnalyzerBeanDescriptorForClass(
			Class<A> analyzerBeanClass) {
		AnalyzerBeanDescriptor<?> descriptor = _analyzerBeanDescriptors
				.get(analyzerBeanClass);
		if (descriptor == null) {
			descriptor = AnnotationBasedAnalyzerBeanDescriptor.create(
					analyzerBeanClass);
			_analyzerBeanDescriptors.put(analyzerBeanClass, descriptor);
		}
		return (AnalyzerBeanDescriptor<A>) descriptor;
	}

	@Override
	public Collection<AnalyzerBeanDescriptor<?>> getAnalyzerBeanDescriptors() {
		return Collections.unmodifiableCollection(_analyzerBeanDescriptors
				.values());
	}

	@Override
	public Collection<TransformerBeanDescriptor<?>> getTransformerBeanDescriptors() {
		return Collections.unmodifiableCollection(_transformerBeanDescriptors
				.values());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Transformer<?>> TransformerBeanDescriptor<T> getTransformerBeanDescriptorForClass(
			Class<T> transformerBeanClass) {
		TransformerBeanDescriptor<?> descriptor = _transformerBeanDescriptors
				.get(transformerBeanClass);
		if (descriptor == null) {
			descriptor = AnnotationBasedTransformerBeanDescriptor.create(
					transformerBeanClass);
			_transformerBeanDescriptors.put(transformerBeanClass, descriptor);
		}
		return (TransformerBeanDescriptor<T>) descriptor;
	}

	@Override
	public RendererBeanDescriptor getRendererBeanDescriptorForClass(
			Class<? extends Renderer<?, ?>> rendererBeanClass) {
		return _rendererBeanDescriptors.get(rendererBeanClass);
	}

	@Override
	public Collection<RendererBeanDescriptor> getRendererBeanDescriptors() {
		return Collections.unmodifiableCollection(_rendererBeanDescriptors
				.values());
	}
}
