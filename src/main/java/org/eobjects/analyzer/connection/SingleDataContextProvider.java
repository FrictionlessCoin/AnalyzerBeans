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
package org.eobjects.analyzer.connection;

import java.io.Closeable;
import java.io.IOException;

import org.eobjects.analyzer.util.SchemaNavigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.eobjects.metamodel.DataContext;

public final class SingleDataContextProvider extends UsageAwareDataContextProvider {

	private static final Logger logger = LoggerFactory.getLogger(SingleDataContextProvider.class);

	private final DataContext _dataContext;
	private final SchemaNavigator _schemaNavigator;
	private final Datastore _datastore;
	private final Closeable[] _closeables;

	public SingleDataContextProvider(DataContext dataContext, Datastore datastore, Closeable... closeables) {
		_dataContext = dataContext;
		_schemaNavigator = new SchemaNavigator(dataContext);
		_datastore = datastore;
		_closeables = closeables;
	}

	@Override
	public DataContext getDataContext() {
		return _dataContext;
	}

	@Override
	public SchemaNavigator getSchemaNavigator() {
		return _schemaNavigator;
	}

	@Override
	public Datastore getDatastore() {
		return _datastore;
	}

	@Override
	protected void closeInternal() {
		for (int i = 0; i < _closeables.length; i++) {
			Closeable closeable = _closeables[i];
			try {
				closeable.close();
			} catch (IOException e) {
				logger.error("Could not close _closeables[" + i + "]", e);
			}
		}
	}
}
