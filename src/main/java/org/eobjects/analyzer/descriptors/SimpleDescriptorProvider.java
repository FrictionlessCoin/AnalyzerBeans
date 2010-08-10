package org.eobjects.analyzer.descriptors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eobjects.analyzer.beans.Analyzer;
import org.eobjects.analyzer.beans.Transformer;
import org.eobjects.analyzer.result.renderer.Renderer;

/**
 * A simple descriptor provider with a method signature suitable externalizing
 * class names of analyzer and transformer beans. For example, if you're using
 * the Spring Framework you initialize this descriptor provider as follows:
 * 
 * <pre>
 * &lt;bean id="descriptorProvider" class="org.eobjects.analyzer.descriptors.SimpleDescriptorProvider"&gt;
 *   &lt;property name="analyzerClassNames"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;org.eobjects.analyzer.beans.StringAnalyzer&lt;/value&gt;
 *       &lt;value&gt;org.eobjects.analyzer.beans.valuedist.ValueDistributionAnalyzer&lt;/value&gt;
 *       ...
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   &lt;property name="transformerClassNames"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;org.eobjects.analyzer.beans.TokenizerTransformer&lt;/value&gt;
 *       ...
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   &lt;property name="rendererClassNames"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;org.eobjects.analyzer.result.renderer.DefaultTextRenderer&lt;/value&gt;
 *       ...
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * @author Kasper Sørensen
 */
public class SimpleDescriptorProvider implements DescriptorProvider {

	private List<AnalyzerBeanDescriptor> _analyzerBeanDescriptors = new ArrayList<AnalyzerBeanDescriptor>();
	private List<TransformerBeanDescriptor> _transformerBeanDescriptors = new ArrayList<TransformerBeanDescriptor>();
	private List<RendererBeanDescriptor> _rendererBeanDescriptors = new ArrayList<RendererBeanDescriptor>();

	public SimpleDescriptorProvider() {
	}

	public void addAnalyzerBeanDescriptor(
			AnalyzerBeanDescriptor analyzerBeanDescriptor) {
		_analyzerBeanDescriptors.add(analyzerBeanDescriptor);
	}

	public void addTransformerBeanDescriptor(
			TransformerBeanDescriptor transformerBeanDescriptor) {
		_transformerBeanDescriptors.add(transformerBeanDescriptor);
	}

	public void addRendererBeanDescriptor(
			RendererBeanDescriptor rendererBeanDescriptor) {
		_rendererBeanDescriptors.add(rendererBeanDescriptor);
	}

	@Override
	public List<AnalyzerBeanDescriptor> getAnalyzerBeanDescriptors() {
		return _analyzerBeanDescriptors;
	}

	public void setAnalyzerBeanDescriptors(
			List<AnalyzerBeanDescriptor> descriptors) {
		_analyzerBeanDescriptors = descriptors;
	}

	@Override
	public AnalyzerBeanDescriptor getAnalyzerBeanDescriptorForClass(
			Class<? extends Analyzer<?>> analyzerBeanClass) {
		for (AnalyzerBeanDescriptor descriptor : _analyzerBeanDescriptors) {
			if (descriptor.getBeanClass() == analyzerBeanClass) {
				return descriptor;
			}
		}
		return null;
	}

	@Override
	public TransformerBeanDescriptor getTransformerBeanDescriptorForClass(
			Class<? extends Transformer<?>> transformerBeanClass) {
		for (TransformerBeanDescriptor descriptor : _transformerBeanDescriptors) {
			if (descriptor.getBeanClass() == transformerBeanClass) {
				return descriptor;
			}
		}
		return null;
	}

	@Override
	public List<TransformerBeanDescriptor> getTransformerBeanDescriptors() {
		return _transformerBeanDescriptors;
	}

	public void setTransformerBeanDescriptors(
			List<TransformerBeanDescriptor> transformerBeanDescriptors) {
		_transformerBeanDescriptors = transformerBeanDescriptors;
	}

	@Override
	public List<RendererBeanDescriptor> getRendererBeanDescriptors() {
		return _rendererBeanDescriptors;
	}

	public void setRendererBeanDescriptors(
			List<RendererBeanDescriptor> rendererBeanDescriptors) {
		_rendererBeanDescriptors = rendererBeanDescriptors;
	}

	@Override
	public RendererBeanDescriptor getRendererBeanDescriptorForClass(
			Class<? extends Renderer<?, ?>> rendererBeanClass) {
		for (RendererBeanDescriptor descriptor : _rendererBeanDescriptors) {
			if (descriptor.getBeanClass() == rendererBeanClass) {
				return descriptor;
			}
		}
		return null;
	}

	public void setAnalyzerClassNames(Collection<String> classNames)
			throws ClassNotFoundException {
		for (String className : classNames) {
			@SuppressWarnings("unchecked")
			Class<? extends Analyzer<?>> c = (Class<? extends Analyzer<?>>) Class
					.forName(className);
			AnalyzerBeanDescriptor descriptor = getAnalyzerBeanDescriptorForClass(c);
			if (descriptor == null) {
				addAnalyzerBeanDescriptor(new AnnotationBasedAnalyzerBeanDescriptor(
						c));
			}
		}
	}

	public void setTransformerClassNames(Collection<String> classNames)
			throws ClassNotFoundException {
		for (String className : classNames) {
			@SuppressWarnings("unchecked")
			Class<? extends Transformer<?>> c = (Class<? extends Transformer<?>>) Class
					.forName(className);
			TransformerBeanDescriptor descriptor = getTransformerBeanDescriptorForClass(c);
			if (descriptor == null) {
				addTransformerBeanDescriptor(new AnnotationBasedTransformerBeanDescriptor(
						c));
			}
		}
	}

	public void setRendererClassNames(Collection<String> classNames)
			throws ClassNotFoundException {
		for (String className : classNames) {
			@SuppressWarnings("unchecked")
			Class<? extends Renderer<?, ?>> c = (Class<? extends Renderer<?, ?>>) Class
					.forName(className);
			RendererBeanDescriptor descriptor = getRendererBeanDescriptorForClass(c);
			if (descriptor == null) {
				addRendererBeanDescriptor(new AnnotationBasedRendererBeanDescriptor(
						c));
			}
		}
	}
}
