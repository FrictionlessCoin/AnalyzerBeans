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
package org.eobjects.analyzer.beans;

import java.util.List;

import org.eobjects.analyzer.result.ColumnComparisonResult;
import org.eobjects.analyzer.result.ColumnDifference;

import dk.eobjects.metamodel.schema.Column;
import dk.eobjects.metamodel.schema.ColumnType;
import dk.eobjects.metamodel.schema.MutableColumn;
import junit.framework.TestCase;

public class CompareColumnsAnalyzerTest extends TestCase {

	public void testNoDiffs() throws Exception {
		Column column1 = new MutableColumn("column", ColumnType.VARCHAR, null, 4, true);

		CompareColumnsAnalyzer analyzer = new CompareColumnsAnalyzer(column1,
				column1);
		analyzer.run(null);
		assertTrue(analyzer.getResult().isColumnsEqual());

		Column column2 = new MutableColumn("column", ColumnType.VARCHAR, null, 4, true);

		analyzer = new CompareColumnsAnalyzer(column1, column2);
		analyzer.run(null);
		assertTrue(analyzer.getResult().isColumnsEqual());
	}

	public void testSimpleDiffs() throws Exception {
		Column column1 = new MutableColumn("column1", ColumnType.INTEGER, null, 3,
				false);
		Column column2 = new MutableColumn("column2", ColumnType.VARCHAR, null, 4,
				false);

		CompareColumnsAnalyzer analyzer = new CompareColumnsAnalyzer(column1,
				column2);
		analyzer.run(null);
		ColumnComparisonResult result = analyzer.getResult();
		assertFalse(result.isColumnsEqual());

		List<ColumnDifference<?>> diffs = result.getColumnDifferences();
		assertEquals(3, diffs.size());
		assertEquals(
				"Columns 'column1' and 'column2' differ on 'name': [column1] vs. [column2]",
				diffs.get(0).toString());
		assertEquals(
				"Columns 'column1' and 'column2' differ on 'type': [INTEGER] vs. [VARCHAR]",
				diffs.get(1).toString());
		assertEquals(
				"Columns 'column1' and 'column2' differ on 'column number': [3] vs. [4]",
				diffs.get(2).toString());

		// new column 2
		column2 = new MutableColumn("column1", ColumnType.INTEGER, null, 3, true);
		analyzer = new CompareColumnsAnalyzer(column1, column2);
		analyzer.run(null);
		result = analyzer.getResult();
		assertFalse(result.isColumnsEqual());

		diffs = result.getColumnDifferences();
		assertEquals(1, diffs.size());

		assertEquals(
				"Columns 'column1' and 'column1' differ on 'nullable': [false] vs. [true]",
				diffs.get(0).toString());
	}
}
