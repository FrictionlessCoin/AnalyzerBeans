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
package org.eobjects.analyzer.job.runner;

import org.eobjects.analyzer.beans.api.Concurrent;
import org.eobjects.analyzer.beans.api.Filter;
import org.eobjects.analyzer.beans.api.QueryOptimizedFilter;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.FilterOutcome;
import org.eobjects.analyzer.job.ImmutableFilterOutcome;

final class FilterConsumer extends AbstractRowProcessingConsumer implements RowProcessingConsumer {

    private final AnalysisJob _job;
    private final Filter<?> _filter;
    private final FilterJob _filterJob;
    private final InputColumn<?>[] _inputColumns;
    private final AnalysisListener _analysisListener;
    private final boolean _concurrent;

    public FilterConsumer(Filter<?> filter, FilterJob filterJob, InputColumn<?>[] inputColumns,
            RowProcessingPublishers publishers) {
        super(filterJob, filterJob);
        _filter = filter;
        _filterJob = filterJob;
        _inputColumns = inputColumns;
        if (publishers == null) {
            _job = null;
            _analysisListener = null;
        } else {
            _job = publishers.getAnalysisJob();
            _analysisListener = publishers.getAnalysisListener();
        }

        Concurrent concurrent = filterJob.getDescriptor().getAnnotation(Concurrent.class);
        if (concurrent == null) {
            // filter are by default concurrent
            _concurrent = true;
        } else {
            _concurrent = concurrent.value();
        }
    }

    @Override
    public boolean isConcurrent() {
        return _concurrent;
    }

    @Override
    public InputColumn<?>[] getRequiredInput() {
        return _inputColumns;
    }

    @Override
    public Filter<?> getComponent() {
        return _filter;
    }

    @Override
    public void consume(InputRow row, int distinctCount, OutcomeSink outcomes, RowProcessingChain chain) {
        try {
            Enum<?> category = _filter.categorize(row);
            FilterOutcome outcome = new ImmutableFilterOutcome(_filterJob, category);
            outcomes.add(outcome);
            chain.processNext(row, distinctCount, outcomes);
        } catch (RuntimeException e) {
            _analysisListener.errorInFilter(_job, _filterJob, row, e);
        }
    }

    @Override
    public FilterJob getComponentJob() {
        return _filterJob;
    }

    @Override
    public String toString() {
        return "FilterConsumer[" + _filter + "]";
    }

    public boolean isQueryOptimizable(FilterOutcome filterOutcome) {
        if (_filter instanceof QueryOptimizedFilter) {
            @SuppressWarnings("rawtypes")
            QueryOptimizedFilter queryOptimizedFilter = (QueryOptimizedFilter) _filter;
            @SuppressWarnings("unchecked")
            boolean optimizable = queryOptimizedFilter.isOptimizable(filterOutcome.getCategory());
            return optimizable;
        }
        return false;
    }
}
