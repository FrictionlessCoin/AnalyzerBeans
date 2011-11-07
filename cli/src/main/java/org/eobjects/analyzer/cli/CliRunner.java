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
package org.eobjects.analyzer.cli;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eobjects.analyzer.beans.api.Renderer;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.JaxbConfigurationReader;
import org.eobjects.analyzer.connection.DatastoreConnection;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.data.DataTypeFamily;
import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.BeanDescriptor;
import org.eobjects.analyzer.descriptors.ConfiguredPropertyDescriptor;
import org.eobjects.analyzer.descriptors.ExplorerBeanDescriptor;
import org.eobjects.analyzer.descriptors.FilterBeanDescriptor;
import org.eobjects.analyzer.descriptors.TransformerBeanDescriptor;
import org.eobjects.analyzer.job.ComponentJob;
import org.eobjects.analyzer.job.JaxbJobReader;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.renderer.RendererFactory;
import org.eobjects.analyzer.result.renderer.TextRenderingFormat;
import org.eobjects.metamodel.DataContext;
import org.eobjects.metamodel.schema.Schema;
import org.eobjects.metamodel.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the execution logic to run a job from the command line.
 * 
 * @author Kasper Sørensen
 */
public final class CliRunner {

	private final static Logger logger = LoggerFactory.getLogger(CliRunner.class);

	private final CliArguments _arguments;
	private final PrintWriter _out;

	public CliRunner(CliArguments arguments, PrintWriter out) {
		_arguments = arguments;
		_out = out;
	}

	public void run() throws Throwable {
		run(new JaxbConfigurationReader().create(_arguments.getConfigurationFile()));
	}

	public void run(AnalyzerBeansConfiguration configuration) throws Throwable {
		File jobFile = _arguments.getJobFile();
		CliListType listType = _arguments.getListType();
		try {
			if (jobFile != null) {
				runJob(configuration);
			} else if (listType != null) {
				switch (listType) {
				case ANALYZERS:
					printAnalyzers(configuration);
					break;
				case TRANSFORMERS:
					printTransformers(configuration);
					break;
				case FILTERS:
					printFilters(configuration);
					break;
				case EXPLORERS:
					printExplorers(configuration);
					break;
				case DATASTORES:
					printDatastores(configuration);
					break;
				case SCHEMAS:
					printSchemas(configuration);
					break;
				case TABLES:
					printTables(configuration);
					break;
				case COLUMNS:
					printColumns(configuration);
					break;
				default:
					throw new IllegalArgumentException("Unknown list type: " + listType);
				}
			} else {
				throw new IllegalArgumentException(
						"Neither --job-file nor --list-type is specified. Try running with -usage to see usage help.");
			}
		} catch (Exception e) {
			logger.error("Exception thrown in {}", e, this);
			System.err.println("Error:");
			e.printStackTrace(System.err);
		} finally {
			configuration.getTaskRunner().shutdown();
		}
	}

	private void printColumns(AnalyzerBeansConfiguration configuration) {
		String datastoreName = _arguments.getDatastoreName();
		String tableName = _arguments.getTableName();
		String schemaName = _arguments.getSchemaName();

		if (datastoreName == null) {
			System.err.println("You need to specify the datastore name!");
		} else if (tableName == null) {
			System.err.println("You need to specify a table name!");
		} else {
			Datastore ds = configuration.getDatastoreCatalog().getDatastore(datastoreName);
			if (ds == null) {
				System.err.println("No such datastore: " + datastoreName);
			} else {
				DatastoreConnection con = ds.openConnection();
				DataContext dc = con.getDataContext();
				Schema schema;
				if (schemaName == null) {
					schema = dc.getDefaultSchema();
				} else {
					schema = dc.getSchemaByName(schemaName);
				}
				if (schema == null) {
					System.err.println("No such schema: " + schemaName);
				} else {
					Table table = schema.getTableByName(tableName);
					if (table == null) {
						_out.println("No such table: " + tableName);
					} else {
						String[] columnNames = table.getColumnNames();
						_out.println("Columns:");
						_out.println("--------");
						for (String columnName : columnNames) {
							_out.println(columnName);
						}
					}
				}
				con.close();
			}
		}
	}

	private void printTables(AnalyzerBeansConfiguration configuration) {
		String datastoreName = _arguments.getDatastoreName();
		String schemaName = _arguments.getSchemaName();

		if (datastoreName == null) {
			System.err.println("You need to specify the datastore name!");
		} else {
			Datastore ds = configuration.getDatastoreCatalog().getDatastore(datastoreName);
			if (ds == null) {
				System.err.println("No such datastore: " + datastoreName);
			} else {
				DatastoreConnection con = ds.openConnection();
				DataContext dc = con.getDataContext();
				Schema schema;
				if (schemaName == null) {
					schema = dc.getDefaultSchema();
				} else {
					schema = dc.getSchemaByName(schemaName);
				}
				if (schema == null) {
					System.err.println("No such schema: " + schemaName);
				} else {
					String[] tableNames = schema.getTableNames();
					if (tableNames == null || tableNames.length == 0) {
						System.err.println("No tables in schema!");
					} else {
						_out.println("Tables:");
						_out.println("-------");
						for (String tableName : tableNames) {
							_out.println(tableName);
						}
					}
				}
				con.close();
			}
		}
	}

	private void printSchemas(AnalyzerBeansConfiguration configuration) {
		String datastoreName = _arguments.getDatastoreName();

		if (datastoreName == null) {
			System.err.println("You need to specify the datastore name!");
		} else {
			Datastore ds = configuration.getDatastoreCatalog().getDatastore(datastoreName);
			if (ds == null) {
				System.err.println("No such datastore: " + datastoreName);
			} else {
				DatastoreConnection con = ds.openConnection();
				String[] schemaNames = con.getDataContext().getSchemaNames();
				if (schemaNames == null || schemaNames.length == 0) {
					_out.println("No schemas in datastore!");
				} else {
					_out.println("Schemas:");
					_out.println("--------");
					for (String schemaName : schemaNames) {
						_out.println(schemaName);
					}
				}
				con.close();
			}
		}
	}

	private void printDatastores(AnalyzerBeansConfiguration configuration) {
		String[] datastoreNames = configuration.getDatastoreCatalog().getDatastoreNames();
		if (datastoreNames == null || datastoreNames.length == 0) {
			_out.println("No datastores configured!");
		} else {
			_out.println("Datastores:");
			_out.println("-----------");
			for (String datastoreName : datastoreNames) {
				_out.println(datastoreName);
			}
		}
	}

	protected void runJob(AnalyzerBeansConfiguration configuration) throws Throwable {
		final File jobFile = _arguments.getJobFile();
		final InputStream inputStream = new BufferedInputStream(new FileInputStream(jobFile));

		final JaxbJobReader jobReader = new JaxbJobReader(configuration);

		final Map<String, String> variableOverrides = _arguments.getVariableOverrides();
		final AnalysisJobBuilder analysisJobBuilder = jobReader.create(inputStream, variableOverrides);

		final AnalysisRunner runner = new AnalysisRunnerImpl(configuration, new CliProgressAnalysisListener());
		final AnalysisResultFuture resultFuture = runner.run(analysisJobBuilder.toAnalysisJob());

		resultFuture.await();

		if (resultFuture.isSuccessful()) {
			_out.println("SUCCESS!");
			final Set<Entry<ComponentJob, AnalyzerResult>> results = resultFuture.getResultMap().entrySet();

			final RendererFactory rendererFactory = new RendererFactory(configuration.getDescriptorProvider(), null);

			for (Entry<ComponentJob, AnalyzerResult> result : results) {
				final ComponentJob componentJob = result.getKey();
				final AnalyzerResult analyzerResult = result.getValue();
				String name = componentJob.getName();
				if (name == null) {
					name = componentJob.toString();
				}

				_out.println("\nRESULT: " + name);

				Renderer<? super AnalyzerResult, ? extends CharSequence> renderer = rendererFactory.getRenderer(
						analyzerResult, TextRenderingFormat.class);
				CharSequence renderedResult = renderer.render(analyzerResult);

				_out.println(renderedResult);
			}
		} else {
			_out.println("ERROR!");
			_out.println("------");

			List<Throwable> errors = resultFuture.getErrors();
			_out.println(errors.size() + " error(s) occurred while executing the job:");

			for (Throwable throwable : errors) {
				_out.println("------");
				throwable.printStackTrace(_out);
			}
			
			throw errors.get(0);
		}
	}

	protected void printAnalyzers(AnalyzerBeansConfiguration configuration) {
		Collection<AnalyzerBeanDescriptor<?>> descriptors = configuration.getDescriptorProvider()
				.getAnalyzerBeanDescriptors();
		if (descriptors == null || descriptors.isEmpty()) {
			_out.println("No analyzers configured!");
		} else {
			_out.println("Analyzers:");
			_out.println("----------");
			printBeanDescriptors(descriptors);
		}
	}

	private void printTransformers(AnalyzerBeansConfiguration configuration) {
		Collection<TransformerBeanDescriptor<?>> descriptors = configuration.getDescriptorProvider()
				.getTransformerBeanDescriptors();
		if (descriptors == null || descriptors.isEmpty()) {
			_out.println("No transformers configured!");
		} else {
			_out.println("Transformers:");
			_out.println("-------------");
			printBeanDescriptors(descriptors);
		}
	}

	private void printFilters(AnalyzerBeansConfiguration configuration) {
		Collection<FilterBeanDescriptor<?, ?>> descriptors = configuration.getDescriptorProvider()
				.getFilterBeanDescriptors();
		if (descriptors == null || descriptors.isEmpty()) {
			_out.println("No filters configured!");
		} else {
			_out.println("Filters:");
			_out.println("--------");
			printBeanDescriptors(descriptors);
		}
	}

	private void printExplorers(AnalyzerBeansConfiguration configuration) {
		Collection<ExplorerBeanDescriptor<?>> descriptors = configuration.getDescriptorProvider()
				.getExplorerBeanDescriptors();
		if (descriptors == null || descriptors.isEmpty()) {
			_out.println("No explorers configured!");
		} else {
			_out.println("Explorers:");
			_out.println("----------");
			printBeanDescriptors(descriptors);
		}
	}

	protected void printBeanDescriptors(Collection<? extends BeanDescriptor<?>> descriptors) {
		logger.debug("Printing {} descriptors", descriptors.size());
		for (BeanDescriptor<?> descriptor : descriptors) {
			_out.println("name: " + descriptor.getDisplayName());

			Set<ConfiguredPropertyDescriptor> propertiesForInput = descriptor.getConfiguredPropertiesForInput();
			if (propertiesForInput.size() == 1) {
				ConfiguredPropertyDescriptor propertyForInput = propertiesForInput.iterator().next();
				if (propertyForInput != null) {
					if (propertyForInput.isArray()) {
						_out.println(" - Consumes multiple input columns (type: "
								+ propertyForInput.getInputColumnDataTypeFamily() + ")");
					} else {
						_out.println(" - Consumes a single input column (type: "
								+ propertyForInput.getInputColumnDataTypeFamily() + ")");
					}
				}
			} else {
				_out.println(" - Consumes " + propertiesForInput.size() + " named inputs");
				for (ConfiguredPropertyDescriptor propertyForInput : propertiesForInput) {
					if (propertyForInput.isArray()) {
						_out.println("   Input columns: " + propertyForInput.getName() + " (type: "
								+ propertyForInput.getInputColumnDataTypeFamily() + ")");
					} else {
						_out.println("   Input column: " + propertyForInput.getName() + " (type: "
								+ propertyForInput.getInputColumnDataTypeFamily() + ")");
					}
				}
			}

			Set<ConfiguredPropertyDescriptor> properties = descriptor.getConfiguredProperties();
			for (ConfiguredPropertyDescriptor property : properties) {
				if (!property.isInputColumn()) {
					_out.println(" - Property: name=" + property.getName() + ", type="
							+ property.getBaseType().getSimpleName() + ", required=" + property.isRequired());
				}
			}

			if (descriptor instanceof TransformerBeanDescriptor<?>) {
				DataTypeFamily dataTypeFamily = ((TransformerBeanDescriptor<?>) descriptor).getOutputDataTypeFamily();
				_out.println(" - Output type is: " + dataTypeFamily);
			}

			if (descriptor instanceof FilterBeanDescriptor<?, ?>) {
				Set<String> categoryNames = ((FilterBeanDescriptor<?, ?>) descriptor).getOutcomeCategoryNames();
				for (String categoryName : categoryNames) {
					_out.println(" - Outcome category: " + categoryName);
				}
			}
		}
	}
}
