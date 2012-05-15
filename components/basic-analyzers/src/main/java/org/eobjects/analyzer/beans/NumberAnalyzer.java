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
package org.eobjects.analyzer.beans;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.eobjects.analyzer.beans.api.Analyzer;
import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Concurrent;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.result.AnnotatedRowsResult;
import org.eobjects.analyzer.result.Crosstab;
import org.eobjects.analyzer.result.CrosstabDimension;
import org.eobjects.analyzer.result.CrosstabNavigator;
import org.eobjects.analyzer.storage.InMemoryRowAnnotationFactory;
import org.eobjects.analyzer.storage.RowAnnotation;
import org.eobjects.analyzer.storage.RowAnnotationFactory;

/**
 * Number analyzer, which provides statistical information for number values:
 * 
 * <ul>
 * <li>Highest value</li>
 * <li>Lowest value</li>
 * <li>Sum</li>
 * <li>Mean</li>
 * <li>Geometric mean</li>
 * <li>Standard deviation</li>
 * <li>Variance</li>
 * </ul>
 */
@AnalyzerBean("Number analyzer")
@Description("Provides insight into number-column values.")
@Concurrent(true)
public class NumberAnalyzer implements Analyzer<NumberAnalyzerResult> {

	public static final String DIMENSION_COLUMN = "Column";
	public static final String DIMENSION_MEASURE = "Measure";
	public static final String MEASURE_ROW_COUNT = "Row count";
	public static final String MEASURE_NULL_COUNT = "Null count";
	public static final String MEASURE_HIGHEST_VALUE = "Highest value";
	public static final String MEASURE_LOWEST_VALUE = "Lowest value";
	public static final String MEASURE_SUM = "Sum";
	public static final String MEASURE_MEAN = "Mean";
	public static final String MEASURE_GEOMETRIC_MEAN = "Geometric mean";
	public static final String MEASURE_STANDARD_DEVIATION = "Standard deviation";
	public static final String MEASURE_VARIANCE = "Variance";

	private Map<InputColumn<? extends Number>, NumberAnalyzerColumnDelegate> _columnDelegates = new HashMap<InputColumn<? extends Number>, NumberAnalyzerColumnDelegate>();

	@Configured
	InputColumn<? extends Number>[] _columns;

	@Provided
	RowAnnotationFactory _annotationFactory;

	public NumberAnalyzer() {
	}

	public NumberAnalyzer(InputColumn<? extends Number>... columns) {
		this();
		_columns = columns;
		_annotationFactory = new InMemoryRowAnnotationFactory();
		init();
	}

	@Initialize
	public void init() {
		for (InputColumn<? extends Number> column : _columns) {
			_columnDelegates.put(column, new NumberAnalyzerColumnDelegate(_annotationFactory));
		}
	}

	@Override
	public void run(InputRow row, int distinctCount) {
		for (InputColumn<? extends Number> column : _columns) {
			NumberAnalyzerColumnDelegate delegate = _columnDelegates.get(column);
			Number value = row.getValue(column);

			delegate.run(row, value, distinctCount);
		}
	}

	@Override
	public NumberAnalyzerResult getResult() {
		CrosstabDimension measureDimension = new CrosstabDimension(DIMENSION_MEASURE);
		measureDimension.addCategory(MEASURE_ROW_COUNT);
		measureDimension.addCategory(MEASURE_NULL_COUNT);
		measureDimension.addCategory(MEASURE_HIGHEST_VALUE);
		measureDimension.addCategory(MEASURE_LOWEST_VALUE);
		measureDimension.addCategory(MEASURE_SUM);
		measureDimension.addCategory(MEASURE_MEAN);
		measureDimension.addCategory(MEASURE_GEOMETRIC_MEAN);
		measureDimension.addCategory(MEASURE_STANDARD_DEVIATION);
		measureDimension.addCategory(MEASURE_VARIANCE);

		CrosstabDimension columnDimension = new CrosstabDimension(DIMENSION_COLUMN);
		for (InputColumn<? extends Number> column : _columns) {
			columnDimension.addCategory(column.getName());
		}

		Crosstab<Number> crosstab = new Crosstab<Number>(Number.class, columnDimension, measureDimension);
		for (InputColumn<? extends Number> column : _columns) {
			CrosstabNavigator<Number> nav = crosstab.navigate().where(columnDimension, column.getName());
			NumberAnalyzerColumnDelegate delegate = _columnDelegates.get(column);

			SummaryStatistics s = delegate.getStatistics();
			int nullCount = delegate.getNullCount();

			nav.where(measureDimension, MEASURE_NULL_COUNT).put(nullCount);

			if (nullCount > 0) {
				addAttachment(nav, delegate.getNullAnnotation(), column);
			}

			int numRows = delegate.getNumRows();
			nav.where(measureDimension, MEASURE_ROW_COUNT).put(numRows);

			long nonNullCount = s.getN();

			if (nonNullCount > 0) {
				double highestValue = s.getMax();
				double lowestValue = s.getMin();
				double sum = s.getSum();
				double mean = s.getMean();
				double geometricMean = s.getGeometricMean();
				double standardDeviation = s.getStandardDeviation();
				double variance = s.getVariance();

				nav.where(measureDimension, MEASURE_HIGHEST_VALUE).put(highestValue);
				addAttachment(nav, delegate.getMaxAnnotation(), column);

				nav.where(measureDimension, MEASURE_LOWEST_VALUE).put(lowestValue);
				addAttachment(nav, delegate.getMinAnnotation(), column);

				nav.where(measureDimension, MEASURE_SUM).put(sum);
				nav.where(measureDimension, MEASURE_MEAN).put(mean);
				nav.where(measureDimension, MEASURE_GEOMETRIC_MEAN).put(geometricMean);
				nav.where(measureDimension, MEASURE_STANDARD_DEVIATION).put(standardDeviation);
				nav.where(measureDimension, MEASURE_VARIANCE).put(variance);
			}
		}
		return new NumberAnalyzerResult(_columns, crosstab);
	}

	private void addAttachment(CrosstabNavigator<Number> nav, RowAnnotation annotation, InputColumn<?> column) {
		nav.attach(new AnnotatedRowsResult(annotation, _annotationFactory, column));
	}
}
