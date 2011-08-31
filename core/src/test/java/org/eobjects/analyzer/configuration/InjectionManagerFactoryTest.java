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
package org.eobjects.analyzer.configuration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.eobjects.analyzer.beans.api.AnalyzerBean;
import org.eobjects.analyzer.beans.api.Configured;
import org.eobjects.analyzer.beans.api.RowProcessingAnalyzer;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.RowProcessingAnalyzerJobBuilder;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.result.NumberResult;
import org.eobjects.analyzer.test.TestHelper;
import org.junit.Ignore;

public class InjectionManagerFactoryTest extends TestCase {

	@Ignore
	@AnalyzerBean("Fancy transformer")
	public static class FancyTransformer implements RowProcessingAnalyzer<NumberResult> {

		@Inject
		AtomicInteger fancyInjection;

		@Configured
		InputColumn<String> col;

		@Override
		public void run(InputRow row, int distinctCount) {
		}

		@Override
		public NumberResult getResult() {
			return new NumberResult(fancyInjection.get());
		}
	}

	public void testInjectCustomClass() throws Exception {
		final AtomicBoolean touched = new AtomicBoolean(false);

		final InjectionManager injectionManager = new InjectionManager() {
			@SuppressWarnings("unchecked")
			@Override
			public <E> E getInstance(InjectionPoint<E> injectionPoint) {
				touched.set(true);
				assertEquals(AtomicInteger.class, injectionPoint.getBaseType());
				return (E) new AtomicInteger(42);
			}
		};

		final InjectionManagerFactory injectionManagerFactory = new InjectionManagerFactory() {
			@Override
			public InjectionManager getInjectionManager(AnalysisJob job) {
				return injectionManager;
			}
		};

		final AnalyzerBeansConfigurationImpl conf = new AnalyzerBeansConfigurationImpl().replace(
				new DatastoreCatalogImpl(TestHelper.createSampleDatabaseDatastore("orderdb"))).replace(
				injectionManagerFactory);

		final AnalysisJobBuilder ajb = new AnalysisJobBuilder(conf);
		ajb.setDatastore("orderdb");
		ajb.addSourceColumns("PUBLIC.EMPLOYEES.EMPLOYEENUMBER");

		final RowProcessingAnalyzerJobBuilder<FancyTransformer> analyzerBuilder = ajb
				.addRowProcessingAnalyzer(FancyTransformer.class);
		analyzerBuilder.addInputColumns(ajb.getSourceColumns());

		final AnalysisResultFuture result = new AnalysisRunnerImpl(conf).run(ajb.toAnalysisJob());
		assertTrue(result.isSuccessful());
		assertTrue(touched.get());
		assertEquals("42", result.getResults().get(0).toString());

	}
}