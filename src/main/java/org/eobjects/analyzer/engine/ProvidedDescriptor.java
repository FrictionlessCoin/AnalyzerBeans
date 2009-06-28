package org.eobjects.analyzer.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang.ArrayUtils;
import org.eobjects.analyzer.annotations.Provided;

public class ProvidedDescriptor {

	private Field _field;
	private Method _method;
	private Class<?> _baseType;
	private Type[] _typeArguments;

	public ProvidedDescriptor(Field field, Provided providedAnnotation) throws IllegalArgumentException {
		_field = field;
		setType(_field.getType());
		setGenericType(_field.getGenericType());
	}

	public ProvidedDescriptor(Method method, Provided providedAnnotation) throws IllegalArgumentException {
		_method = method;
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new IllegalArgumentException("The @Provided annotated method " + method + " defines "
					+ parameterTypes.length + " parameters, a single parameter is required");
		}
		setType(parameterTypes[0]);
		setGenericType(_method.getGenericParameterTypes()[0]);
	}

	private void setType(Class<?> type) throws IllegalArgumentException {
		if (!(AnnotationHelper.isMap(type) || AnnotationHelper.isList(type))) {
			throw new IllegalArgumentException("The type " + _baseType
					+ " is not supported by the @Provided annotation");
		}
		_baseType = type;
	}

	private void setGenericType(Type genericType) {
		if (genericType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericType;
			_typeArguments = parameterizedType.getActualTypeArguments();
			for (Type type : _typeArguments) {
				if (!(AnnotationHelper.isString(type) || AnnotationHelper.isBoolean(type)
						|| AnnotationHelper.isDouble(type) || AnnotationHelper.isInteger(type) || AnnotationHelper
						.isLong(type))) {
					throw new IllegalArgumentException("The type " + _baseType
							+ " is not supported by the @Provided annotation");
				}
			}
		}
	}

	@Override
	public String toString() {
		if (_field == null) {
			return "ProvidedDescriptor[method=" + _method.getName() + ",baseType=" + _baseType + ",typeParameters="
					+ ArrayUtils.toString(_typeArguments) + "]";
		} else {
			return "ProvidedDescriptor[field=" + _field.getName() + ",baseType=" + _baseType + ",typeParameters="
					+ ArrayUtils.toString(_typeArguments) + "]";
		}
	}

	/**
	 * TODO: This is not called yet, provider-framework not impmlemented
	 */
	public void assignValue(Object analyzerBean, Object value) throws IllegalArgumentException {
		try {
			if (_method != null) {
				_method.invoke(analyzerBean, value);
			} else {
				_field.set(analyzerBean, value);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not assign value '" + value + "' to "
					+ (_method == null ? _field : _method), e);
		}
	}
}
