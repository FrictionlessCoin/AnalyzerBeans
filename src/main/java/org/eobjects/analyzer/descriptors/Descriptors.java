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

import org.eobjects.analyzer.beans.api.Analyzer;
import org.eobjects.analyzer.beans.api.Filter;
import org.eobjects.analyzer.beans.api.Renderer;
import org.eobjects.analyzer.beans.api.Transformer;

/**
 * Contains static factory and utility methods for descriptors within this
 * package.
 * 
 * @author Kasper Sørensen
 */
public class Descriptors {

	private Descriptors() {
		// prevent instantiation
	}

	/**
	 * Creates a {@link ComponentDescriptor} for any class.
	 * 
	 * @param <C>
	 * @param componentClass
	 * @return
	 */
	public static <C> ComponentDescriptor<C> ofComponent(Class<C> componentClass) {
		return new SimpleComponentDescriptor<C>(componentClass, true);
	}

	/**
	 * Creates an {@link AnalyzerBeanDescriptor} for an analyzer class.
	 * 
	 * @param <A>
	 * @param analyzerClass
	 * @return
	 */
	public static <A extends Analyzer<?>> AnalyzerBeanDescriptor<A> ofAnalyzer(Class<A> analyzerClass) {
		return new AnnotationBasedAnalyzerBeanDescriptor<A>(analyzerClass);
	}

	/**
	 * Creates a {@link FilterBeanDescriptor} for a filter class.
	 * 
	 * @param <F>
	 * @param <C>
	 * @param filterClass
	 * @return
	 */
	public static <F extends Filter<C>, C extends Enum<C>> FilterBeanDescriptor<F, C> ofFilter(Class<F> filterClass) {
		return new AnnotationBasedFilterBeanDescriptor<F, C>(filterClass);
	}

	/**
	 * Creates a {@link FilterBeanDescriptor} for a filter class.
	 * 
	 * Alternative factory method used when sufficient type-information about
	 * the {@link Filter} class is not available.
	 * 
	 * This method is basically a hack to make the compiler happy, see Ticket
	 * #417.
	 * 
	 * @see http://eobjects.org/trac/ticket/417
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static FilterBeanDescriptor<?, ?> ofFilterUnbound(Class<?> clazz) {
		return new AnnotationBasedFilterBeanDescriptor(clazz);
	}

	/**
	 * Creates a {@link TransformerBeanDescriptor} for a transformer class.
	 * 
	 * @param <T>
	 * @param transformerClass
	 * @return
	 */
	public static <T extends Transformer<?>> TransformerBeanDescriptor<T> ofTransformer(Class<T> transformerClass) {
		return new AnnotationBasedTransformerBeanDescriptor<T>(transformerClass);
	}

	/**
	 * Creates a {@link RendererBeanDescriptor} for a renderer class.
	 * 
	 * @param <R>
	 * @param rendererClass
	 * @return
	 */
	public static <R extends Renderer<?, ?>> RendererBeanDescriptor ofRenderer(Class<R> rendererClass) {
		return new AnnotationBasedRendererBeanDescriptor(rendererClass);
	}
}
