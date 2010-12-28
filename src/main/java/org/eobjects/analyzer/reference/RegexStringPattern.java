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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.eobjects.metamodel.util.BaseObject;

/**
 * Represents a string pattern which is based on a regular expression (regex).
 * There are two basic modes of matching when using a regex string pattern:
 * Using entire string matching or subsequence matching. This mode is determined
 * using the <code>matchEntireString</code> property.
 * 
 * @author Kasper Sørensen
 */
public final class RegexStringPattern extends BaseObject implements StringPattern {

	private static final long serialVersionUID = 1L;

	private final String _name;
	private final String _expression;
	private final boolean _matchEntireString;
	private transient Pattern _pattern;

	public RegexStringPattern(String name, String expression, boolean matchEntireString) {
		_name = name;
		_expression = expression;
		_matchEntireString = matchEntireString;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public boolean matches(String string) {
		if (string == null) {
			return false;
		}
		Matcher matcher = getPattern().matcher(string);

		if (matcher.find()) {
			if (_matchEntireString) {
				int s = matcher.start();
				if (s == 0) {
					int e = matcher.end();
					if (e == string.length()) {
						return true;
					}
				}
				return false;
			}
			return true;
		}
		return false;
	}

	public Pattern getPattern() {
		synchronized (this) {
			if (_pattern == null) {
				_pattern = Pattern.compile(_expression);
			}
		}
		return _pattern;
	}

	public boolean isMatchEntireString() {
		return _matchEntireString;
	}

	public String getExpression() {
		return _expression;
	}

	@Override
	protected void decorateIdentity(List<Object> identifiers) {
		identifiers.add(_name);
		identifiers.add(_expression);
		identifiers.add(_matchEntireString);
	}

	@Override
	public String toString() {
		return "RegexStringPattern[name=" + _name + ",expression=" + _expression + "]";
	}

}
