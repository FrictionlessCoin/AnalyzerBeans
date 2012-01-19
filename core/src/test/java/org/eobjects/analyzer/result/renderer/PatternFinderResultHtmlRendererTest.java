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
package org.eobjects.analyzer.result.renderer;

import java.io.File;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.stringpattern.PatternFinderAnalyzer;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.MockInputColumn;
import org.eobjects.analyzer.data.MockInputRow;
import org.eobjects.analyzer.result.PatternFinderResult;
import org.eobjects.analyzer.storage.InMemoryRowAnnotationFactory;
import org.eobjects.metamodel.util.FileHelper;

public class PatternFinderResultHtmlRendererTest extends TestCase {

	public void testSinglePatterns() throws Exception {
		InputColumn<String> col1 = new MockInputColumn<String>("email username", String.class);

		PatternFinderAnalyzer analyzer = new PatternFinderAnalyzer();
		analyzer.setColumn(col1);
		analyzer.setRowAnnotationFactory(new InMemoryRowAnnotationFactory());
		analyzer.init();

		analyzer.run(new MockInputRow().put(col1, "kasper"), 1);
		analyzer.run(new MockInputRow().put(col1, "kasper.sorensen"), 1);
		analyzer.run(new MockInputRow().put(col1, "info"), 1);
		analyzer.run(new MockInputRow().put(col1, "kasper.sorensen"), 1);
		analyzer.run(new MockInputRow().put(col1, "winfried.vanholland"), 1);
		analyzer.run(new MockInputRow().put(col1, "kaspers"), 1);

		PatternFinderResult result = analyzer.getResult();

		String html = new PatternFinderResultHtmlRenderer().render(result);
		assertEquals(
				FileHelper.readFileAsString(new File("src/test/resources/pattern_finder_result_html_renderer_single.html")),
				html);
	}

	public void testMultiplePatterns() throws Exception {
		InputColumn<String> col1 = new MockInputColumn<String>("email username", String.class);
		InputColumn<String> col2 = new MockInputColumn<String>("email domain", String.class);

		PatternFinderAnalyzer analyzer = new PatternFinderAnalyzer();
		analyzer.setColumn(col1);
		analyzer.setGroupColumn(col2);
		analyzer.setRowAnnotationFactory(new InMemoryRowAnnotationFactory());
		analyzer.init();

		analyzer.run(new MockInputRow().put(col1, "kasper").put(col2, "eobjects.dk"), 1);
		analyzer.run(new MockInputRow().put(col1, "kasper.sorensen").put(col2, "eobjects.dk"), 1);
		analyzer.run(new MockInputRow().put(col1, "info").put(col2, "eobjects.dk"), 1);
		analyzer.run(new MockInputRow().put(col1, "kasper.sorensen").put(col2, "humaninference.com"), 1);
		analyzer.run(new MockInputRow().put(col1, "winfried.vanholland").put(col2, "humaninference.com"), 1);
		analyzer.run(new MockInputRow().put(col1, "kaspers").put(col2, "humaninference.com"), 1);

		PatternFinderResult result = analyzer.getResult();

		String html = new PatternFinderResultHtmlRenderer().render(result);
		assertEquals(FileHelper.readFileAsString(new File(
				"src/test/resources/pattern_finder_result_html_renderer_multiple.html")), html);
	}
}
