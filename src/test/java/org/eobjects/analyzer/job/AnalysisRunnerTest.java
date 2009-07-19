package org.eobjects.analyzer.job;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.eobjects.analyzer.beans.mock.ExploringBeanMock;
import org.eobjects.analyzer.beans.mock.RowProcessingBeanMock;
import org.eobjects.analyzer.lifecycle.ProvidedList;
import org.eobjects.analyzer.result.AnalysisResult;
import org.eobjects.analyzer.result.AnalyzerBeanResult;

import com.sleepycat.collections.StoredMap;

import dk.eobjects.metamodel.DataContext;
import dk.eobjects.metamodel.DataContextFactory;
import dk.eobjects.metamodel.MetaModelTestCase;
import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.Table;

public class AnalysisRunnerTest extends MetaModelTestCase {

	private static final Integer CONFIGURED2_VALUE = 1337;
	private static final String CONFIGURED1_VALUE = "configString1";
	private DataContext dc;
	private Table employeesTable;
	private Table customersTable;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (dc == null) {
			Connection connection = getTestDbConnection();
			dc = DataContextFactory.createJdbcDataContext(connection);
			employeesTable = dc.getDefaultSchema().getTableByName("EMPLOYEES");
			customersTable = dc.getDefaultSchema().getTableByName("CUSTOMERS");
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ExploringBeanMock.clearInstances();
		RowProcessingBeanMock.clearInstances();
	}

	public void testRowProcessingBeans() throws Exception {
		AnalysisJob job = new AnalysisJob(RowProcessingBeanMock.class);
		setReusableMockProperties(job);
		Column[] employeeColumns = employeesTable.getColumns();
		Column[] customerColumns = customersTable.getColumns();
		job.putColumnProperty("Columns", employeeColumns[0],
				employeeColumns[1], customerColumns[0]);

		AnalysisRunner runner = new AnalysisRunner();
		runner.addJob(job);
		runner.run(dc);

		List<RowProcessingBeanMock> beans = RowProcessingBeanMock
				.getInstances();
		assertEquals(2, beans.size());

		RowProcessingBeanMock bean = beans.get(0);
		performReusableMockTests(bean);
		assertEquals(23, bean.getRowCount());
		assertEquals(23, bean.getRunCount());
		Column[] columns = bean.getColumns();
		assertEquals(2, columns.length);
		assertEquals(employeeColumns[0], columns[0]);
		assertEquals(employeeColumns[1], columns[1]);

		bean = beans.get(1);
		performReusableMockTests(bean);
		assertEquals(122, bean.getRowCount());
		assertEquals(122, bean.getRunCount());
		columns = bean.getColumns();
		assertEquals(1, columns.length);
		assertEquals(customerColumns[0], columns[0]);

		AnalysisResult result = runner.getResult();
		List<AnalyzerBeanResult> results = result.getAnalyzerBeanResults();
		assertEquals(4, results.size());
	}

	private void performReusableMockTests(RowProcessingBeanMock bean) {
		assertTrue(bean.isClose1());
		assertTrue(bean.isClose2());
		assertTrue(bean.isInit1());
		assertTrue(bean.isInit2());
		assertTrue(bean.isResult1());
		assertTrue(bean.isResult2());
		assertEquals(CONFIGURED1_VALUE, bean.getConfigured1());
		assertEquals(CONFIGURED2_VALUE, bean.getConfigured2());
		List<Boolean> providedList = bean.getProvidedList();
		assertNotNull(providedList);
		assertTrue(providedList instanceof ProvidedList<?>);

		Map<String, Long> providedMap = bean.getProvidedMap();
		assertNotNull(providedMap);
		assertTrue(providedMap instanceof StoredMap<?, ?>);
	}

	private void setReusableMockProperties(AnalysisJob job) {
		job.putStringProperty("Configured1", CONFIGURED1_VALUE);
		job.putIntegerProperty("Configured2", CONFIGURED2_VALUE);
	}
}
