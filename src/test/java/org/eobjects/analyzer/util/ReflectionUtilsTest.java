package org.eobjects.analyzer.util;

import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.CrosstabResult;
import org.eobjects.analyzer.result.PatternFinderResult;
import org.eobjects.analyzer.util.ReflectionUtilTestHelpClass.ClassA;
import org.eobjects.analyzer.util.ReflectionUtilTestHelpClass.ClassB;

public class ReflectionUtilsTest extends TestCase {

	public InputColumn<String> stringInputColumn;

	@SuppressWarnings("rawtypes")
	public InputColumn rawInputColumn;

	public InputColumn<?> unspecifiedInputColumn;

	public InputColumn<? extends Number> unspecifiedNumberInputColumn;

	public InputColumn<String>[] stringInputColumns;

	public InputColumn<? super Number>[] unspecifiedNumberSuperclassInputColumns;

	public InputColumn<Comparable<String>> stringComparableInputColumn;

	public void testExplodeCamelCase() throws Exception {
		assertEquals("Foo bar", ReflectionUtils.explodeCamelCase("fooBar", false));
		assertEquals("f", ReflectionUtils.explodeCamelCase("f", false));
		assertEquals("", ReflectionUtils.explodeCamelCase("", false));
		assertEquals("My name is john doe", ReflectionUtils.explodeCamelCase("MyNameIsJohnDoe", false));
		assertEquals("H e l l o", ReflectionUtils.explodeCamelCase("h e l l o", false));

		assertEquals("Name", ReflectionUtils.explodeCamelCase("getName", true));
	}

	public void testInputColumnType() throws Exception {
		Field field = getClass().getField("stringInputColumn");
		assertEquals(1, ReflectionUtils.getTypeParameterCount(field));
		assertEquals(String.class, ReflectionUtils.getTypeParameter(field, 0));

		field = getClass().getField("rawInputColumn");
		assertEquals(0, ReflectionUtils.getTypeParameterCount(field));

		field = getClass().getField("unspecifiedNumberInputColumn");
		assertEquals(1, ReflectionUtils.getTypeParameterCount(field));
		assertEquals(Number.class, ReflectionUtils.getTypeParameter(field, 0));

		field = getClass().getField("stringInputColumns");
		assertEquals(1, ReflectionUtils.getTypeParameterCount(field));
		assertEquals(String.class, ReflectionUtils.getTypeParameter(field, 0));
		assertTrue(field.getType().isArray());

		field = getClass().getField("unspecifiedNumberSuperclassInputColumns");
		assertEquals(1, ReflectionUtils.getTypeParameterCount(field));
		assertEquals(Object.class, ReflectionUtils.getTypeParameter(field, 0));
		assertTrue(field.getType().isArray());

		field = getClass().getField("stringComparableInputColumn");
		assertEquals(1, ReflectionUtils.getTypeParameterCount(field));
		assertEquals(Comparable.class, ReflectionUtils.getTypeParameter(field, 0));
	}

	public void testIsNumber() throws Exception {
		assertTrue(ReflectionUtils.isNumber(Long.class));
		assertTrue(ReflectionUtils.isNumber(Float.class));
		assertFalse(ReflectionUtils.isNumber(String.class));
		assertFalse(ReflectionUtils.isNumber(Object.class));
	}

	public void testGetHierarchyDistance() throws Exception {
		assertEquals(0, ReflectionUtils.getHierarchyDistance(String.class, String.class));
		assertEquals(1, ReflectionUtils.getHierarchyDistance(String.class, CharSequence.class));
		assertEquals(1, ReflectionUtils.getHierarchyDistance(String.class, Object.class));
		assertEquals(1, ReflectionUtils.getHierarchyDistance(Number.class, Object.class));
		assertEquals(2, ReflectionUtils.getHierarchyDistance(Integer.class, Object.class));
		assertEquals(1, ReflectionUtils.getHierarchyDistance(Integer.class, Number.class));

		assertEquals(1, ReflectionUtils.getHierarchyDistance(CrosstabResult.class, AnalyzerResult.class));
		assertEquals(2, ReflectionUtils.getHierarchyDistance(PatternFinderResult.class, AnalyzerResult.class));
	}

	public void testGetFields() throws Exception {
		Field[] fields = ReflectionUtils.getFields(ClassA.class);
		assertEquals(1, fields.length);

		assertEquals(ClassA.class, fields[0].getDeclaringClass());
		assertEquals("a", fields[0].getName());

		fields = ReflectionUtils.getFields(ClassB.class);
		assertEquals(2, fields.length);

		assertEquals(ClassB.class, fields[0].getDeclaringClass());
		assertEquals("b", fields[0].getName());
		assertEquals(ClassA.class, fields[1].getDeclaringClass());
		assertEquals("a", fields[1].getName());
	}
}
