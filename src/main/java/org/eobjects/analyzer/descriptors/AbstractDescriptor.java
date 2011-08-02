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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eobjects.analyzer.util.ReflectionUtils;

/**
 * Abstract descriptor implementation which contains only the bare bones of a
 * class field- and method-inspecting descriptor.
 * 
 * @author Kasper Sørensen
 * 
 * @param <B>
 */
abstract class AbstractDescriptor<B> {

	private final Class<B> _componentClass;

	public AbstractDescriptor(Class<B> componentClass) {
		if (componentClass == null) {
			throw new IllegalArgumentException("beanClass cannot be null");
		}
		if (componentClass.isInterface() || Modifier.isAbstract(componentClass.getModifiers())) {
			throw new DescriptorException("Bean (" + componentClass + ") is not a non-abstract class");
		}

		_componentClass = componentClass;
	}

	protected void visitClass() {
		Field[] fields = ReflectionUtils.getFields(_componentClass);
		for (Field field : fields) {
			visitField(field);
		}

		Method[] methods = ReflectionUtils.getMethods(_componentClass);
		for (Method method : methods) {
			visitMethod(method);
		}
	}

	protected abstract void visitField(Field field);

	protected abstract void visitMethod(Method method);

	public Class<B> getComponentClass() {
		return _componentClass;
	}

	@Override
	public int hashCode() {
		return _componentClass.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj.getClass() == this.getClass()) {
			AbstractDescriptor<?> that = (AbstractDescriptor<?>) obj;
			return this._componentClass == that._componentClass;
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + _componentClass.getName() + "]";
	}
}
