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

import java.io.File;
import java.io.PrintWriter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Defines the Command-line arguments. These are populated by the CLI parser.
 * 
 * @author Kasper Sørensen
 */
public class CliArguments {

	/**
	 * Parses the CLI arguments and creates a CliArguments instance
	 * 
	 * @param args
	 *            the arguments as a string array
	 * @return
	 * @throws CmdLineException
	 */
	public static CliArguments parse(String[] args) throws CmdLineException {
		CliArguments arguments = new CliArguments();
		CmdLineParser parser = new CmdLineParser(arguments);
		parser.parseArgument(args);
		return arguments;
	}

	/**
	 * Prints the usage information for the CLI
	 * @param out
	 */
	public static void printUsage(PrintWriter out) {
		CliArguments arguments = new CliArguments();
		CmdLineParser parser = new CmdLineParser(arguments);
		parser.setUsageWidth(120);
		parser.printUsage(out, null);
	}

	@Option(name = "-conf", aliases = { "-configuration", "--configuration-file" }, usage = "XML file describing the configuration of AnalyzerBeans", required = true)
	private File configurationFile;

	@Option(name = "-job", aliases = { "--job-file" }, usage = "An analysis job XML file to execute")
	private File jobFile;

	@Option(name = "-list", usage = "Used to print a list of various elements available in the configuration")
	private CliListType listType;

	@Option(name = "-ds", aliases = { "-datastore", "--datastore-name" }, usage = "Name of datastore when printing a list of schemas, tables or columns")
	private String datastoreName;

	@Option(name = "-s", aliases = { "-schema", "--schema-name" }, usage = "Name of schema when printing a list of tables or columns")
	private String schemaName;

	@Option(name = "-t", aliases = { "-table", "--table-name" }, usage = "Name of table when printing a list of columns")
	private String tableName;

	private CliArguments() {
		// instantiation only allowed by factory (parse(...)) method.
	}

	public File getConfigurationFile() {
		return configurationFile;
	}

	public void setConfigurationFile(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	public File getJobFile() {
		return jobFile;
	}

	public void setJobFile(File jobFile) {
		this.jobFile = jobFile;
	}

	public CliListType getListType() {
		return listType;
	}

	public void setListType(CliListType listType) {
		this.listType = listType;
	}

	public String getDatastoreName() {
		return datastoreName;
	}

	public void setDatastoreName(String datastoreName) {
		this.datastoreName = datastoreName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
}
