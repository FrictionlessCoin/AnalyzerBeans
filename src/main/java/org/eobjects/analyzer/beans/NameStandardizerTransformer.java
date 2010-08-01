package org.eobjects.analyzer.beans;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eobjects.analyzer.annotations.Configured;
import org.eobjects.analyzer.annotations.Initialize;
import org.eobjects.analyzer.annotations.TransformerBean;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.util.HasGroupLiteral;
import org.eobjects.analyzer.util.NamedPattern;
import org.eobjects.analyzer.util.NamedPatternMatch;

@TransformerBean("Name standardizer")
public class NameStandardizerTransformer implements Transformer<String> {

	public static final String[] DEFAULT_PATTERNS = { "FIRSTNAME LASTNAME",
			"TITULATION. FIRSTNAME LASTNAME", "FIRSTNAME MIDDLENAME LASTNAME",
			"TITULATION. FIRSTNAME MIDDLENAME LASTNAME", "LASTNAME, FIRSTNAME",
			"LASTNAME, FIRSTNAME MIDDLENAME" };

	public static enum NamePart implements HasGroupLiteral {
		FIRSTNAME, LASTNAME, MIDDLENAME, TITULATION;

		@Override
		public String getGroupLiteral() {
			if (this == TITULATION) {
				return "(Mr|Ms|Mrs|Hr|Fru|Frk)";
			}
			return null;
		}
	}

	@Inject
	@Configured
	InputColumn<String> inputColumn;

	@Inject
	@Configured("Name patterns")
	String[] stringPatterns = DEFAULT_PATTERNS;

	private List<NamedPattern<NamePart>> namedPatterns;

	@Initialize
	public void init() {
		if (stringPatterns == null) {
			stringPatterns = new String[0];
		}

		namedPatterns = new ArrayList<NamedPattern<NamePart>>();

		for (String stringPattern : stringPatterns) {
			namedPatterns.add(new NamedPattern<NamePart>(stringPattern,
					NamePart.class));
		}
	}

	@Override
	public OutputColumns getOutputColumns() {
		return new OutputColumns("Firstname", "Lastname", "Middlename",
				"Titulation");
	}

	@Override
	public String[] transform(InputRow inputRow) {
		String value = inputRow.getValue(inputColumn);
		return transform(value);
	}

	public String[] transform(String value) {
		String firstName = null;
		String lastName = null;
		String middleName = null;
		String titulation = null;

		if (value != null) {
			for (NamedPattern<NamePart> namedPattern : namedPatterns) {
				NamedPatternMatch<NamePart> match = namedPattern.match(value);
				if (match != null) {
					firstName = match.get(NamePart.FIRSTNAME);
					lastName = match.get(NamePart.LASTNAME);
					middleName = match.get(NamePart.MIDDLENAME);
					titulation = match.get(NamePart.TITULATION);
					break;
				}
			}
		}
		return new String[] { firstName, lastName, middleName, titulation };
	}

	@SuppressWarnings("unchecked")
	public void setInputColumn(InputColumn<?> inputColumn) {
		this.inputColumn = (InputColumn<String>) inputColumn;
	}

	public void setStringPatterns(String... stringPatterns) {
		this.stringPatterns = stringPatterns;
	}
}
