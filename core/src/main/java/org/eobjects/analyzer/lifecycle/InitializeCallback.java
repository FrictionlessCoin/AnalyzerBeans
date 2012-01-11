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
package org.eobjects.analyzer.lifecycle;

import java.util.Set;

import org.eobjects.analyzer.descriptors.ComponentDescriptor;
import org.eobjects.analyzer.descriptors.InitializeMethodDescriptor;
import org.eobjects.analyzer.descriptors.ValidateMethodDescriptor;

/**
 * Life cycle callback for the initialize phase.
 * 
 * @author Kasper Sørensen
 */
final class InitializeCallback implements LifeCycleCallback<Object, ComponentDescriptor<?>> {
	
	private final boolean _validate;
	private final boolean _initialize;

	public InitializeCallback(boolean validate, boolean initialize) {
		_validate = validate;
		_initialize = initialize;
	}

	@Override
	public void onEvent(Object component, ComponentDescriptor<?> descriptor) {
		if (_validate) {
			Set<ValidateMethodDescriptor> validateDescriptors = descriptor.getValidateMethods();
			for (ValidateMethodDescriptor validateDescriptor : validateDescriptors) {
				validateDescriptor.validate(component);
			}
		}

		if (_initialize) {
			Set<InitializeMethodDescriptor> initializeDescriptors = descriptor.getInitializeMethods();
			for (InitializeMethodDescriptor initializeDescriptor : initializeDescriptors) {
				initializeDescriptor.initialize(component);
			}
		}
	}

}
