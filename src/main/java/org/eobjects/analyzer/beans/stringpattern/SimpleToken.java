package org.eobjects.analyzer.beans.stringpattern;

public class SimpleToken implements Token {

	private TokenType _type;
	private StringBuilder _stringBuilder;

	public SimpleToken(TokenType type, StringBuilder stringBuilder) {
		_type = type;
		_stringBuilder = stringBuilder;
	}

	public SimpleToken(TokenType type, String string) {
		_type = type;
		_stringBuilder = new StringBuilder(string);
	}

	public SimpleToken(TokenType type, char c) {
		_type = type;
		_stringBuilder = new StringBuilder();
		_stringBuilder.append(c);
	}

	@Override
	public String getString() {
		return _stringBuilder.toString();
	}

	public void appendChar(char c) {
		_stringBuilder.append(c);
	}

	public void appendString(String s) {
		_stringBuilder.append(s);
	}

	public void prependChar(char c) {
		_stringBuilder.insert(0, c);
	}

	@Override
	public TokenType getType() {
		return _type;
	}

	public void setType(TokenType type) {
		_type = type;
	}

	@Override
	public String toString() {
		return "Token['" + getString() + "' (" + _type + ")]";
	}
}