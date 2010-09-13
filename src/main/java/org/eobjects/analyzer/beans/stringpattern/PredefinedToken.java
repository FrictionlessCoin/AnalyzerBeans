package org.eobjects.analyzer.beans.stringpattern;

public class PredefinedToken implements Token {

	private PredefinedTokenDefinition _predefinedTokenDefintion;
	private String _string;

	public PredefinedToken(PredefinedTokenDefinition tokenDefinition,
			String string) {
		_predefinedTokenDefintion = tokenDefinition;
		_string = string;
	}

	public PredefinedTokenDefinition getPredefinedTokenDefintion() {
		return _predefinedTokenDefintion;
	}
	
	@Override
	public int length() {
		return _string.length();
	}

	@Override
	public String getString() {
		return _string;
	}

	@Override
	public TokenType getType() {
		return TokenType.PREDEFINED;
	}

	@Override
	public String toString() {
		return "Token['" + _string + "' (PREDEFINED "
				+ _predefinedTokenDefintion.getName() + ")]";
	}
	
	@Override
	public char charAt(int index) {
		return _string.charAt(index);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((_predefinedTokenDefintion == null) ? 0
						: _predefinedTokenDefintion.hashCode());
		result = prime * result + ((_string == null) ? 0 : _string.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PredefinedToken other = (PredefinedToken) obj;
		if (_predefinedTokenDefintion == null) {
			if (other._predefinedTokenDefintion != null)
				return false;
		} else if (!_predefinedTokenDefintion
				.equals(other._predefinedTokenDefintion))
			return false;
		if (_string == null) {
			if (other._string != null)
				return false;
		} else if (!_string.equals(other._string))
			return false;
		return true;
	}
}
