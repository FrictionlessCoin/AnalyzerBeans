package org.eobjects.analyzer.beans;

import java.util.StringTokenizer;

import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.storage.RowAnnotation;
import org.eobjects.analyzer.storage.RowAnnotationFactory;
import org.eobjects.analyzer.util.AverageBuilder;
import org.eobjects.analyzer.util.CharIterator;

/**
 * Helper class for the String Analyzer. This class collects all the statistics
 * for a single column. The String Analyzer then consists of a number of these
 * delegates.
 * 
 * @author Kasper Sørensen
 * 
 */
final class StringAnalyzerColumnDelegate {

	private final RowAnnotationFactory _annotationFactory;
	private final AverageBuilder _charAverageBuilder = new AverageBuilder();
	private final AverageBuilder _whitespaceAverageBuilder = new AverageBuilder();
	private final RowAnnotation _nullAnnotation;
	private final RowAnnotation _entirelyUppercaseAnnotation;
	private final RowAnnotation _entirelyLowercaseAnnotation;
	private final RowAnnotation _maxCharsAnnotation;
	private final RowAnnotation _minCharsAnnotation;
	private final RowAnnotation _maxWhitespaceAnnotation;
	private final RowAnnotation _minWhitespaceAnnotation;
	private final RowAnnotation _uppercaseExclFirstLetterAnnotation;
	private final RowAnnotation _digitAnnotation;
	private final RowAnnotation _diacriticAnnotation;
	private final RowAnnotation _maxWordsAnnotation;
	private final RowAnnotation _minWordsAnnotation;
	private int _numRows;
	private int _numNull;
	private int _numEntirelyUppercase;
	private int _numEntirelyLowercase;
	private int _numChars;
	private Integer _minChars;
	private Integer _maxChars;
	private Integer _minWhitespace;
	private Integer _maxWhitespace;
	private int _numUppercase;
	private int _numUppercaseExclFirstLetter;
	private int _numLowercase;
	private int _numDigit;
	private int _numDiacritics;
	private int _numNonLetter;
	private int _numWords;
	private Integer _maxWords;
	private Integer _minWords;

	public StringAnalyzerColumnDelegate(RowAnnotationFactory annotationFactory) {
		_annotationFactory = annotationFactory;
		_nullAnnotation = annotationFactory.createAnnotation();
		_entirelyUppercaseAnnotation = annotationFactory.createAnnotation();
		_entirelyLowercaseAnnotation = annotationFactory.createAnnotation();
		_maxCharsAnnotation = annotationFactory.createAnnotation();
		_minCharsAnnotation = annotationFactory.createAnnotation();
		_maxWhitespaceAnnotation = annotationFactory.createAnnotation();
		_minWhitespaceAnnotation = annotationFactory.createAnnotation();
		_uppercaseExclFirstLetterAnnotation = annotationFactory.createAnnotation();
		_digitAnnotation = annotationFactory.createAnnotation();
		_diacriticAnnotation = annotationFactory.createAnnotation();
		_maxWordsAnnotation = annotationFactory.createAnnotation();
		_minWordsAnnotation = annotationFactory.createAnnotation();
	}

	public void run(InputRow row, String value, int distinctCount) {
		_numRows += distinctCount;

		if (value == null) {
			_numNull += distinctCount;
			_annotationFactory.annotate(row, distinctCount, _nullAnnotation);
		} else {
			int numChars = value.length();
			int totalChars = numChars * distinctCount;
			int numWords = new StringTokenizer(value).countTokens();
			int totalWords = numWords * distinctCount;

			int numWhitespace = 0;
			int numDigits = 0;
			int numDiacritics = 0;
			int numLetters = 0;
			int numNonLetters = 0;
			int numUppercase = 0;
			int numUppercaseExclFirstLetter = 0;
			int numLowercase = 0;

			boolean firstLetter = true;
			CharIterator it = new CharIterator(value);
			while (it.hasNext()) {
				it.next();
				if (it.isLetter()) {
					numLetters += distinctCount;
					if (it.isUpperCase()) {
						numUppercase += distinctCount;
						if (!firstLetter) {
							numUppercaseExclFirstLetter += distinctCount;
						}
					} else {
						numLowercase += distinctCount;
					}
					if (it.isDiacritic()) {
						numDiacritics += distinctCount;
					}
					firstLetter = false;
				} else {
					numNonLetters += distinctCount;
					if (it.isDigit()) {
						numDigits += distinctCount;
					}
					if (it.isWhitespace()) {
						numWhitespace++;
					}
					if (it.is('.')) {
						firstLetter = true;
					}
				}
			}

			_numUppercase += +numUppercase;
			if (numUppercaseExclFirstLetter > 0) {
				_annotationFactory.annotate(row, distinctCount, _uppercaseExclFirstLetterAnnotation);
				_numUppercaseExclFirstLetter += numUppercaseExclFirstLetter;
			}
			_numLowercase += numLowercase;
			_numNonLetter += numNonLetters;

			if (_minChars == null) {
				// This is the first time we encounter a non-null value, so
				// we just set all counters
				_minChars = numChars;
				_maxChars = numChars;
				_minWords = numWords;
				_maxWords = numWords;
				_minWhitespace = numWhitespace;
				_maxWhitespace = numWhitespace;
			}

			_numChars += totalChars;
			_numWords += totalWords;

			if (numDiacritics > 0) {
				_numDiacritics += numDiacritics;
				_annotationFactory.annotate(row, distinctCount, _diacriticAnnotation);
			}

			if (numDigits > 0) {
				_numDigit += numDigits;
				_annotationFactory.annotate(row, distinctCount, _digitAnnotation);
			}

			if (_maxChars < numChars) {
				_annotationFactory.reset(_maxCharsAnnotation);
				_maxChars = numChars;
			}
			if (_maxChars == numChars) {
				_annotationFactory.annotate(row, distinctCount, _maxCharsAnnotation);
			}

			if (_minChars > numChars) {
				_annotationFactory.reset(_minCharsAnnotation);
				_minChars = numChars;
			}
			if (_minChars == numChars) {
				_annotationFactory.annotate(row, distinctCount, _minCharsAnnotation);
			}

			if (_maxWords < numWords) {
				_maxWords = numWords;
				_annotationFactory.reset(_maxWordsAnnotation);
			}
			if (_maxWords == numWords) {
				_annotationFactory.annotate(row, distinctCount, _maxWordsAnnotation);
			}
			if (_minWords > numWords) {
				_minWords = numWords;
				_annotationFactory.reset(_minWordsAnnotation);
			}
			if (_minWords == numWords) {
				_annotationFactory.annotate(row, distinctCount, _minWordsAnnotation);
			}

			if (_maxWhitespace < numWhitespace) {
				_maxWhitespace = numWhitespace;
				_annotationFactory.reset(_maxWhitespaceAnnotation);
			}
			if (_maxWhitespace == numWhitespace) {
				_annotationFactory.annotate(row, distinctCount, _maxWhitespaceAnnotation);
			}

			if (_minWhitespace > numWhitespace) {
				_minWhitespace = numWhitespace;
				_annotationFactory.reset(_minWhitespaceAnnotation);
			}
			if (_minWhitespace == numWhitespace) {
				_annotationFactory.annotate(row, distinctCount, _minWhitespaceAnnotation);
			}

			if (numLetters > 0) {
				if (value.equals(value.toUpperCase())) {
					_numEntirelyUppercase += distinctCount;
					_annotationFactory.annotate(row, distinctCount, _entirelyUppercaseAnnotation);
				}

				if (value.equals(value.toLowerCase())) {
					_numEntirelyLowercase += distinctCount;
					_annotationFactory.annotate(row, distinctCount, _entirelyLowercaseAnnotation);
				}
			}

			_charAverageBuilder.addValue(numChars);
			_whitespaceAverageBuilder.addValue(numWhitespace);
		}
	}

	public int getNumRows() {
		return _numRows;
	}

	public int getNumNull() {
		return _numNull;
	}

	public int getNumEntirelyUppercase() {
		return _numEntirelyUppercase;
	}

	public int getNumEntirelyLowercase() {
		return _numEntirelyLowercase;
	}

	public int getNumChars() {
		return _numChars;
	}

	public Integer getMinChars() {
		return _minChars;
	}

	public Integer getMaxChars() {
		return _maxChars;
	}

	public Integer getMinWhitespace() {
		return _minWhitespace;
	}

	public Integer getMaxWhitespace() {
		return _maxWhitespace;
	}

	public int getNumUppercase() {
		return _numUppercase;
	}

	public int getNumUppercaseExclFirstLetter() {
		return _numUppercaseExclFirstLetter;
	}

	public int getNumLowercase() {
		return _numLowercase;
	}

	public int getNumDigit() {
		return _numDigit;
	}

	public int getNumDiacritics() {
		return _numDiacritics;
	}

	public int getNumNonLetter() {
		return _numNonLetter;
	}

	public int getNumWords() {
		return _numWords;
	}

	public Integer getMinWords() {
		return _minWords;
	}

	public Integer getMaxWords() {
		return _maxWords;
	}

	public AverageBuilder getCharAverageBuilder() {
		return _charAverageBuilder;
	}

	public AverageBuilder getWhitespaceAverageBuilder() {
		return _whitespaceAverageBuilder;
	}

	public RowAnnotation getNullAnnotation() {
		return _nullAnnotation;
	}

	public RowAnnotation getEntirelyUppercaseAnnotation() {
		return _entirelyUppercaseAnnotation;
	}

	public RowAnnotation getEntirelyLowercaseAnnotation() {
		return _entirelyLowercaseAnnotation;
	}

	public RowAnnotation getMaxCharsAnnotation() {
		return _maxCharsAnnotation;
	}

	public RowAnnotation getMinCharsAnnotation() {
		return _minCharsAnnotation;
	}

	public RowAnnotation getMaxWhitespaceAnnotation() {
		return _maxWhitespaceAnnotation;
	}

	public RowAnnotation getMinWhitespaceAnnotation() {
		return _minWhitespaceAnnotation;
	}

	public RowAnnotation getUppercaseExclFirstLetterAnnotation() {
		return _uppercaseExclFirstLetterAnnotation;
	}

	public RowAnnotation getDigitAnnotation() {
		return _digitAnnotation;
	}

	public RowAnnotation getDiacriticAnnotation() {
		return _diacriticAnnotation;
	}

	public RowAnnotation getMaxWordsAnnotation() {
		return _maxWordsAnnotation;
	}

	public RowAnnotation getMinWordsAnnotation() {
		return _minWordsAnnotation;
	}
}
