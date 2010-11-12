package org.eobjects.analyzer.beans;

import javax.inject.Inject;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.beans.api.RowProcessingAnalyzer;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.result.ValidationResult;
import org.eobjects.analyzer.storage.RowAnnotation;
import org.eobjects.analyzer.storage.RowAnnotationFactory;
import org.eobjects.analyzer.util.CompareUtils;

/**
 * An analyzer that picks up rows where configured values are not equal
 * 
 * @author Kasper Sørensen
 */
@AnalyzerBean("Equality validation")
@Description("Validates that values from two separate columns are equal")
public class EqualityValidationAnalyzer implements RowProcessingAnalyzer<ValidationResult> {

	@Inject
	@Configured
	@Description("Values in this columns...")
	InputColumn<?> input1;

	@Inject
	@Configured
	@Description("... should be equal to values in the column")
	InputColumn<?> input2;

	@Provided
	RowAnnotationFactory _rowAnnotationFactory;

	private RowAnnotation _rowAnnotation;

	@Initialize
	public void init() {
		_rowAnnotation = _rowAnnotationFactory.createAnnotation();
	}

	@Override
	public void run(InputRow row, int distinctCount) {
		Object v1 = row.getValue(input1);
		Object v2 = row.getValue(input2);

		if (!CompareUtils.equals(v1, v2)) {
			_rowAnnotationFactory.annotate(row, distinctCount, _rowAnnotation);
		}
	}

	@Override
	public ValidationResult getResult() {
		return new ValidationResult(_rowAnnotation, _rowAnnotationFactory, input1, input2);
	}
}
