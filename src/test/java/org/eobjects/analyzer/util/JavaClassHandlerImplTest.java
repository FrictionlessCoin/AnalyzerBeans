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
package org.eobjects.analyzer.util;

import java.io.File;

import org.eobjects.analyzer.reference.Function;

import junit.framework.TestCase;

public class JavaClassHandlerImplTest extends TestCase {

	File dir = new File("target/javaClassHandlerTestClasses");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		dir.mkdirs();
	}

	public void testGetClassName() throws Exception {
		JavaClassHandlerImpl cu = new JavaClassHandlerImpl(dir);

		assertEquals(
				"MyClass",
				cu.getClassName("package foo.bar; import java.util.*; import\n com.Myclass ; "
						+ "public class MyClass<E> extends Object {}"));
		assertEquals(
				"foo.bar",
				cu.getPackageName("package foo.bar; import java.util.*; import\n com.Myclass ; "
						+ "public class MyClass<E> extends Object {}"));

		assertEquals(
				"MyClass",
				cu.getClassName("package foo.bar; import java.util.*; import\n com.\nclass ; "
						+ "public class MyClass<E> extends Object {}"));

		assertEquals(
				"MyClass",
				cu.getClassName("package foo.bar; import java.util.*; import\n com. class ; "
						+ "public class MyClass<E> extends Object {}"));

		assertEquals("MyClass", cu.getClassName("package foo.bar.\n class;"
				+ "public class MyClass<E> extends Object {}"));
		assertEquals("foo.bar.class",
				cu.getPackageName("package foo.bar.\n class;"
						+ "public class MyClass<E> extends Object {}"));
	}

	public void testCompileAndLoad() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("package org.eobjects.analyzer.result;");
		sb.append("import java.util.*;");
		sb.append("import org.eobjects.analyzer.util.*;");
		sb.append("import org.eobjects.analyzer.reference.*;");
		sb.append("public class MyFunction implements Function<String,Number> {");
		sb.append("  public Number run(String str) { return str.length(); }");
		sb.append("}");
		Class<?> c = new JavaClassHandlerImpl(dir)
				.compileAndLoad(sb.toString());
		assertNotNull(c);

		assertTrue(ReflectionUtils.is(c, Function.class));

		@SuppressWarnings("unchecked")
		Function<String, Number> f = (Function<String, Number>) c.newInstance();
		Number result = f.run("hello");
		assertEquals(5, result.intValue());
	}
}
