/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.descriptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

import org.eobjects.analyzer.util.CollectionUtils;
import org.eobjects.analyzer.util.ReflectionUtils;
import org.eobjects.analyzer.util.SchemaNavigator;

public class AbstractPropertyDescriptor implements PropertyDescriptor {

	private final Field _field;
	private final Class<?> _baseType;
	private final Type _genericType;
	private final BeanDescriptor<?> _beanDescriptor;

	public AbstractPropertyDescriptor(Field field, BeanDescriptor<?> beanDescriptor) {
		if (field == null) {
			throw new IllegalArgumentException("field cannot be null");
		}
		_field = field;
		_field.setAccessible(true);
		_baseType = _field.getType();
		_genericType = _field.getGenericType();
		_beanDescriptor = beanDescriptor;
		init();
	}

	private void init() {
		if (!(ReflectionUtils.isMap(_baseType) || ReflectionUtils.isList(_baseType) || SchemaNavigator.class != _baseType)) {
			throw new DescriptorException("The type " + _baseType + " is not supported by the @Provided annotation");
		}
	}

	@Override
	public String getName() {
		return _field.getName();
	}

	@Override
	public void setValue(Object bean, Object value) throws IllegalArgumentException {
		try {
			_field.set(bean, value);
		} catch (Exception e) {
			throw new IllegalStateException("Could not assign value '" + value + "' to " + _field, e);
		}
	}

	@Override
	public Object getValue(Object bean) throws IllegalArgumentException {
		if (bean == null) {
			throw new IllegalArgumentException("bean cannot be null");
		}
		try {
			return _field.get(bean);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not retrieve property '" + getName() + "' from bean: " + bean);
		}
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return CollectionUtils.set(_field.getAnnotations());
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return _field.getAnnotation(annotationClass);
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
	public Class<?> getType() {
		return _baseType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_field == null) ? 0 : _field.hashCode());
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
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[field=" + _field.getName() + ",baseType=" + _baseType + "]";
	}

	@Override
	public int compareTo(PropertyDescriptor o) {
		if (o == null) {
			return 1;
		}
		if (o == this) {
			return 0;
		}
		int result = getName().compareTo(o.getName());
		if (result == 0) {
			result = getBeanDescriptor().compareTo(o.getBeanDescriptor());
		}
		if (result == 0) {
			result = getType().hashCode() - o.getType().hashCode();
		}
		return result;
	}

	@Override
	public BeanDescriptor<?> getBeanDescriptor() {
		return _beanDescriptor;
	}
}
