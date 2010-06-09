package org.eobjects.analyzer.job;

import javax.sql.DataSource;

import org.eobjects.analyzer.util.SchemaNavigator;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.DataContextFactory;

public class DataSourceDataContextProvider implements DataContextProvider {

	private DataContext dataContext;
	private SchemaNavigator schemaNavigator;

	// TODO: Lazy load datacontext based on JNDI name?

	public DataSourceDataContextProvider(DataSource ds) {
		this.dataContext = DataContextFactory.createJdbcDataContext(ds);
		this.schemaNavigator = new SchemaNavigator(dataContext);
	}

	@Override
	public DataContext getDataContext() {
		return dataContext;
	}

	@Override
	public SchemaNavigator getSchemaNavigator() {
		return schemaNavigator;
	}

}
