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

import org.eobjects.metamodel.DataContext;
import org.eobjects.metamodel.DataContextFactory;

/**
 * Datastore implementation for XML files.
 * 
 * @author Kasper Sørensen
 */
public class XmlDatastore extends UsageAwareDatastore implements FileDatastore {

	private static final long serialVersionUID = 1L;

	private final String _filename;

	public XmlDatastore(String name, String filename) {
		super(name);
		_filename = filename;
	}

	@Override
	public String getFilename() {
		return _filename;
	}

	@Override
	protected UsageAwareDataContextProvider createDataContextProvider() {
		final File file = new File(_filename);
		final DataContext dataContext = DataContextFactory.createXmlDataContext(file, true, false);
		return new SingleDataContextProvider(dataContext, this);
	}

	@Override
	public PerformanceCharacteristics getPerformanceCharacteristics() {
		return new PerformanceCharacteristicsImpl(false);
	}
}
