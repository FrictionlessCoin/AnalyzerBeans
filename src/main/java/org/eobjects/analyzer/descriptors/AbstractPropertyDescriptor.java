package org.eobjects.analyzer.descriptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import org.eobjects.analyzer.util.CollectionUtils;
import org.eobjects.analyzer.util.ReflectionUtils;
import org.eobjects.analyzer.util.SchemaNavigator;

public class AbstractPropertyDescriptor implements PropertyDescriptor {

	private Method _method;
	private Field _field;
	private Class<?> _baseType;
	private Type _genericType;

	public AbstractPropertyDescriptor(Method setterMethod) {
		_method = setterMethod;
		_method.setAccessible(true);
		Class<?>[] parameterTypes = setterMethod.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new DescriptorException("The method " + setterMethod
					+ " defines " + parameterTypes.length
					+ " parameters, a single parameter is required");
		}
		setType(parameterTypes[0]);
		setGenericType(_method.getGenericParameterTypes()[0]);
	}

	public AbstractPropertyDescriptor(Field field) {
		_field = field;
		_field.setAccessible(true);
		setType(_field.getType());
		setGenericType(_field.getGenericType());
	}

	private void setType(Class<?> type) throws DescriptorException {
		if (!(ReflectionUtils.isMap(type) || ReflectionUtils.isList(type) || SchemaNavigator.class != type)) {
			throw new DescriptorException("The type " + _baseType
					+ " is not supported by the @Provided annotation");
		}
		_baseType = type;
	}

	private void setGenericType(Type genericType) throws DescriptorException {
		_genericType = genericType;
	}

	@Override
	public String getName() {
		return (_method == null ? _field.getName() : _method.getName());
	}

	@Override
	public void assignValue(Object bean, Object value)
			throws IllegalArgumentException {
		try {
			if (_method != null) {
				_method.invoke(bean, value);
			} else {
				_field.set(bean, value);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Could not assign value '" + value
					+ "' to " + (_method == null ? _field : _method), e);
		}
	}

	@Override
	public Set<Annotation> getAnnotations() {
		if (_field == null) {
			return CollectionUtils.set(_method.getAnnotations());
		} else {
			return CollectionUtils.set(_field.getAnnotations());
		}
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		if (_field == null) {
			return _method.getAnnotation(annotationClass);
		} else {
			return _field.getAnnotation(annotationClass);
		}
	}

	@Override
	public int getTypeArgumentCount() {
		return ReflectionUtils.getTypeParameterCount(_genericType);
	}

	@Override
	public Type getTypeArgument(int i) throws IndexOutOfBoundsException {
		return ReflectionUtils.getTypeParameter(_genericType, i);
	}

	@Override
	public Class<?> getBaseType() {
		if (_baseType.isArray()) {
			return _baseType.getComponentType();
		}
		return _baseType;
	}
	
	@Override
	public boolean isArray() {
		return _baseType.isArray();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_field == null) ? 0 : _field.hashCode());
		result = prime * result + ((_method == null) ? 0 : _method.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractPropertyDescriptor other = (AbstractPropertyDescriptor) obj;
		if (_field == null) {
			if (other._field != null)
				return false;
		} else if (!_field.equals(other._field))
			return false;
		if (_method == null) {
			if (other._method != null)
				return false;
		} else if (!_method.equals(other._method))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (_field == null) {
			return getClass().getSimpleName() + "[method=" + _method.getName()
					+ ",baseType=" + _baseType + "]";
		} else {
			return getClass().getSimpleName() + "[field=" + _field.getName()
					+ ",baseType=" + _baseType + "]";
		}
	}
}
