package org.eobjects.analyzer.data;

import junit.framework.TestCase;
import dk.eobjects.metamodel.schema.ColumnType;
import dk.eobjects.metamodel.schema.MutableColumn;

public class MetaModelInputColumnTest extends TestCase {

	public void testGetDataTypeFamily() throws Exception {
		assertEquals(DataTypeFamily.STRING,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.VARCHAR)).getDataTypeFamily());

		assertEquals(DataTypeFamily.STRING,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.CHAR)).getDataTypeFamily());

		assertEquals(DataTypeFamily.UNDEFINED,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.BLOB)).getDataTypeFamily());

		assertEquals(DataTypeFamily.NUMBER,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.INTEGER)).getDataTypeFamily());

		assertEquals(DataTypeFamily.NUMBER,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.FLOAT)).getDataTypeFamily());

		assertEquals(DataTypeFamily.DATE,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.DATE)).getDataTypeFamily());

		assertEquals(DataTypeFamily.DATE,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.TIMESTAMP)).getDataTypeFamily());

		assertEquals(DataTypeFamily.BOOLEAN,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.BIT)).getDataTypeFamily());

		assertEquals(DataTypeFamily.UNDEFINED,
				new MetaModelInputColumn(new MutableColumn("foobar", ColumnType.JAVA_OBJECT)).getDataTypeFamily());
	}

	public void testConstructorArgRequired() throws Exception {
		try {
			new MetaModelInputColumn(null);
			fail("Exception expected");
		} catch (IllegalArgumentException e) {
			assertEquals("column cannot be null", e.getMessage());
		}
	}
}
