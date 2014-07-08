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

import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.result.AnalyzerResult;

/**
 * An {@link AnalysisListener} that does nothing. Useful base-class for
 * implementing only particular methods of the {@link AnalysisListener}
 * interface.
 */
public class AnalysisListenerAdaptor implements AnalysisListener {

    @Override
    public void jobBegin(AnalysisJob job, AnalysisJobMetrics metrics) {
    }

    @Override
    public void jobSuccess(AnalysisJob job, AnalysisJobMetrics metrics) {
    }

    @Override
    public void rowProcessingBegin(AnalysisJob job, RowProcessingMetrics metrics) {
    }

    @Override
    public void rowProcessingProgress(AnalysisJob job, RowProcessingMetrics metrics, InputRow row, int currentRow) {
        rowProcessingProgress(job, metrics, currentRow);
    }

    /**
     * 
     * @param job
     * @param metrics
     * @param currentRow
     * 
     * @deprecated use
     *             {@link #rowProcessingProgress(AnalysisJob, RowProcessingMetrics, InputRow, int)}
     *             instead.
     */
    @Deprecated
    protected void rowProcessingProgress(AnalysisJob job, RowProcessingMetrics metrics, int currentRow) {
    }

    @Override
    public void rowProcessingSuccess(AnalysisJob job, RowProcessingMetrics metrics) {
    }

    @Override
    public void analyzerBegin(AnalysisJob job, AnalyzerJob analyzerJob, AnalyzerMetrics metrics) {
    }

    @Override
    public void analyzerSuccess(AnalysisJob job, AnalyzerJob analyzerJob, AnalyzerResult result) {
    }

    @Override
    public void errorInFilter(AnalysisJob job, FilterJob filterJob, InputRow row, Throwable throwable) {
    }

    @Override
    public void errorInTransformer(AnalysisJob job, TransformerJob transformerJob, InputRow row, Throwable throwable) {
    }

    @Override
    public void errorInAnalyzer(AnalysisJob job, AnalyzerJob analyzerJob, InputRow row, Throwable throwable) {
    }

    @Override
    public void errorUknown(AnalysisJob job, Throwable throwable) {
    }
}
