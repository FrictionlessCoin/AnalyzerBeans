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

import java.util.Arrays;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.api.OutputColumns;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.MockInputColumn;
import org.eobjects.analyzer.data.MockInputRow;

public class DatastoreLookupTransformerTest extends TestCase {

	public void testScenario() throws Exception {
		DatastoreLookupTransformer trans = new DatastoreLookupTransformer();
		trans.datastore = new CsvDatastore("my ds", "src/test/resources/employees.csv");
		trans.outputColumns = new String[] { "name" };
		trans.conditionColumns = new String[] { "email" };
		InputColumn<String> col1 = new MockInputColumn<String>("my email col", String.class);
		trans.conditionValues = new InputColumn[] { col1  };
		
		OutputColumns outputColumns = trans.getOutputColumns();
		assertEquals("OutputColumns[email (lookup)]", outputColumns.toString());
		assertEquals(String.class, outputColumns.getColumnType(0));
		
		trans.init();
		
		assertEquals("[Jane Doe]", Arrays.toString(trans.transform(new MockInputRow().put(col1, "jane.doe@company.com"))));
		assertEquals("[null]", Arrays.toString(trans.transform(new MockInputRow().put(col1, "foo bar"))));
		
		trans.close();
	}
}
