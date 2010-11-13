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

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.concurrent.JobTaskListener;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.result.NumberResult;

public class AnalysisResultFutureImplTest extends TestCase {

	public void testIsSuccessful() throws Exception {
		Queue<AnalyzerJobResult> resultQueue = new LinkedList<AnalyzerJobResult>();
		JobTaskListener jobCompletionListener = EasyMock.createMock(JobTaskListener.class);
		ErrorAware errorAware = EasyMock.createMock(ErrorAware.class);

		EasyMock.expect(jobCompletionListener.isDone()).andReturn(true);
		EasyMock.expect(errorAware.isErrornous()).andReturn(false);
		EasyMock.expect(errorAware.isErrornous()).andReturn(true);

		EasyMock.replay(jobCompletionListener, errorAware);

		AnalysisResultFutureImpl resultFuture = new AnalysisResultFutureImpl(resultQueue, jobCompletionListener, errorAware);
		assertTrue(resultFuture.isSuccessful());

		assertFalse(resultFuture.isSuccessful());

		EasyMock.verify(jobCompletionListener, errorAware);
	}

	public void testGetResultByJob() throws Exception {
		AnalyzerJob analyzerJob1 = EasyMock.createMock(AnalyzerJob.class);
		AnalyzerJob analyzerJob2 = EasyMock.createMock(AnalyzerJob.class);
		AnalyzerJob analyzerJob3 = EasyMock.createMock(AnalyzerJob.class);

		Queue<AnalyzerJobResult> resultQueue = new LinkedList<AnalyzerJobResult>();

		resultQueue.add(new AnalyzerJobResult(new NumberResult(1), analyzerJob1));
		resultQueue.add(new AnalyzerJobResult(new NumberResult(2), analyzerJob2));

		JobTaskListener jobCompletionListener = EasyMock.createMock(JobTaskListener.class);
		ErrorAware errorAware = EasyMock.createMock(ErrorAware.class);
		EasyMock.expect(jobCompletionListener.isDone()).andReturn(true);
		EasyMock.expect(errorAware.isErrornous()).andReturn(false).times(4);

		EasyMock.replay(jobCompletionListener, errorAware);

		AnalysisResultFutureImpl resultFuture = new AnalysisResultFutureImpl(resultQueue, jobCompletionListener, errorAware);

		resultFuture.await();

		assertEquals("1", resultFuture.getResult(analyzerJob1).toString());
		assertEquals("2", resultFuture.getResult(analyzerJob2).toString());
		assertNull(resultFuture.getResult(analyzerJob3));
		
		Map<AnalyzerJob, AnalyzerResult> resultMap = resultFuture.getResultMap();

		EasyMock.verify(jobCompletionListener, errorAware);

		assertEquals(2, resultMap.size());
		assertEquals("1", resultMap.get(analyzerJob1).toString());
		assertEquals("2", resultMap.get(analyzerJob2).toString());
	}
}
