package org.eobjects.analyzer.beans.writers;

import java.io.File;

import junit.framework.TestCase;

import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.DataContextProvider;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.concurrent.MultiThreadedTaskRunner;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.metamodel.UpdateCallback;
import org.eobjects.metamodel.UpdateScript;
import org.eobjects.metamodel.UpdateableDataContext;
import org.eobjects.metamodel.create.TableCreationBuilder;
import org.eobjects.metamodel.data.DataSet;
import org.eobjects.metamodel.schema.Column;
import org.eobjects.metamodel.schema.Table;

public class DatastoreWriterAnalyzerTest extends TestCase {

	public void testMultiThreadedRunNoColumnNames() throws Exception {
		final CsvDatastore datastoreIn = new CsvDatastore("in",
				"src/test/resources/datastorewriter-in.csv");
		final CsvDatastore datastoreOut = new CsvDatastore("out",
				"target/datastorewriter-out.csv");

		// count input lines and get columns
		final Column[] columns;
		final Number countIn;
		{
			DataContextProvider dcp = datastoreIn.getDataContextProvider();
			Table table = dcp.getDataContext().getDefaultSchema().getTables()[0];

			columns = table.getColumns();

			DataSet ds = dcp.getDataContext().query().from(table).selectCount()
					.execute();
			assertTrue(ds.next());
			countIn = (Number) ds.getRow().getValue(0);
			assertFalse(ds.next());
			ds.close();

			dcp.close();
		}

		// create output file
		{
			DataContextProvider dcp = datastoreOut.getDataContextProvider();
			final UpdateableDataContext dc = (UpdateableDataContext) dcp
					.getDataContext();
			dc.executeUpdate(new UpdateScript() {
				@Override
				public void run(UpdateCallback callback) {
					TableCreationBuilder createTableBuilder = callback
							.createTable(dc.getDefaultSchema(), "mytable");
					for (Column column : columns) {
						createTableBuilder = createTableBuilder.withColumn(
								column.getName()).ofType(column.getType());
					}
					createTableBuilder.execute();
				}
			});
			dcp.close();
		}

		// run a "copy lines" job with multithreading
		{
			DatastoreCatalog datastoreCatalog = new DatastoreCatalogImpl(
					datastoreIn);

			AnalyzerBeansConfiguration configuration = new AnalyzerBeansConfigurationImpl()
					.replace(new MultiThreadedTaskRunner(4)).replace(
							datastoreCatalog);

			AnalysisJobBuilder ajb = new AnalysisJobBuilder(configuration);
			ajb.setDatastore(datastoreIn);

			ajb.addSourceColumns(columns);

			AnalyzerJobBuilder<DatastoreWriterAnalyzer> analyzerJobBuilder = ajb
					.addAnalyzer(DatastoreWriterAnalyzer.class);
			analyzerJobBuilder.addInputColumns(ajb.getSourceColumns());
			analyzerJobBuilder.setConfiguredProperty("Datastore", datastoreOut);

			assertTrue(analyzerJobBuilder.isConfigured());

			AnalysisRunner runner = new AnalysisRunnerImpl(configuration);
			AnalysisResultFuture resultFuture = runner.run(ajb.toAnalysisJob());

			assertTrue(resultFuture.isSuccessful());
		}

		// count output file lines
		final Number countOut;
		{
			DataContextProvider dcp = datastoreOut.getDataContextProvider();
			DataSet ds = dcp
					.getDataContext()
					.query()
					.from(dcp.getDataContext().getDefaultSchema().getTables()[0])
					.selectCount().execute();
			assertTrue(ds.next());
			countOut = (Number) ds.getRow().getValue(0);
			assertFalse(ds.next());
			ds.close();
			dcp.close();
		}

		assertEquals(countIn, countOut);

		assertTrue("Could not delete output file",
				new File(datastoreOut.getFilename()).delete());
	}
}
