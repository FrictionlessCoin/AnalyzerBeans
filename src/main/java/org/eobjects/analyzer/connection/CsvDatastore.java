package org.eobjects.analyzer.connection;

import java.io.File;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.DataContextFactory;

public final class CsvDatastore implements Datastore {

	private static final long serialVersionUID = 1L;

	private final String _name;
	private final String _filename;
	private final Character _quoteChar;
	private final Character _separatorChar;

	public CsvDatastore(String name, String filename) {
		this(name, filename, null, null);
	}

	public CsvDatastore(String name, String filename, Character quoteChar, Character separatorChar) {
		_name = name;
		_filename = filename;
		_quoteChar = quoteChar;
		_separatorChar = separatorChar;
	}

	@Override
	public void close() {
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public DataContextProvider getDataContextProvider() {
		DataContext dataContext;
		if (_quoteChar == null && _separatorChar == null) {
			dataContext = DataContextFactory.createCsvDataContext(new File(_filename));
		} else {
			char separatorChar = _separatorChar == null ? DataContextFactory.DEFAULT_CSV_SEPARATOR_CHAR : _separatorChar;
			char quoteChar = _quoteChar == null ? DataContextFactory.DEFAULT_CSV_QUOTE_CHAR : _quoteChar;
			dataContext = DataContextFactory.createCsvDataContext(new File(_filename), separatorChar, quoteChar, false);
		}
		return new SingleDataContextProvider(dataContext, this);
	}

}
