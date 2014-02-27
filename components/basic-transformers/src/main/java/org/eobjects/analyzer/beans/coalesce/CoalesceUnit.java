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

import org.eobjects.analyzer.beans.api.Convertable;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.util.ReflectionUtils;

/**
 * Represents a set of columns to be coalesced.
 */
@Convertable(CoalesceUnitConverter.class)
public class CoalesceUnit {

    private final String[] _inputColumnNames;

    // transient cached view of columns
    private transient InputColumn<?>[] _inputColumns;

    public CoalesceUnit(String... columnNames) {
        _inputColumnNames = columnNames;
    }

    public CoalesceUnit(InputColumn<?>... inputColumns) {
        if (inputColumns == null || inputColumns.length == 0) {
            throw new IllegalArgumentException("InputColumns cannot be null or empty");
        }
        _inputColumns = inputColumns;
        _inputColumnNames = new String[inputColumns.length];
        for (int i = 0; i < _inputColumnNames.length; i++) {
            _inputColumnNames[i] = inputColumns[i].getName();
        }
    }

    public String[] getInputColumnNames() {
        return _inputColumnNames;
    }

    public InputColumn<?>[] getInputColumns(InputColumn<?>[] allInputColumns) {
        if (_inputColumns == null) {
            _inputColumns = new InputColumn[_inputColumnNames.length];
            for (int i = 0; i < _inputColumnNames.length; i++) {
                final String name = _inputColumnNames[i];
                for (int j = 0; j < allInputColumns.length; j++) {
                    InputColumn<?> inputColumn = allInputColumns[j];
                    if (name.equals(inputColumn.getName())) {
                        _inputColumns[i] = inputColumn;
                        break;
                    }
                }
                if (_inputColumns[i] == null) {
                    throw new IllegalStateException("Column not found: " + name);
                }
            }
        }
        return _inputColumns;
    }

    public Class<?> getOutputDataType(InputColumn<?>[] allInputColumns) {
        Class<?> candidate = null;
        for (final InputColumn<?> inputColumn : getInputColumns(allInputColumns)) {
            final Class<?> dataType = inputColumn.getDataType();
            if (candidate == null) {
                candidate = dataType;
            } else if (candidate == Object.class) {
                return candidate;
            } else {
                if (candidate != dataType) {
                    if (ReflectionUtils.is(dataType, candidate)) {
                        // keep the current candidate
                    } else if (ReflectionUtils.is(candidate, dataType)) {
                        candidate = dataType;
                    } else {
                        return Object.class;
                    }
                }
            }
        }

        if (candidate == null) {
            return Object.class;
        }

        return candidate;
    }
}
