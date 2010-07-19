package org.eobjects.analyzer.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eobjects.analyzer.descriptors.AbstractBeanDescriptor;
import org.eobjects.analyzer.descriptors.AnnotationHelper;
import org.eobjects.analyzer.descriptors.ConfiguredDescriptor;

@SuppressWarnings("unchecked")
class AbstractBeanJobBuilder<D extends AbstractBeanDescriptor, B> {

	private Map<ConfiguredDescriptor, Object> _properties = new HashMap<ConfiguredDescriptor, Object>();
	private D _descriptor;

	public AbstractBeanJobBuilder(D descriptor, Class<B> builderClass) {
		_descriptor = descriptor;
		if (!AnnotationHelper.is(getClass(), builderClass)) {
			throw new IllegalArgumentException(
					"Builder class does not correspond to actual class of builder");
		}
	}

	public D getDescriptor() {
		return _descriptor;
	}

	public boolean isConfigured() {
		for (ConfiguredDescriptor configuredDescriptor : _descriptor
				.getConfiguredDescriptors()) {
			if (!getConfiguredProperties().containsKey(configuredDescriptor)) {
				return false;
			}
		}
		return true;
	}

	public B setConfiguredProperty(String configuredName, Object value) {
		ConfiguredDescriptor configuredDescriptor = _descriptor
				.getConfiguredDescriptor(configuredName);
		if (configuredDescriptor == null) {
			throw new IllegalArgumentException("No such configured property: "
					+ configuredName);
		}
		return setConfiguredProperty(configuredDescriptor, value);
	}

	public B setConfiguredProperty(ConfiguredDescriptor configuredDescriptor,
			Object value) {
		if (configuredDescriptor == null) {
			throw new IllegalArgumentException(
					"configuredDescriptor cannot be null");
		}
		if (value != null) {
			if (!AnnotationHelper.is(value.getClass(),
					configuredDescriptor.getBaseType())) {
				throw new IllegalArgumentException("Invalid value type: "
						+ value.getClass().getName() + ", expected: "
						+ configuredDescriptor.getBaseType().getName());
			}
		}
		_properties.put(configuredDescriptor, value);
		return (B) this;
	}

	public Map<ConfiguredDescriptor, Object> getConfiguredProperties() {
		return Collections.unmodifiableMap(_properties);
	}
}
