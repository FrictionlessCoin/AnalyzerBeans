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
package org.eobjects.analyzer.beans.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eobjects.analyzer.beans.api.Alias;
import org.eobjects.analyzer.beans.api.Close;
import org.eobjects.analyzer.beans.api.ColumnProperty;
import org.eobjects.analyzer.beans.api.Concurrent;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Description;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.beans.api.OutputRowCollector;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.beans.api.SchemaProperty;
import org.eobjects.analyzer.beans.api.TableProperty;
import org.eobjects.analyzer.beans.api.Transformer;
import org.eobjects.analyzer.beans.api.TransformerBean;
import org.eobjects.analyzer.beans.api.Validate;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreConnection;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.util.CollectionUtils2;
import org.eobjects.metamodel.data.DataSet;
import org.eobjects.metamodel.query.CompiledQuery;
import org.eobjects.metamodel.query.OperatorType;
import org.eobjects.metamodel.query.Query;
import org.eobjects.metamodel.query.QueryParameter;
import org.eobjects.metamodel.schema.Column;
import org.eobjects.metamodel.util.HasName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A transformer that can do a lookup (like a left join) based on a set of
 * columns in any datastore.
 * 
 * @author Kasper Sørensen
 */
@TransformerBean("Table lookup")
@Alias("Datastore lookup")
@Description("Perform a lookup based on a table in any of your registered datastore (like a LEFT join).")
@Concurrent(true)
public class TableLookupTransformer implements Transformer<Object> {

    private static final Logger logger = LoggerFactory.getLogger(TableLookupTransformer.class);

    public static enum JoinSemantic implements HasName {
        LEFT("Left join"), INNER("Inner join"), INNER_MIN_ONE("Inner join (min. 1 output record)");

        private final String _name;

        private JoinSemantic(String name) {
            _name = name;
        }

        @Override
        public String getName() {
            return _name;
        }

        public boolean isCacheable() {
            // inner joined result sets are not cached since their size exceeds
            // the cache capabilities.
            return this == LEFT;
        }
    }

    @Inject
    @Configured
    Datastore datastore;

    @Inject
    @Configured(required = false)
    InputColumn<?>[] conditionValues;

    @Inject
    @Configured(required = false)
    @ColumnProperty
    String[] conditionColumns;

    @Inject
    @Configured
    @ColumnProperty
    String[] outputColumns;

    @Inject
    @Configured
    @Alias("Schema")
    @SchemaProperty
    String schemaName;

    @Inject
    @Configured
    @Alias("Table")
    @TableProperty
    String tableName;

    @Inject
    @Configured
    @Description("Use a client-side cache to avoid looking up multiple times with same inputs.")
    boolean cacheLookups = true;

    @Inject
    @Configured
    @Description("Which kind of semantic to apply to the lookup, compared to a SQL JOIN.")
    JoinSemantic joinSemantic = JoinSemantic.LEFT;

    @Inject
    @Provided
    OutputRowCollector outputRowCollector;

    private final Map<List<Object>, Object[]> cache = Collections.synchronizedMap(CollectionUtils2
            .<List<Object>, Object[]> createCacheMap());
    private Column[] queryOutputColumns;
    private Column[] queryConditionColumns;
    private DatastoreConnection datastoreConnection;
    private CompiledQuery lookupQuery;

    private void resetCachedColumns() {
        queryOutputColumns = null;
        queryConditionColumns = null;
    }

    private Column[] getQueryConditionColumns() {
        if (queryConditionColumns == null) {
            if (isCarthesianProductMode()) {
                queryConditionColumns = new Column[0];
            } else {
                final DatastoreConnection con = datastore.openConnection();
                try {
                    queryConditionColumns = con.getSchemaNavigator().convertToColumns(schemaName, tableName,
                            conditionColumns);
                } finally {
                    con.close();
                }
            }
        }
        return queryConditionColumns;
    }

    /**
     * Gets the output columns of the lookup query
     * 
     * @param checkNames
     *            whether to check/validate/adjust the names of these columns
     * @return
     */
    private Column[] getQueryOutputColumns(boolean checkNames) {
        if (queryOutputColumns == null) {
            final DatastoreConnection con = datastore.openConnection();
            try {
                queryOutputColumns = con.getSchemaNavigator().convertToColumns(schemaName, tableName, outputColumns);
            } finally {
                con.close();
            }
        } else if (checkNames) {
            if (!isQueryOutputColumnsUpdated()) {
                queryOutputColumns = null;
                return getQueryOutputColumns(false);
            }
        }
        return queryOutputColumns;
    }

    /**
     * Checks the validity of the current (cached) output columns array.
     * 
     * @return true if the current columns are valid
     */
    private boolean isQueryOutputColumnsUpdated() {
        if (queryOutputColumns.length != outputColumns.length) {
            return false;
        }
        for (int i = 0; i < queryOutputColumns.length; i++) {
            Column outputColumn = queryOutputColumns[i];
            String expectedName = outputColumns[i];
            if (!expectedName.equals(outputColumn.getName())) {
                return false;
            }
            if (tableName != null && !tableName.equals(outputColumn.getTable().getName())) {
                return false;
            }
        }

        return true;
    }

    @Initialize
    public void init() {
        datastoreConnection = datastore.openConnection();
        resetCachedColumns();
        cache.clear();
        compileLookupQuery();
    }

    private void compileLookupQuery() {
        try {
            Column[] queryOutputColumns = getQueryOutputColumns(false);
            Query query = new Query().from(queryOutputColumns[0].getTable()).select(queryOutputColumns);

            if (!isCarthesianProductMode()) {
                Column[] queryConditionColumns = getQueryConditionColumns();
                for (int i = 0; i < queryConditionColumns.length; i++) {
                    query = query.where(queryConditionColumns[i], OperatorType.EQUALS_TO, new QueryParameter());
                }
            }
            
            if (joinSemantic == JoinSemantic.LEFT) {
                query = query.setMaxRows(1);
            }

            lookupQuery = datastoreConnection.getDataContext().compileQuery(query);

        } catch (RuntimeException e) {
            logger.error("Error occurred while compiling lookup query", e);
            throw e;
        }
    }
    
    private boolean isCarthesianProductMode() {
        return (conditionColumns == null || conditionColumns.length == 0) && (conditionValues == null || conditionValues.length == 0);
    }

    @Validate
    public void validate() {
        if (isCarthesianProductMode()) {
            // carthesian product mode
            return;
        }
        final Column[] queryConditionColumns = getQueryConditionColumns();
        final List<String> columnsNotFound = new ArrayList<String>();
        for (int i = 0; i < queryConditionColumns.length; i++) {
            if (queryConditionColumns[i] == null) {
                columnsNotFound.add(conditionColumns[i]);
            }
        }

        if (!columnsNotFound.isEmpty()) {
            throw new IllegalArgumentException("Could not find column(s): " + columnsNotFound);
        }
    }

    @Override
    public OutputColumns getOutputColumns() {
        Column[] queryOutputColumns = getQueryOutputColumns(true);
        String[] names = new String[queryOutputColumns.length];
        Class<?>[] types = new Class[queryOutputColumns.length];
        for (int i = 0; i < queryOutputColumns.length; i++) {
            Column column = queryOutputColumns[i];
            if (column == null) {
                throw new IllegalArgumentException("Could not find column: " + outputColumns[i]);
            }
            names[i] = column.getName() + " (lookup)";
            types[i] = column.getType().getJavaEquivalentClass();
        }
        return new OutputColumns(names, types);
    }

    @Override
    public Object[] transform(InputRow inputRow) {
        final List<Object> queryInput;
        
        if (isCarthesianProductMode()) {
            queryInput = Collections.emptyList();
        } else {
            queryInput = new ArrayList<Object>(conditionValues.length);
            for (InputColumn<?> inputColumn : conditionValues) {
                Object value = inputRow.getValue(inputColumn);
                queryInput.add(value);
            }
        }

        logger.info("Looking up based on condition values: {}", queryInput);

        Object[] result;
        if (cacheLookups && joinSemantic.isCacheable()) {
            result = cache.get(queryInput);
            if (result == null) {
                result = performQuery(queryInput);
                cache.put(queryInput, result);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Returning cached lookup result: {}", Arrays.toString(result));
                }
            }
        } else {
            result = performQuery(queryInput);
        }

        return result;
    }

    private Object[] performQuery(List<Object> queryInput) {
        try {
            final Column[] queryConditionColumns = getQueryConditionColumns();

            final Object[] parameterValues = new Object[queryConditionColumns.length];
            for (int i = 0; i < queryConditionColumns.length; i++) {
                parameterValues[i] = queryInput.get(i);
            }

            final DataSet dataSet = datastoreConnection.getDataContext().executeQuery(lookupQuery, parameterValues);
            try {
                return handleDataSet(dataSet);
            } finally {
                dataSet.close();
            }
        } catch (RuntimeException e) {
            logger.error("Error occurred while looking up based on conditions: " + queryInput, e);
            throw e;
        }
    }

    private Object[] handleDataSet(DataSet dataSet) {
        if (!dataSet.next()) {
            logger.warn("Result of lookup: None!");
            switch (joinSemantic) {
            case LEFT:
            case INNER_MIN_ONE:
                return new Object[outputColumns.length];
            default:
                return null;
            }
        }

        do {
            final Object[] result = dataSet.getRow().getValues();
            if (logger.isInfoEnabled()) {
                logger.info("Result of lookup: " + Arrays.toString(result));
            }
            switch (joinSemantic) {
            case LEFT:
                return result;
            default:
                outputRowCollector.putValues(result);
            }

        } while (dataSet.next());

        return null;
    }

    @Close
    public void close() {
        lookupQuery.close();
        datastoreConnection.close();
        cache.clear();
        queryOutputColumns = null;
        queryConditionColumns = null;
    }
}
