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
package org.eobjects.analyzer.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eobjects.analyzer.beans.api.OutputRowCollector;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreConnection;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.concurrent.ThreadLocalOutputRowCollector;
import org.eobjects.analyzer.reference.ReferenceDataCatalog;
import org.eobjects.analyzer.result.renderer.RendererFactory;
import org.eobjects.analyzer.storage.CollectionFactory;
import org.eobjects.analyzer.storage.CollectionFactoryImpl;
import org.eobjects.analyzer.storage.RowAnnotation;
import org.eobjects.analyzer.storage.RowAnnotationFactory;
import org.eobjects.analyzer.util.SchemaNavigator;
import org.eobjects.metamodel.DataContext;
import org.eobjects.metamodel.util.LazyRef;
import org.eobjects.metamodel.util.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple injection manager implementation, which is aware of catalogs used
 * within the {@link AnalyzerBeansConfiguration}, but not anymore.
 * 
 * @author Kasper Sørensen
 */
public class InjectionManagerImpl implements InjectionManager {

	private static final Logger logger = LoggerFactory.getLogger(InjectionManagerImpl.class);

	private final AnalyzerBeansConfiguration _configuration;
	private final AnalysisJob _job;
	private final Ref<RowAnnotationFactory> _rowAnntationFactoryRef;

	/**
	 * Constructs an {@link InjectionManager} for use within the scope of a job
	 * execution.
	 * 
	 * @param configuration
	 * @param job
	 */
	public InjectionManagerImpl(AnalyzerBeansConfiguration configuration, AnalysisJob job) {
	    _configuration = configuration;
		_job = job;
		_rowAnntationFactoryRef = createRowAnnotationFactoryRef();
	}

	/**
	 * Creates a new {@link InjectionManager} without any job-context.
	 * Convenient for use outside of an actual job, mimicing a job situation
	 * etc.
	 * 
	 * @param configuration
	 */
	public InjectionManagerImpl(AnalyzerBeansConfiguration configuration) {
		this(configuration, null);
	}

	private Ref<RowAnnotationFactory> createRowAnnotationFactoryRef() {
		return new LazyRef<RowAnnotationFactory>() {
			@Override
			protected RowAnnotationFactory fetch() {
				logger.info("Creating RowAnnotationFactory for job: {}", _job);
				RowAnnotationFactory rowAnnotationFactory = _configuration.getStorageProvider().createRowAnnotationFactory();
				if (rowAnnotationFactory == null) {
					throw new IllegalStateException("Storage provider returned null RowAnnotationFactory!");
				}
				return rowAnnotationFactory;
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getInstance(InjectionPoint<E> injectionPoint) {
		final Class<E> baseType = injectionPoint.getBaseType();
		if (baseType == ReferenceDataCatalog.class) {
			return (E) _configuration.getReferenceDataCatalog();
		} else if (baseType == OutputRowCollector.class) {
			return (E) new ThreadLocalOutputRowCollector();
		} else if (baseType == DatastoreCatalog.class) {
			return (E) _configuration.getDatastoreCatalog();
		} else if (baseType == CollectionFactory.class) {
			return (E) new CollectionFactoryImpl(_configuration.getStorageProvider());
		} else if (baseType == RendererFactory.class) {
		    return (E) new RendererFactory(_configuration);
		} else if (baseType == RowAnnotationFactory.class) {
			return (E) _rowAnntationFactoryRef.get();
		} else if (baseType == AnalyzerBeansConfiguration.class) {
		    return (E) _configuration;
		} else if (baseType == AnalysisJob.class) {
		    return (E) _job;
		} else if (baseType == RowAnnotation.class) {
			return (E) _rowAnntationFactoryRef.get().createAnnotation();
		} else if (baseType == DatastoreConnection.class && _job != null) {
			return (E) _job.getDatastore().openConnection();
		} else if (baseType == DataContext.class && _job != null) {
			return (E) _job.getDatastore().openConnection().getDataContext();
		} else if (baseType == SchemaNavigator.class && _job != null) {
			return (E) _job.getDatastore().openConnection().getSchemaNavigator();
		} else {
			// only inject persistent lists, sets, maps into @Provided fields.
			if (injectionPoint.getAnnotation(Provided.class) != null && injectionPoint.isGenericType()) {
				final Class<?> clazz1 = injectionPoint.getGenericTypeArgument(0);
				if (baseType == List.class) {
					List<?> list = _configuration.getStorageProvider().createList(clazz1);
					return (E) list;
				} else if (baseType == Set.class) {
					Set<?> set = _configuration.getStorageProvider().createSet(clazz1);
					return (E) set;
				} else if (baseType == Map.class) {
					Class<?> clazz2 = (Class<?>) injectionPoint.getGenericTypeArgument(1);
					Map<?, ?> map = _configuration.getStorageProvider().createMap(clazz1, clazz2);
					return (E) map;
				} else {
					logger.warn("Could not handle @Provided injection for injection point: {}", injectionPoint);
				}
			} else {
				logger.warn("Could not handle injection for injection point: {}", injectionPoint);
			}
		}

		// unsupported injection type
		return null;
	}

}
