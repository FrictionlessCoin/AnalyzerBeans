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
package org.eobjects.analyzer.beans.datastructures;

import java.util.List;

import javax.inject.Inject;

import org.eobjects.analyzer.beans.api.Categorized;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.beans.api.OutputRowCollector;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.beans.api.TransformerBean;
import org.eobjects.analyzer.beans.categories.DataStructuresCategory;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;

/**
 * Transformer for extracting elements from lists.
 * 
 * @author Kasper Sørensen
 * @author Shekhar Gulati
 * @author Saurabh Gupta
 */
@TransformerBean("Extract elements from list")
@Categorized(DataStructuresCategory.class)
public class ExtractFromListTransformer implements Transformer<Object> {

	@Inject
	@Configured
	InputColumn<List<?>> listColumn;

	@Inject
	@Configured
	Class<?> elementType;

	@Inject
	@Configured
	boolean verifyTypes = false;

	@Inject
	@Provided
	OutputRowCollector outputRowCollector;

	@Override
	public OutputColumns getOutputColumns() {
		String[] columnNames = new String[] { listColumn.getName() + " (element)" };
		Class<?>[] columnTypes = new Class[] { elementType };
		return new OutputColumns(columnNames, columnTypes);
	}

	@Override
	public Object[] transform(InputRow row) {
		List<?> list = row.getValue(listColumn);
		if (list == null || list.isEmpty()) {
			return new Object[1];
		}

		for (Object value : list) {
			if (verifyTypes) {
				value = elementType.cast(value);
			}
			outputRowCollector.putValues(value);
		}

		return null;
	}

}
