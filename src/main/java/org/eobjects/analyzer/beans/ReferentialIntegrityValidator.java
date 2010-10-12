package org.eobjects.analyzer.beans;

import java.util.ArrayList;
import java.util.List;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.ExploringAnalyzer;
import org.eobjects.analyzer.result.DataSetResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.data.DataSet;
import dk.eobjects.metamodel.data.Row;
import dk.eobjects.metamodel.query.FromItem;
import dk.eobjects.metamodel.query.JoinType;
import dk.eobjects.metamodel.query.Query;
import dk.eobjects.metamodel.query.SelectClause;
import dk.eobjects.metamodel.query.SelectItem;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

/**
 * An analyzer that performs a check on a weak foreign key / reference.
 * 
 * @author Kasper Sørensen
 */
@AnalyzerBean("Referential Integrity validator")
public class ReferentialIntegrityValidator implements
		ExploringAnalyzer<DataSetResult> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private List<Row> invalidRows;

	@Configured("Primary key column")
	Column primaryKeyColumn;

	@Configured("Foreign key column")
	Column foreignKeyColumn;

	@Configured("Accept NULL foreign keys?")
	boolean acceptNullForeignKey;

	/**
	 * Returns a query with the following select items:
	 * <ol>
	 * <li>the primary key value</li>
	 * <li>the foreign key value</li>
	 * <li>the remaining "informational" values from the columns of the foreign
	 * table</li>
	 * </ol>
	 * 
	 * @return
	 */
	public Query createQuery() {
		Table primaryTable = primaryKeyColumn.getTable();
		Table foreignTable = foreignKeyColumn.getTable();
		List<Column> informationalForeignColumns = new ArrayList<Column>();
		for (Column column : foreignTable.getColumns()) {
			if (column != foreignKeyColumn) {
				informationalForeignColumns.add(column);
			}
		}

		Query leftQuery = new Query()
				.select(foreignKeyColumn)
				.select(informationalForeignColumns
						.toArray(new Column[informationalForeignColumns.size()]))
				.from(foreignTable);
		Query rightQuery = new Query().select(primaryKeyColumn).from(
				primaryTable);
		if (logger.isDebugEnabled()) {
			logger.debug("Left query: " + leftQuery);
			logger.debug("Right query: " + rightQuery);
		}

		FromItem leftSide = new FromItem(leftQuery).setAlias("a");
		FromItem rightSide = new FromItem(rightQuery).setAlias("b");

		SelectClause leftSelectClause = leftQuery.getSelectClause();
		SelectClause rightSelectClause = rightQuery.getSelectClause();

		SelectItem leftOn = leftSelectClause.getItem(0);
		SelectItem rightOn = rightSelectClause.getItem(0);

		// Create master query
		Query q = new Query().from(new FromItem(JoinType.LEFT, leftSide,
				rightSide, new SelectItem[] { leftOn },
				new SelectItem[] { rightOn }));
		for (SelectItem si : rightSelectClause.getItems()) {
			q.select(new SelectItem(si, rightSide));
		}
		for (SelectItem si : leftSelectClause.getItems()) {
			q.select(new SelectItem(si, leftSide));
		}
		return q;
	}

	@Override
	public void run(DataContext dc) {
		invalidRows = new ArrayList<Row>();

		Query q = createQuery();

		SelectItem foreignKeySelectItem = q.getSelectClause().getItem(1);
		SelectItem primaryKeySelectItem = q.getSelectClause().getItem(0);

		DataSet dataSet = dc.executeQuery(q);
		while (dataSet.next()) {
			Row row = dataSet.getRow();

			Object foreignKey = row.getValue(foreignKeySelectItem);
			if (foreignKey == null) {
				if (acceptNullForeignKey) {
					if (logger.isInfoEnabled()) {
						logger.info("Accepting row with NULL primary key: "
								+ row);
					}
				} else {
					invalidRows.add(row);
				}
			} else {
				Object primaryKey = row.getValue(primaryKeySelectItem);

				if (primaryKey == null) {
					invalidRows.add(row);
				} else if (!primaryKey.equals(foreignKey)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Unexpected state! Primary and foreign key values are not null and different! PK="
								+ primaryKey + ", FK=" + foreignKey);
					}
					invalidRows.add(row);
				}
			}

		}
		dataSet.close();
	}

	public void setAcceptNullForeignKey(boolean acceptNullForeignKey) {
		this.acceptNullForeignKey = acceptNullForeignKey;
	}

	public void setPrimaryKeyColumn(Column idColumn) {
		this.primaryKeyColumn = idColumn;
	}

	public void setForeignKeyColumn(Column parentIdColumn) {
		this.foreignKeyColumn = parentIdColumn;
	}

	@Override
	public DataSetResult getResult() {
		// TODO: Should probably use validation result?
		return new DataSetResult(invalidRows);
	}
}
