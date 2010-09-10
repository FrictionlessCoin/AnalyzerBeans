package org.eobjects.analyzer.util;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Schema;
import dk.eobjects.metamodel.schema.Table;

/**
 * Helper class for converting objects to and from string representations as
 * used for example in serialized XML jobs.
 * 
 * The string converter corrently supports instances and arrays of:
 * <ul>
 * <li>Boolean</li>
 * <li>Byte</li>
 * <li>Short</li>
 * <li>Long</li>
 * <li>Integer</li>
 * <li>Double</li>
 * <li>String</li>
 * <li>java.util.Date</li>
 * <li>java.sql.Date</li>
 * <li>java.util.Calendar</li>
 * <li>dk.eobjects.metamodel.schema.Column</li>
 * <li>dk.eobjects.metamodel.schema.Table</li>
 * <li>dk.eobjects.metamodel.schema.Schema</li>
 * </ul>
 * 
 * @author Kasper Sørensen
 */
public final class StringConversionUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(StringConversionUtils.class);

	private static final String[][] ESCAPE_MAPPING = { { "&amp;", "&" },
			{ "&#91;", "[" }, { "&#93;", "]" }, { "&#44;", "," },
			{ "&lt;", "<" }, { "&gt;", ">" }, { "&quot;", "\"" },
			{ "&copy;", "\u00a9" }, { "&reg;", "\u00ae" },
			{ "&euro;", "\u20a0" } };

	// ISO 8601
	private static final String dateFormatString = "yyyy-MM-dd'T'HH:mm:ss S";

	public static final String serialize(Object o) {
		if (o == null) {
			logger.debug("o is null!");
			return "<null>";
		}

		if (ReflectionUtils.isArray(o)) {
			StringBuilder sb = new StringBuilder();
			int length = Array.getLength(o);
			sb.append('[');
			for (int i = 0; i < length; i++) {
				Object obj = Array.get(o, i);
				if (i != 0) {
					sb.append(',');
				}
				sb.append(serialize(obj));
			}
			sb.append(']');
			return sb.toString();
		}

		if (o instanceof Schema) {
			return escape(((Schema) o).getName());
		}
		if (o instanceof Table) {
			return escape(((Table) o).getQualifiedLabel());
		}
		if (o instanceof Column) {
			return escape(((Column) o).getQualifiedLabel());
		}
		if (o instanceof Boolean || o instanceof Number || o instanceof String) {
			return escape(o.toString());
		}
		if (o instanceof java.sql.Date) {
			o = new Date(((java.sql.Date) o).getTime());
		}
		if (o instanceof Calendar) {
			o = ((Calendar) o).getTime();
		}
		if (o instanceof Date) {
			return new SimpleDateFormat(dateFormatString).format((Date) o);
		}

		logger.warn("Could not convert type: {}", o.getClass().getName());
		return escape(o.toString());
	}

	private static final String escape(String str) {
		for (String[] mapping : ESCAPE_MAPPING) {
			String escapedValue = mapping[1];
			if (str.contains(escapedValue)) {
				str = str.replace(escapedValue, mapping[0]);
			}
		}
		return str;
	}

	private static final String unescape(String str) {
		for (String[] mapping : ESCAPE_MAPPING) {
			String unescapedValue = mapping[0];
			if (str.contains(unescapedValue)) {
				str = str.replaceAll(unescapedValue, mapping[1]);
			}
		}
		return str;
	}

	/**
	 * Deserializes a string to a java object.
	 * 
	 * @param <E>
	 * @param str
	 * @param type
	 * @param schemaNavigator
	 *            schema navigator to use when type is Column, Table or Schema.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static final <E> E deserialize(String str, Class<E> type,
			SchemaNavigator schemaNavigator) {
		logger.debug("deserialize(\"{}\", {})", str, type);
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}

		if ("<null>".equals(str)) {
			return null;
		}

		if (type.isArray()) {
			return (E) deserializeArray(str, type, schemaNavigator);
		}
		if (ReflectionUtils.isString(type)) {
			return (E) unescape(str);
		}
		if (ReflectionUtils.isBoolean(type)) {
			return (E) Boolean.valueOf(str);
		}
		if (ReflectionUtils.isInteger(type)) {
			return (E) Integer.valueOf(str);
		}
		if (ReflectionUtils.isLong(type)) {
			return (E) Long.valueOf(str);
		}
		if (ReflectionUtils.isByte(type)) {
			return (E) Byte.valueOf(str);
		}
		if (ReflectionUtils.isShort(type)) {
			return (E) Short.valueOf(str);
		}
		if (ReflectionUtils.isDouble(type)) {
			return (E) Double.valueOf(str);
		}
		if (ReflectionUtils.isFloat(type)) {
			return (E) Float.valueOf(str);
		}
		if (ReflectionUtils.isDate(type)) {
			return (E) toDate(str);
		}
		if (ReflectionUtils.is(type, Calendar.class)) {
			Date date = toDate(str);
			Calendar c = Calendar.getInstance();
			c.setTime(date);
			return (E) c;
		}
		if (ReflectionUtils.is(type, java.sql.Date.class)) {
			Date date = toDate(str);
			return (E) new java.sql.Date(date.getTime());
		}
		if (ReflectionUtils.isColumn(type)) {
			return (E) schemaNavigator.convertToColumn(str);
		}
		if (ReflectionUtils.isTable(type)) {
			return (E) schemaNavigator.convertToTable(str);
		}
		if (ReflectionUtils.isSchema(type)) {
			return (E) schemaNavigator.convertToSchema(str);
		}

		throw new IllegalArgumentException("Could not convert to type: "
				+ type.getName());
	}

	private static final Date toDate(String str) {
		try {
			return (Date) new SimpleDateFormat(dateFormatString).parse(str);
		} catch (ParseException e) {
			logger.error("Could not parse date: " + str, e);
			throw new IllegalArgumentException(e);
		}
	}

	private static final Object deserializeArray(final String str,
			Class<?> type, SchemaNavigator schemaNavigator) {
		Class<?> componentType = type.getComponentType();

		if (logger.isDebugEnabled()) {
			logger.debug("deserializeArray(\"{}\")", str);
			logger.debug("component type is: {}", componentType);

			char[] charArray = str.toCharArray();
			int beginningBrackets = 0;
			int endingBrackets = 0;
			for (char c : charArray) {
				if (c == '[') {
					beginningBrackets++;
				} else if (c == ']') {
					endingBrackets++;
				}
			}
			logger.debug("brackets statistics: beginning={}, ending={}",
					beginningBrackets, endingBrackets);
			if (beginningBrackets != endingBrackets) {
				logger.warn("Unbalanced beginning and ending brackets!");
			}
		}

		if ("[]".equals(str)) {
			logger.debug("found [], returning empty array");
			return Array.newInstance(componentType, 0);
		}

		if (!str.startsWith("[") || !str.endsWith("]")) {
			if (str.indexOf(',') == -1) {
				Object result = Array.newInstance(componentType, 1);
				Array.set(result, 0,
						deserialize(str, componentType, schemaNavigator));
				return result;
			}
			throw new IllegalArgumentException(
					"Cannot parse string as array, bracket encapsulation and comma delimitors expected. Found: "
							+ str);
		}

		final String innerString = str.substring(1, str.length() - 1);
		logger.debug("innerString: {}", innerString);

		List<Object> objects = new ArrayList<Object>();
		int offset = 0;
		while (offset < innerString.length()) {
			logger.debug("offset: {}", offset);
			final int commaIndex = innerString.indexOf(',', offset);
			logger.debug("commaIndex: {}", commaIndex);
			final int bracketBeginIndex = innerString.indexOf('[', offset);
			logger.debug("bracketBeginIndex: {}", bracketBeginIndex);

			if (commaIndex == -1) {
				logger.debug("no comma found");
				String s = innerString.substring(offset);
				objects.add(deserialize(s, componentType, schemaNavigator));
				offset = innerString.length();
			} else if (bracketBeginIndex == -1
					|| commaIndex < bracketBeginIndex) {
				String s = innerString.substring(offset, commaIndex);
				if ("".equals(s)) {
					offset++;
				} else {
					logger.debug("no brackets in next element: \"{}\"", s);
					objects.add(deserialize(s, componentType, schemaNavigator));
					offset = commaIndex + 1;
				}
			} else {

				String s = innerString.substring(bracketBeginIndex);
				int nextBracket = 0;
				int depth = 1;
				logger.debug("substring with nested array: {}", s);

				while (depth > 0) {
					final int searchOffset = nextBracket + 1;
					int nextEndBracket = s.indexOf(']', searchOffset);
					if (nextEndBracket == -1) {
						throw new IllegalStateException(
								"No ending bracket in array string: "
										+ s.substring(searchOffset));
					}
					int nextBeginBracket = s.indexOf('[', searchOffset);
					if (nextBeginBracket == -1) {
						nextBeginBracket = s.length();
					}

					nextBracket = Math.min(nextEndBracket, nextBeginBracket);
					char c = s.charAt(nextBracket);
					logger.debug("nextBracket: {} ({})", nextBracket, c);

					if (c == '[') {
						depth++;
					} else if (c == ']') {
						depth--;
					} else {
						throw new IllegalStateException("Unexpected char: " + c);
					}
					logger.debug("depth: {}", depth);
					if (depth == 0) {
						s = s.substring(0, nextBracket + 1);
						logger.debug("identified array: {}", s);
					}
				}

				logger.debug("recursing to nested array: {}", s);

				logger.debug("inner array string: " + s);
				objects.add(deserializeArray(s, componentType, schemaNavigator));

				offset = bracketBeginIndex + s.length();
			}
		}

		Object result = Array.newInstance(componentType, objects.size());
		for (int i = 0; i < objects.size(); i++) {
			Array.set(result, i, objects.get(i));
		}
		return result;
	}
}