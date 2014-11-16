/**
 * AnalyzerBeans
 * Copyright (C) 2014 Neopost - Customer Information Management
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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eobjects.analyzer.beans.api.Alias;

/**
 * Abstract implementation of the {@link BeanDescriptor} interface. Convenient
 * for implementing it's subclasses.
 * 
 * 
 * 
 * @param <B>
 */
abstract class AbstractBeanDescriptor<B> extends SimpleComponentDescriptor<B> implements BeanDescriptor<B> {

	private static final long serialVersionUID = 1L;
	
	private final boolean _requireInputColumns;

	public AbstractBeanDescriptor(Class<B> beanClass, boolean requireInputColumns) {
		super(beanClass);
		_requireInputColumns = requireInputColumns;
	}

	@Override
	protected void visitClass() {
		super.visitClass();

		if (_requireInputColumns) {
			int numConfiguredColumns = 0;
			int numConfiguredColumnArrays = 0;
			for (ConfiguredPropertyDescriptor cd : _configuredProperties) {
				if (cd.isInputColumn()) {
					if (cd.isArray()) {
						numConfiguredColumnArrays++;
					} else {
						numConfiguredColumns++;
					}
				}
			}
			final int totalColumns = numConfiguredColumns + numConfiguredColumnArrays;
			if (totalColumns == 0) {
				throw new DescriptorException(getComponentClass()
						+ " does not define a @Configured InputColumn or InputColumn-array");
			}
		}
	}

	@Override
	public Set<ConfiguredPropertyDescriptor> getConfiguredPropertiesForInput() {
		return getConfiguredPropertiesForInput(true);
	}

	@Override
	public Set<ConfiguredPropertyDescriptor> getConfiguredPropertiesForInput(boolean includeOptional) {
		Set<ConfiguredPropertyDescriptor> descriptors = new TreeSet<ConfiguredPropertyDescriptor>(_configuredProperties);
		for (Iterator<ConfiguredPropertyDescriptor> it = descriptors.iterator(); it.hasNext();) {
			ConfiguredPropertyDescriptor propertyDescriptor = it.next();
			if (!propertyDescriptor.isInputColumn()) {
				it.remove();
			} else if (!includeOptional && !propertyDescriptor.isRequired()) {
				it.remove();
			}
		}
		return descriptors;
	}

	@Override
	public String[] getAliases() {
		Alias alias = getAnnotation(Alias.class);
		if (alias == null) {
			return new String[0];
		}
		return alias.value();
	}
}
