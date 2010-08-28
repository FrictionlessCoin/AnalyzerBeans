package org.eobjects.analyzer.util;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Schema;
import dk.eobjects.metamodel.schema.Table;

public final class SchemaNavigator {

	private final DataContext dataContext;

	public SchemaNavigator(DataContext dataContext) {
		this.dataContext = dataContext;
	}

	public Schema convertToSchema(String schemaName) {
		return dataContext.getSchemaByName(schemaName);
	}

	public Schema[] convertToSchemas(String[] schemaNames) {
		Schema[] result = new Schema[schemaNames.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = convertToSchema(schemaNames[i]);
		}
		return result;
	}

	public Table[] convertToTables(String[] tableNames) {
		Table[] result = new Table[tableNames.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = convertToTable(tableNames[i]);
		}
		return result;
	}

	public Table convertToTable(String tableName) {
		return dataContext.getTableByQualifiedLabel(tableName);
	}

	public Column[] convertToColumns(String[] columnNames) {
		Column[] result = new Column[columnNames.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = convertToColumn(columnNames[i]);
		}
		return result;
	}

	public Column convertToColumn(String columnName) {
		Schema schema = null;
		String[] schemaNames = dataContext.getSchemaNames();
		for (String schemaName : schemaNames) {
			if (columnName.startsWith(schemaName)) {
				schema = dataContext.getSchemaByName(schemaName);
				String remainingPath1 = columnName.substring(schemaName.length());
				
				assert remainingPath1.charAt(0) == '.';
				
				remainingPath1 = remainingPath1.substring(1);
				
				Table table = null;
				String[] tableNames = schema.getTableNames();
				for (String tableName : tableNames) {
					if (remainingPath1.startsWith(tableName)) {
						table = schema.getTableByName(tableName);
						String remainingPath2 = remainingPath1.substring(tableName.length());
						
						assert remainingPath2.charAt(0) == '.';
						
						remainingPath2 = remainingPath2.substring(1);
						
						Column column = table.getColumnByName(remainingPath2);
						if (column != null) {
							return column;
						}
					}
				}
			}
		}
		
		return null;
	}
}
