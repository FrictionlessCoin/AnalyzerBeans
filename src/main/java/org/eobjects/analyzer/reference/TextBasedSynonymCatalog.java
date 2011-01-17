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
package org.eobjects.analyzer.reference;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eobjects.analyzer.util.StringUtils;
import org.eobjects.metamodel.util.FileHelper;
import org.h2.util.SoftHashMap;

public final class TextBasedSynonymCatalog implements SynonymCatalog {

	private static final long serialVersionUID = 1L;

	private transient SoftHashMap<String, String> _masterTermCache;
	private transient File _file;

	private final String _filename;
	private final String _name;
	private final boolean _caseSensitive;
	private final String _encoding;

	public TextBasedSynonymCatalog(String name, String filename, boolean caseSensitive, String encoding) {
		_name = name;
		_filename = filename;
		_caseSensitive = caseSensitive;
		_encoding = encoding;
	}

	public TextBasedSynonymCatalog(String name, File file, boolean caseSensitive, String encoding) {
		_name = name;
		_filename = file.getPath();
		_file = file;
		_caseSensitive = caseSensitive;
		_encoding = encoding;
	}

	private File getFile() {
		if (_file == null) {
			_file = new File(_filename);
		}
		return _file;
	}

	@Override
	public String getName() {
		return _name;
	}

	public String getEncoding() {
		return _encoding;
	}

	public String getFilename() {
		return _filename;
	}

	public boolean isCaseSensitive() {
		return _caseSensitive;
	}

	@Override
	public Collection<Synonym> getSynonyms() {
		BufferedReader reader = FileHelper.getBufferedReader(getFile(), _encoding);
		try {
			List<Synonym> synonyms = new ArrayList<Synonym>();
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				line = line.trim();
				synonyms.add(new TextBasedSynonym(line, _caseSensitive));
			}
			return synonyms;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private SoftHashMap<String, String> getMasterTermCache() {
		if (_masterTermCache == null) {
			synchronized (this) {
				if (_masterTermCache == null) {
					_masterTermCache = new SoftHashMap<String, String>();
				}
			}
		}
		return _masterTermCache;
	}

	@Override
	public String getMasterTerm(String term) {
		if (StringUtils.isNullOrEmpty(term)) {
			return null;
		}
		String masterTerm = getMasterTermCache().get(term);
		if (masterTerm != null) {
			return masterTerm;
		}

		BufferedReader reader = FileHelper.getBufferedReader(getFile(), _encoding);
		try {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				line = line.trim();
				TextBasedSynonym synonym = new TextBasedSynonym(line, _caseSensitive);
				masterTerm = synonym.getMasterTerm();
				if (term.equals(masterTerm) || synonym.getSynonyms().containsValue(term)) {
					getMasterTermCache().put(term, masterTerm);
					return masterTerm;
				}
			}
			return null;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
