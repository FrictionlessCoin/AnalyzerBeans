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

import java.io.File;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.DataContextFactory;

/**
 * Datastore implementation for OpenOffice database files (.odb).
 * 
 * @author Kasper Sørensen
 */
public class OdbDatastore implements Datastore {

	private static final long serialVersionUID = 1L;

	private final String _name;
	private final String _filename;
	private transient DataContextProvider _dataContextProvider;

	public OdbDatastore(String name, String filename) {
		_name = name;
		_filename = filename;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public DataContextProvider getDataContextProvider() {
		if (_dataContextProvider == null) {
			DataContext dc = DataContextFactory.createOpenOfficeDataContext(new File(_filename));
			_dataContextProvider = new SingleDataContextProvider(dc, this);
		}
		return _dataContextProvider;
	}

	@Override
	public void close() {
		_dataContextProvider = null;
	}

}
