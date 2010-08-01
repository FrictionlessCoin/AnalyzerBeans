package org.eobjects.analyzer.beans;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.ObjectUtils;
import org.eobjects.analyzer.annotations.AnalyzerBean;
import org.eobjects.analyzer.annotations.Configured;
import org.eobjects.analyzer.annotations.Result;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;

@AnalyzerBean("Equality analyzer")
public class EqualityAnalyzer implements RowProcessingAnalyzer {

	@Inject
	@Configured
	InputColumn<?>[] input;

	List<Object[]> invalidRows = new ArrayList<Object[]>();

	@Override
	public void run(InputRow row, int distinctCount) {
		Object[] rowValues = new Object[input.length];
		for (int i = 0; i < rowValues.length; i++) {
			rowValues[i] = row.getValue(input[i]);
		}

		boolean valid = true;
		for (int i = 1; i < rowValues.length; i++) {
			Object value1 = rowValues[i - 1];
			Object value2 = rowValues[i];
			if (!ObjectUtils.equals(value1, value2)) {
				valid = false;
				break;
			}
		}

		if (!valid) {
			invalidRows.add(rowValues);
		}
	}
	
	@Result
	public ValidationResult getResult() {
		return new ValidationResult(this, input, invalidRows);
	}
}
