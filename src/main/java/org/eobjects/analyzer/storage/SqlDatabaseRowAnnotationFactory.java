package org.eobjects.analyzer.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.data.MockInputRow;

public class SqlDatabaseRowAnnotationFactory implements RowAnnotationFactory {

	private final Map<InputColumn<?>, String> _inputColumnNames = new LinkedHashMap<InputColumn<?>, String>();
	private final Map<RowAnnotation, String> _annotationColumnNames = new HashMap<RowAnnotation, String>();
	private final Connection _connection;
	private final SqlDatabaseStorageProvider _storageProvider;
	private final String _tableName;
	private final AtomicInteger _nextColumnIndex = new AtomicInteger(1);
	private final PreparedStatement _containsRowStatement;

	public SqlDatabaseRowAnnotationFactory(Connection connection, String tableName,
			SqlDatabaseStorageProvider storageProvider) {
		_connection = connection;
		_storageProvider = storageProvider;
		_tableName = tableName;
		String intType = _storageProvider.getSqlType(Integer.class);
		performUpdate("CREATE TABLE " + tableName + " (id " + intType + " PRIMARY KEY, distinct_count " + intType + ")");

		try {
			_containsRowStatement = _connection.prepareStatement("SELECT COUNT(*) FROM " + _tableName + " WHERE id = ?");
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		performUpdate("DROP TABLE " + _tableName);
	}

	private void performUpdate(String sql) {
		SqlDatabaseUtils.performUpdate(_connection, sql);
	}

	@Override
	public RowAnnotation createAnnotation() {
		return new RowAnnotationImpl();
	}

	private boolean containsRow(InputRow row) {
		ResultSet rs = null;
		try {
			boolean contains;
			_containsRowStatement.setInt(1, row.getId());
			rs = _containsRowStatement.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				if (count == 0) {
					contains = false;
				} else if (count == 1) {
					contains = true;
				} else {
					throw new IllegalStateException(count + " rows with id=" + row.getId() + " exists in database!");
				}
			} else {
				contains = false;
			}

			return contains;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		} finally {
			SqlDatabaseUtils.safeClose(rs, null);
		}
	}

	@Override
	public void annotate(InputRow row, int distinctCount, RowAnnotation annotation) {
		RowAnnotationImpl a = (RowAnnotationImpl) annotation;

		List<InputColumn<?>> inputColumns = row.getInputColumns();
		List<String> columnNames = new ArrayList<String>(inputColumns.size());
		List<Object> values = new ArrayList<Object>(inputColumns.size());
		for (InputColumn<?> inputColumn : inputColumns) {
			String columnName = getColumnName(inputColumn, true);
			columnNames.add(columnName);
			Object value = row.getValue(inputColumn);
			values.add(value);
		}

		String annotationColumnName = getColumnName(annotation, true);

		if (containsRow(row)) {
			PreparedStatement st = null;
			ResultSet rs = null;

			boolean annotated;
			try {
				st = _connection.prepareStatement("SELECT " + annotationColumnName + " FROM " + _tableName + " WHERE id=?");
				st.setInt(1, row.getId());
				rs = st.executeQuery();
				if (!rs.next()) {
					throw new IllegalStateException("No rows in annotation status ResultSet");
				}
				annotated = rs.getBoolean(1);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			} finally {
				SqlDatabaseUtils.safeClose(rs, st);
			}

			if (!annotated) {
				try {
					st = _connection
							.prepareStatement("UPDATE " + _tableName + " SET " + annotationColumnName + "=TRUE WHERE id=?");
					st.setInt(1, row.getId());
					st.executeUpdate();
					a.incrementRowCount(distinctCount);
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				} finally {
					SqlDatabaseUtils.safeClose(rs, st);
				}
			}

		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO ");
			sb.append(_tableName);
			sb.append(" (id,distinct_count");
			sb.append(',');
			sb.append(annotationColumnName);
			for (String columnName : columnNames) {
				sb.append(',');
				sb.append(columnName);
			}
			sb.append(") VALUES (?,?,?");
			for (int i = 0; i < values.size(); i++) {
				sb.append(",?");
			}
			sb.append(")");

			PreparedStatement st = null;
			try {
				st = _connection.prepareStatement(sb.toString());
				st.setInt(1, row.getId());
				st.setInt(2, distinctCount);
				st.setBoolean(3, true);
				for (int i = 0; i < values.size(); i++) {
					st.setObject(i + 4, values.get(i));
				}
				st.executeUpdate();
				a.incrementRowCount(distinctCount);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			} finally {
				SqlDatabaseUtils.safeClose(null, st);
			}
		}
	}

	private String getColumnName(RowAnnotation annotation, boolean createIfNonExisting) {
		String columnName = _annotationColumnNames.get(annotation);
		if (columnName == null) {
			if (createIfNonExisting) {
				int index = _nextColumnIndex.getAndIncrement();
				columnName = "col" + index;
				performUpdate("ALTER TABLE " + _tableName + " ADD COLUMN " + columnName + " "
						+ _storageProvider.getSqlType(Boolean.class) + " DEFAULT FALSE");
				_annotationColumnNames.put(annotation, columnName);
			}
		}
		return columnName;
	}

	private String getColumnName(InputColumn<?> inputColumn, boolean createIfNonExisting) {
		String columnName = _inputColumnNames.get(inputColumn);
		if (columnName == null) {
			if (createIfNonExisting) {
				int index = _nextColumnIndex.getAndIncrement();
				columnName = "col" + index;
				Class<?> javaType = inputColumn.getDataType();

				performUpdate("ALTER TABLE " + _tableName + " ADD COLUMN " + columnName + " "
						+ _storageProvider.getSqlType(javaType));
				_inputColumnNames.put(inputColumn, columnName);
			}
		}
		return columnName;
	}

	@Override
	public void reset(RowAnnotation annotation) {
		String columnName = getColumnName(annotation, false);
		if (columnName != null) {
			performUpdate("UPDATE " + _tableName + " SET " + columnName + " = FALSE");
		}
	}

	@Override
	public InputRow[] getRows(RowAnnotation annotation) {
		String annotationColumnName = getColumnName(annotation, false);
		if (annotationColumnName == null) {
			return new InputRow[0];
		}
		ResultSet rs = null;
		Statement st = null;
		try {
			st = _connection.createStatement();

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT id");
			ArrayList<InputColumn<?>> inputColumns = new ArrayList<InputColumn<?>>(_inputColumnNames.keySet());
			for (InputColumn<?> inputColumn : inputColumns) {
				sb.append(',');
				String columnName = _inputColumnNames.get(inputColumn);
				sb.append(columnName);
			}
			sb.append(" FROM ");
			sb.append(_tableName);
			sb.append(" WHERE ");
			sb.append(annotationColumnName);
			sb.append(" = TRUE");

			rs = st.executeQuery(sb.toString());
			List<InputRow> rows = new ArrayList<InputRow>();
			while (rs.next()) {
				int id = rs.getInt(1);
				MockInputRow row = new MockInputRow(id);
				int colIndex = 2;
				for (InputColumn<?> inputColumn : inputColumns) {
					Object value = rs.getObject(colIndex);
					row.put(inputColumn, value);
					colIndex++;
				}
				rows.add(row);
			}
			return rows.toArray(new InputRow[rows.size()]);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		} finally {
			SqlDatabaseUtils.safeClose(rs, st);
		}
	}

}
