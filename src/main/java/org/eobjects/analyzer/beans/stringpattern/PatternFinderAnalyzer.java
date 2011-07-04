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
package org.eobjects.analyzer.beans.stringpattern;

import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Concurrent;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.beans.api.RowProcessingAnalyzer;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.result.AnnotatedRowsResult;
import org.eobjects.analyzer.result.Crosstab;
import org.eobjects.analyzer.result.CrosstabDimension;
import org.eobjects.analyzer.result.CrosstabNavigator;
import org.eobjects.analyzer.result.PatternFinderResult;
import org.eobjects.analyzer.storage.RowAnnotation;
import org.eobjects.analyzer.storage.RowAnnotationFactory;
import org.eobjects.analyzer.util.CollectionUtils;
import org.eobjects.analyzer.util.NullTolerableComparator;

@AnalyzerBean("Pattern finder")
@Description("The Pattern Finder will inspect your String values and generate and match string patterns that suit your data.\nIt can be used for a lot of purposes but is excellent for verifying or getting ideas about the format of the string-values in a column.")
@Concurrent(true)
public class PatternFinderAnalyzer implements RowProcessingAnalyzer<PatternFinderResult> {

	@Configured(order = 1)
	InputColumn<String> column;

	@Configured(required = false, order = 2)
	@Description("Optional column to group patterns by")
	InputColumn<String> groupColumn;

	@Configured(required = false, order = 3)
	@Description("Separate text tokens based on case")
	Boolean discriminateTextCase = true;

	@Configured(required = false, order = 4)
	@Description("Separate number tokens based on negativity")
	Boolean discriminateNegativeNumbers = false;

	@Configured(required = false, order = 5)
	@Description("Separate number tokens for decimals")
	Boolean discriminateDecimals = true;

	@Configured(required = false, order = 6)
	@Description("Use '?'-tokens for mixed text and numbers")
	Boolean enableMixedTokens = true;

	@Configured(required = false, order = 7)
	@Description("Ignore whitespace differences")
	Boolean ignoreRepeatedSpaces = false;

	@Configured(required = false, value = "Upper case patterns expand in size", order = 8)
	@Description("Auto-adjust/expand uppercase text tokens")
	boolean upperCaseExpandable = false;

	@Configured(required = false, value = "Lower case patterns expand in size", order = 9)
	@Description("Auto-adjust/expand lowercase text tokens")
	boolean lowerCaseExpandable = true;

	@Configured(required = false, value = "Predefined token name", order = 10)
	String predefinedTokenName;

	@Configured(required = false, value = "Predefined token regexes", order = 11)
	String[] predefinedTokenPatterns;

	@Configured(required = false, order = 12)
	Character decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();

	@Configured(required = false, order = 13)
	Character thousandsSeparator = DecimalFormatSymbols.getInstance().getGroupingSeparator();

	@Configured(required = false, order = 14)
	Character minusSign = DecimalFormatSymbols.getInstance().getMinusSign();

	private Map<String, DefaultPatternFinder> _patternFinders;
	private TokenizerConfiguration _configuration;

	@Provided
	RowAnnotationFactory _rowAnnotationFactory;

	@Initialize
	public void init() {
		if (enableMixedTokens != null) {
			_configuration = new TokenizerConfiguration(enableMixedTokens);
		} else {
			_configuration = new TokenizerConfiguration();
		}

		_configuration.setUpperCaseExpandable(upperCaseExpandable);
		_configuration.setLowerCaseExpandable(lowerCaseExpandable);

		if (discriminateNegativeNumbers != null) {
			_configuration.setDiscriminateNegativeNumbers(discriminateNegativeNumbers);
		}

		if (discriminateDecimals != null) {
			_configuration.setDiscriminateDecimalNumbers(discriminateDecimals);
		}

		if (discriminateTextCase != null) {
			_configuration.setDiscriminateTextCase(discriminateTextCase);
		}

		if (ignoreRepeatedSpaces != null) {
			boolean ignoreSpacesLength = ignoreRepeatedSpaces.booleanValue();
			_configuration.setDistriminateTokenLength(TokenType.WHITESPACE, !ignoreSpacesLength);
		}

		if (decimalSeparator != null) {
			_configuration.setDecimalSeparator(decimalSeparator);
		}

		if (thousandsSeparator != null) {
			_configuration.setThousandsSeparator(thousandsSeparator);
		}

		if (minusSign != null) {
			_configuration.setMinusSign(minusSign);
		}

		if (predefinedTokenName != null && predefinedTokenPatterns != null) {
			Set<String> tokenRegexes = CollectionUtils.set(predefinedTokenPatterns);
			_configuration.getPredefinedTokens().add(new PredefinedTokenDefinition(predefinedTokenName, tokenRegexes));
		}

		_patternFinders = new HashMap<String, DefaultPatternFinder>();
	}

	@Override
	public void run(InputRow row, int distinctCount) {
		final String group;
		if (groupColumn == null) {
			group = null;
		} else {
			group = row.getValue(groupColumn);
		}
		final String value = row.getValue(column);

		run(group, value, row, distinctCount);
	}

	private void run(String group, String value, InputRow row, int distinctCount) {
		DefaultPatternFinder patternFinder = getPatternFinderForGroup(group);
		patternFinder.run(row, value, distinctCount);
	}

	private DefaultPatternFinder getPatternFinderForGroup(String group) {
		DefaultPatternFinder patternFinder = _patternFinders.get(group);
		if (patternFinder == null) {
			synchronized (this) {
				patternFinder = _patternFinders.get(group);
				if (patternFinder == null) {
					patternFinder = new DefaultPatternFinder(_configuration, _rowAnnotationFactory);
					_patternFinders.put(group, patternFinder);
				}
			}
		}
		return patternFinder;
	}

	@Override
	public PatternFinderResult getResult() {
		if (groupColumn == null) {
			Crosstab<?> crosstab = createCrosstab(getPatternFinderForGroup(null));
			return new PatternFinderResult(column, crosstab);
		} else {
			final Map<String, Crosstab<?>> crosstabs = new TreeMap<String, Crosstab<?>>(
					NullTolerableComparator.get(String.class));
			final Set<Entry<String, DefaultPatternFinder>> patternFinderEntries = _patternFinders.entrySet();
			for (Entry<String, DefaultPatternFinder> entry : patternFinderEntries) {
				final DefaultPatternFinder patternFinder = entry.getValue();
				final Crosstab<Serializable> crosstab = createCrosstab(patternFinder);
				crosstabs.put(entry.getKey(), crosstab);
			}
			return new PatternFinderResult(column, groupColumn, crosstabs);
		}
	}

	private Crosstab<Serializable> createCrosstab(DefaultPatternFinder patternFinder) {
		CrosstabDimension measuresDimension = new CrosstabDimension("Measures");
		measuresDimension.addCategory("Match count");
		CrosstabDimension patternDimension = new CrosstabDimension("Pattern");
		Crosstab<Serializable> crosstab = new Crosstab<Serializable>(Serializable.class, measuresDimension, patternDimension);

		Set<Entry<TokenPattern, RowAnnotation>> entrySet = patternFinder.getAnnotations().entrySet();

		// sort the entries so that the ones with the highest amount of
		// matches
		// are at the top
		Set<Entry<TokenPattern, RowAnnotation>> sortedEntrySet = new TreeSet<Entry<TokenPattern, RowAnnotation>>(
				new Comparator<Entry<TokenPattern, RowAnnotation>>() {
					public int compare(Entry<TokenPattern, RowAnnotation> o1, Entry<TokenPattern, RowAnnotation> o2) {
						int result = o2.getValue().getRowCount() - o1.getValue().getRowCount();
						if (result == 0) {
							result = o1.getKey().toSymbolicString().compareTo(o2.getKey().toSymbolicString());
						}
						return result;
					}
				});
		sortedEntrySet.addAll(entrySet);

		for (Entry<TokenPattern, RowAnnotation> entry : sortedEntrySet) {
			CrosstabNavigator<Serializable> nav = crosstab.where(patternDimension, entry.getKey().toSymbolicString());

			nav.where(measuresDimension, "Match count");
			nav.where(patternDimension, entry.getKey().toSymbolicString());
			RowAnnotation annotation = entry.getValue();
			int size = annotation.getRowCount();
			nav.put(size, true);
			nav.attach(new AnnotatedRowsResult(annotation, _rowAnnotationFactory, column));

			nav.where(measuresDimension, "Sample");
			nav.put(entry.getKey().getSampleString(), true);
		}
		return crosstab;
	}

	// setter methods for unittesting purposes
	public void setRowAnnotationFactory(RowAnnotationFactory rowAnnotationFactory) {
		_rowAnnotationFactory = rowAnnotationFactory;
	}

	public void setColumn(InputColumn<String> column) {
		this.column = column;
	}

	public void setPredefinedTokenName(String predefinedTokenName) {
		this.predefinedTokenName = predefinedTokenName;
	}

	public void setPredefinedTokenPatterns(String[] predefinedTokenPatterns) {
		this.predefinedTokenPatterns = predefinedTokenPatterns;
	}

	public void setDiscriminateTextCase(Boolean discriminateTextCase) {
		this.discriminateTextCase = discriminateTextCase;
	}

	public void setDiscriminateNegativeNumbers(Boolean discriminateNegativeNumbers) {
		this.discriminateNegativeNumbers = discriminateNegativeNumbers;
	}

	public void setDiscriminateDecimals(Boolean discriminateDecimals) {
		this.discriminateDecimals = discriminateDecimals;
	}

	public void setEnableMixedTokens(Boolean enableMixedTokens) {
		this.enableMixedTokens = enableMixedTokens;
	}

	public void setUpperCaseExpandable(boolean upperCaseExpandable) {
		this.upperCaseExpandable = upperCaseExpandable;
	}

	public void setLowerCaseExpandable(boolean lowerCaseExpandable) {
		this.lowerCaseExpandable = lowerCaseExpandable;
	}

	public void setDecimalSeparator(Character decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	public void setIgnoreRepeatedSpaces(Boolean ignoreRepeatedSpaces) {
		this.ignoreRepeatedSpaces = ignoreRepeatedSpaces;
	}

	public void setMinusSign(Character minusSign) {
		this.minusSign = minusSign;
	}

	public void setThousandsSeparator(Character thousandsSeparator) {
		this.thousandsSeparator = thousandsSeparator;
	}

	public void setGroupColumn(InputColumn<String> groupColumn) {
		this.groupColumn = groupColumn;
	}
}
