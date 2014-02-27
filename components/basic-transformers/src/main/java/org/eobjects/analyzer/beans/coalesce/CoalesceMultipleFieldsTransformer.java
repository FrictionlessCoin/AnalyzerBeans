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
package org.eobjects.analyzer.beans.coalesce;

import java.util.List;

import org.eobjects.analyzer.beans.api.Categorized;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.beans.api.TransformerBean;
import org.eobjects.analyzer.beans.categories.FilterCategory;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;

@TransformerBean("Coalesce multiple fields")
@Description("Returns the first non-null values of multiple fields and groups of fields. Use it to identify the most "
        + "accurate or most recent observation, if multiple entries have been recorded in separate columns.")
@Categorized({ FilterCategory.class })
public class CoalesceMultipleFieldsTransformer implements Transformer<Object> {
    
    @Configured
    InputColumn<?>[] _input;

    @Configured
    CoalesceUnit[] units;

    @Configured
    @Description("Consider empty strings (\"\") as null also?")
    boolean considerEmptyStringAsNull = true;

    public CoalesceMultipleFieldsTransformer() {
    }

    public CoalesceMultipleFieldsTransformer(CoalesceUnit... units) {
        this();
        this.units = units;
    }

    @Override
    public OutputColumns getOutputColumns() {
        OutputColumns outputColumns = new OutputColumns(units.length);
        for (int i = 0; i < units.length; i++) {
            CoalesceUnit unit = units[i];
            Class<?> dataType = unit.getOutputDataType(_input);
            outputColumns.setColumnType(i, dataType);
        }
        return outputColumns;
    }

    @Override
    public Object[] transform(InputRow inputRow) {
        Object[] result = new Object[units.length];
        for (int i = 0; i < units.length; i++) {
            InputColumn<?>[] inputColumns = units[i].getInputColumns(_input);
            List<Object> values = inputRow.getValues(inputColumns);
            Object value = coalesce(values);
            result[i] = value;
        }
        return result;
    }

    private Object coalesce(List<Object> values) {
        for (Object value : values) {
            if (value != null) {
                if (considerEmptyStringAsNull) {
                    if (!"".equals(value)) {
                        return value;
                    }
                } else {
                    return value;
                }
            }
        }
        return null;
    }

}
