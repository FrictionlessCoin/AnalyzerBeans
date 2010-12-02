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
package org.eobjects.analyzer.beans.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Methods and fields with the @Configured annotation are used to configure an
 * AnalyzerBean before execution. Typically, the @Configured annotated
 * methods/fields will be used to prompt the user for configuration. Methods
 * annotated with @Configured need to have a single argument, equivalent to a
 * property setter/modifier method.
 * 
 * Valid types for @Configured annotated fields and method arguments are single
 * instances or arrays of:
 * <ul>
 * <li>Boolean</li>
 * <li>Byte</li>
 * <li>Short</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>Character</li>
 * <li>String</li>
 * <li>java.io.File</li>
 * <li>enum types</li>
 * <li>java.util.regex.Pattern</li>
 * <li>org.eobjects.analyzer.data.InputColumn</li>
 * <li>org.eobjects.analyzer.reference.Dictionary</li>
 * <li>org.eobjects.analyzer.reference.SynonymCatalog</li>
 * <li>org.eobjects.analyzer.reference.StringPattern</li>
 * </ul>
 * 
 * Additionally exploring analyzers are allowed to inject these @Configured
 * types (for querying purposes):
 * 
 * <ul>
 * <li>dk.eobjects.metamodel.schema.Column</li>
 * <li>dk.eobjects.metamodel.schema.Table</li>
 * <li>dk.eobjects.metamodel.schema.Schema</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Documented
@Inherited
@Qualifier
public @interface Configured {

	/**
	 * Defines the name of the required configuration property.
	 * 
	 * @return the name of the configuration property
	 */
	String value() default "";

	boolean required() default true;
}
