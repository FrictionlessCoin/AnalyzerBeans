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
package org.eobjects.analyzer.beans.valuedist;

import junit.framework.TestCase;

import org.eobjects.analyzer.data.MetaModelInputColumn;
import org.eobjects.analyzer.data.MockInputColumn;
import org.eobjects.analyzer.descriptors.AnalyzerBeanDescriptor;
import org.eobjects.analyzer.descriptors.AnnotationBasedAnalyzerBeanDescriptor;
import org.eobjects.analyzer.result.ValueDistributionResult;
import org.eobjects.metamodel.schema.MutableColumn;

public class ValueDistributionAnalyzerTest extends TestCase {

	public void testDescriptor() throws Exception {
		AnalyzerBeanDescriptor<?> desc = AnnotationBasedAnalyzerBeanDescriptor.create(ValueDistributionAnalyzer.class);
		assertEquals(true, desc.isRowProcessingAnalyzer());
		assertEquals(false, desc.isExploringAnalyzer());
		assertEquals(0, desc.getInitializeMethods().size());
		assertEquals(5, desc.getConfiguredProperties().size());
		assertEquals(1, desc.getProvidedProperties().size());
		assertEquals("Value distribution", desc.getDisplayName());
	}

	public void testGetNullAndUniqueCount() throws Exception {
		ValueDistributionAnalyzer vd = new ValueDistributionAnalyzer(new MetaModelInputColumn(new MutableColumn("col")),
				true, null, null);

		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getUniqueCount());
		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getNullCount());

		vd.runInternal("hello", 1);
		assertEquals(1, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		vd.runInternal("world", 1);
		assertEquals(2, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		vd.runInternal("foobar", 2);
		assertEquals(2, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		vd.runInternal("world", 1);
		assertEquals(1, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		vd.runInternal("hello", 3);
		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		vd.runInternal(null, 1);
		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getUniqueCount());
		assertEquals(1, vd.getResult().getSingleValueDistributionResult().getNullCount());

		vd.runInternal(null, 3);
		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getUniqueCount());
		assertEquals(4, vd.getResult().getSingleValueDistributionResult().getNullCount());
	}

	public void testGetValueDistribution() throws Exception {
		ValueDistributionAnalyzer vd = new ValueDistributionAnalyzer(new MetaModelInputColumn(new MutableColumn("col")),
				true, null, null);

		vd.runInternal("hello", 1);
		vd.runInternal("hello", 1);
		vd.runInternal("world", 3);

		ValueCountList topValues = vd.getResult().getSingleValueDistributionResult().getTopValues();
		assertEquals(2, topValues.getActualSize());
		assertEquals("[world->3]", topValues.getValueCounts().get(0).toString());
		assertEquals("[hello->2]", topValues.getValueCounts().get(1).toString());

		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getNullCount());
		assertEquals(0, vd.getResult().getSingleValueDistributionResult().getUniqueCount());

		String[] resultLines = vd.getResult().toString().split("\n");
		assertEquals(6, resultLines.length);
		assertEquals("Value distribution for column: col", resultLines[0]);
		assertEquals("Top values:", resultLines[1]);
		assertEquals(" - world: 3", resultLines[2]);
		assertEquals(" - hello: 2", resultLines[3]);
		assertEquals("Null count: 0", resultLines[4]);
		assertEquals("Unique values: 0", resultLines[5]);
	}

	public void testGroupedRun() throws Exception {
		ValueDistributionAnalyzer vd = new ValueDistributionAnalyzer(new MockInputColumn<String>("foo", String.class),
				new MockInputColumn<String>("bar", String.class), true, null, null);

		vd.runInternal("Copenhagen N", "2200", 3);
		vd.runInternal("Copenhagen E", "2100", 2);
		vd.runInternal("Copenhagen", "1732", 4);
		vd.runInternal("Coppenhagen", "1732", 3);
		
		ValueDistributionResult result = vd.getResult();
		assertTrue(result.isGroupingEnabled());
		
		String resultString = result.toString();
		System.out.println(resultString);
		String[] resultLines = resultString.split("\n");
		assertEquals(20, resultLines.length);
		
		assertEquals("Value distribution for column: foo", resultLines[0]);
		assertEquals("", resultLines[1]);
		assertEquals("Group: 1732", resultLines[2]);
		assertEquals("Top values:", resultLines[3]);
		assertEquals(" - Copenhagen: 4", resultLines[4]);
		assertEquals(" - Coppenhagen: 3", resultLines[5]);
		assertEquals("Null count: 0", resultLines[6]);
		assertEquals("Unique values: 0", resultLines[7]);
		assertEquals("", resultLines[8]);
		assertEquals("Group: 2100", resultLines[9]);
		assertEquals("Top values:", resultLines[10]);
		assertEquals(" - Copenhagen E: 2", resultLines[11]);
		assertEquals("Null count: 0", resultLines[12]);
		assertEquals("Unique values: 0", resultLines[13]);
		assertEquals("", resultLines[14]);
		assertEquals("Group: 2200", resultLines[15]);
		assertEquals("Top values:", resultLines[16]);
		assertEquals(" - Copenhagen N: 3", resultLines[17]);
		assertEquals("Null count: 0", resultLines[18]);
		assertEquals("Unique values: 0", resultLines[19]);
	}
}
