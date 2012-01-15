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
package org.eobjects.analyzer.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eobjects.analyzer.job.ComponentJob;

/**
 * A simple (and Serializable!) implementation of the {@link AnalysisResult}
 * interface. Useful for storing and loading in files or other binary
 * destinations, using Java's serialization API.
 * 
 * @author Kasper Sørensen
 */
public class SimpleAnalysisResult implements Serializable, AnalysisResult {

	private static final long serialVersionUID = 1L;

	private final Map<ComponentJob, AnalyzerResult> _results;

	public SimpleAnalysisResult(Map<ComponentJob, AnalyzerResult> results) {
		_results = results;
	}

	@Override
	public List<AnalyzerResult> getResults() {
		return new ArrayList<AnalyzerResult>(_results.values());
	}

	@Override
	public AnalyzerResult getResult(ComponentJob componentJob) {
		return _results.get(componentJob);
	}

	@Override
	public Map<ComponentJob, AnalyzerResult> getResultMap() {
		return Collections.unmodifiableMap(_results);
	}

}
