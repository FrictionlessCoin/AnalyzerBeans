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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eobjects.analyzer.util.CollectionUtils;
import org.eobjects.metamodel.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextBasedDictionary implements Dictionary {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(TextBasedDictionary.class);

	private transient Map<String, Boolean> _containsValueCache;
	private final String _name;
	private final String _filename;
	private final String _encoding;

	public TextBasedDictionary(String name, String filename, String encoding) {
		_name = name;
		_filename = filename;
		_encoding = encoding;
	}

	private Map<String, Boolean> getContainsValueCache() {
		if (_containsValueCache == null) {
			synchronized (this) {
				if (_containsValueCache == null) {
					_containsValueCache = CollectionUtils.createCacheMap();
				}
			}
		}
		return _containsValueCache;
	}

	@Override
	public String getName() {
		return _name;
	}

	public String getFilename() {
		return _filename;
	}

	public String getEncoding() {
		return _encoding;
	}

	@Override
	public boolean containsValue(String value) {
		if (value == null) {
			return false;
		}
		Boolean result = getContainsValueCache().get(value);
		if (result == null) {
			BufferedReader reader = null;
			try {
				result = false;
				reader = FileHelper.getBufferedReader(new File(_filename), _encoding);
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					if (value.equals(line)) {
						result = true;
						break;
					}
				}
				getContainsValueCache().put(value, result);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						logger.error("Exception occurred when closing reader", e);
					}
				}
			}
		}
		return result;
	}

	@Override
	public ReferenceValues<String> getValues() {
		BufferedReader reader = null;
		try {
			Set<String> values = new HashSet<String>();
			reader = FileHelper.getBufferedReader(new File(_filename), _encoding);
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				values.add(line);
			}
			return new SimpleStringReferenceValues(values, true);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					logger.error("Exception occurred when closing reader", e);
				}
			}
		}
	}

}
