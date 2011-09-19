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
package org.eobjects.analyzer.beans.mock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Close;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.Explorer;
import org.eobjects.analyzer.beans.api.Initialize;
import org.eobjects.analyzer.beans.api.Provided;
import org.eobjects.analyzer.result.NumberResult;
import org.eobjects.metamodel.DataContext;

@AnalyzerBean("Exploring mock")
public class ExploringAnalyzerMock implements Explorer<NumberResult> {

	private static List<ExploringAnalyzerMock> instances = new LinkedList<ExploringAnalyzerMock>();

	public static List<ExploringAnalyzerMock> getInstances() {
		return instances;
	}

	public static void clearInstances() {
		instances.clear();
	}

	public ExploringAnalyzerMock() {
		instances.add(this);
	}

	// A field-level @Configured property
	@Configured
	private String configured1;

	public String getConfigured1() {
		return configured1;
	}

	@Configured
	private Integer configured2;
	
	public Integer getConfigured2() {
		return configured2;
	}

	// A field-level @Provided property
	@Provided
	private Map<String, Long> providedMap;

	public Map<String, Long> getProvidedMap() {
		return providedMap;
	}

	@Provided
	private List<Boolean> providedList;
	
	public List<Boolean> getProvidedList() {
		return providedList;
	}

	private boolean init1 = false;
	private boolean init2 = false;

	@Initialize
	public void init1() {
		this.init1 = true;
	}

	public boolean isInit1() {
		return init1;
	}

	@Initialize
	public void init2() {
		this.init2 = true;
	}

	public boolean isInit2() {
		return init2;
	}

	private int runCount;

	@Override
	public void run(DataContext dc) {
		TestCase.assertNotNull(dc);
		this.runCount++;
	}

	public int getRunCount() {
		return runCount;
	}

	private boolean close1 = false;
	private boolean close2 = false;

	@Close
	public void close1() {
		this.close1 = true;
	}

	public boolean isClose1() {
		return close1;
	}

	@Close
	public void close2() {
		this.close2 = true;
	}

	public boolean isClose2() {
		return close2;
	}

	private boolean result = false;

	@Override
	public NumberResult getResult() {
		result = true;
		return new NumberResult(runCount);
	}

	public boolean isResult() {
		return result;
	}
}
