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
package org.eobjects.analyzer.beans.valuedist;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Concurrent;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.beans.api.RowProcessingAnalyzer;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.result.ValueDistributionGroupResult;
import org.eobjects.analyzer.result.ValueDistributionResult;
import org.eobjects.analyzer.storage.CollectionFactory;
import org.eobjects.analyzer.storage.CollectionFactoryImpl;
import org.eobjects.analyzer.storage.InMemoryStorageProvider;
import org.eobjects.analyzer.util.NullTolerableComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AnalyzerBean("Value distribution")
@Description("Gets the distributions of values that occur in a dataset.\nOften used as an initial way to see if a lot of repeated values are to be expected, if nulls occur and if a few un-repeated values add exceptions to the typical usage-pattern.")
@Concurrent(true)
public class ValueDistributionAnalyzer implements RowProcessingAnalyzer<ValueDistributionResult> {

	private static final Logger logger = LoggerFactory.getLogger(ValueDistributionAnalyzer.class);

	@Inject
	@Configured(value = "Column", order = 1)
	InputColumn<?> _column;

	@Inject
	@Configured(value = "Group column", required = false, order = 2)
	InputColumn<String> _groupColumn;

	@Inject
	@Configured(value = "Record unique values", required = false, order = 3)
	boolean _recordUniqueValues = true;

	@Inject
	@Configured(value = "Top n most frequent values", required = false, order = 4)
	Integer _topFrequentValues;

	@Inject
	@Configured(value = "Bottom n most frequent values", required = false, order = 5)
	Integer _bottomFrequentValues;

	@Inject
	@Provided
	CollectionFactory _collectionFactory;

	private final Map<String, ValueDistributionGroup> _valueDistributionGroups;

	/**
	 * Constructor used for testing and ad-hoc purposes
	 * 
	 * @param column
	 * @param recordUniqueValues
	 * @param topFrequentValues
	 * @param bottomFrequentValues
	 */
	public ValueDistributionAnalyzer(InputColumn<?> column, boolean recordUniqueValues, Integer topFrequentValues,
			Integer bottomFrequentValues) {
		this(column, null, recordUniqueValues, topFrequentValues, bottomFrequentValues);
	}

	/**
	 * Constructor used for testing and ad-hoc purposes
	 * 
	 * @param column
	 * @param groupColumn
	 * @param recordUniqueValues
	 * @param topFrequentValues
	 * @param bottomFrequentValues
	 */
	public ValueDistributionAnalyzer(InputColumn<?> column, InputColumn<String> groupColumn, boolean recordUniqueValues,
			Integer topFrequentValues, Integer bottomFrequentValues) {
		this();
		_column = column;
		_groupColumn = groupColumn;
		_recordUniqueValues = recordUniqueValues;
		_topFrequentValues = topFrequentValues;
		_bottomFrequentValues = bottomFrequentValues;
		_collectionFactory = new CollectionFactoryImpl(new InMemoryStorageProvider());
	}

	/**
	 * Main constructor
	 */
	public ValueDistributionAnalyzer() {
		_valueDistributionGroups = new TreeMap<String, ValueDistributionGroup>(NullTolerableComparator.get(String.class));
	}

	@Override
	public void run(InputRow row, int distinctCount) {
		final Object value = row.getValue(_column);
		if (_groupColumn == null) {
			runInternal(value, distinctCount);
		} else {
			final String group = row.getValue(_groupColumn);
			runInternal(value, group, distinctCount);
		}
	}

	public void runInternal(Object value, int distinctCount) {
		runInternal(value, null, distinctCount);
	}

	public void runInternal(Object value, String group, int distinctCount) {
		final ValueDistributionGroup valueDistributionGroup = getValueDistributionGroup(group);
		final String stringValue;
		if (value == null) {
			logger.debug("value is null");
			stringValue = null;
		} else {
			stringValue = value.toString();
		}
		valueDistributionGroup.run(stringValue, distinctCount);
	}

	private ValueDistributionGroup getValueDistributionGroup(String group) {
		ValueDistributionGroup valueDistributionGroup = _valueDistributionGroups.get(group);
		if (valueDistributionGroup == null) {
			synchronized (this) {
				valueDistributionGroup = _valueDistributionGroups.get(group);
				if (valueDistributionGroup == null) {
					valueDistributionGroup = new ValueDistributionGroup(group, _collectionFactory);
					_valueDistributionGroups.put(group, valueDistributionGroup);
				}
			}
		}
		return valueDistributionGroup;
	}

	@Override
	public ValueDistributionResult getResult() {
		if (_groupColumn == null) {
			logger.info("getResult() invoked, processing single group");
			final ValueDistributionGroup valueDistributionGroup = getValueDistributionGroup(null);
			final ValueDistributionGroupResult ungroupedResult = valueDistributionGroup.createResult(_topFrequentValues,
					_bottomFrequentValues, _recordUniqueValues);
			return new ValueDistributionResult(_column, ungroupedResult);
		} else {
			logger.info("getResult() invoked, processing {} groups", _valueDistributionGroups.size());

			final SortedSet<ValueDistributionGroupResult> groupedResults = new TreeSet<ValueDistributionGroupResult>();
			for (String group : _valueDistributionGroups.keySet()) {
				final ValueDistributionGroup valueDistributibutionGroup = getValueDistributionGroup(group);
				final ValueDistributionGroupResult result = valueDistributibutionGroup.createResult(_topFrequentValues,
						_bottomFrequentValues, _recordUniqueValues);
				groupedResults.add(result);
			}
			return new ValueDistributionResult(_column, _groupColumn, groupedResults);
		}
	}
}
