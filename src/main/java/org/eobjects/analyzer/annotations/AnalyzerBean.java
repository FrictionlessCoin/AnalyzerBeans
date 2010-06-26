package org.eobjects.analyzer.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes that are annotated with the @AnalyzerBean annotation are components
 * for data analysis. All @AnalyzerBean classes must implement either
 * <code>org.eobjects.analyzer.beans.ExploringAnalyzer</code> or
 * <code>org.eobjects.analyzer.beans.RowProcessingAnalyzer</code>.
 * 
 * The life-cycle of an AnalyzerBean is as follows:
 * <ul>
 * <li>Instantiation. All AnalyzerBeans need to provide a no-args constructor.</li>
 * <li>All methods or fields with the @Configured annotation are
 * invoked/assigned to configure the AnalyzerBean before execution.</li>
 * <li>All methods or fields with the @Provided annotation are invoked/assigned</li>
 * <li>Any no-args methods with the @Initialize annotation are executed.</li>
 * <li>If the AnalyzerBean is an ExploringAnalyzer then the run(DataContext)
 * method is called once. If the AnalyzerBean is a RowProcessingAnalyzer then
 * the run(Row,long) method is called for each row in the analyzed DataSet.</li>
 * <li>All methods with the @Result annotation are invoked to retrieve the
 * result.</li>
 * <li>Any no-args methods with the @Close annotation are invoked if the
 * analyzer needs to release any resources.</li>
 * <li>If the analyzer implements the java.io.Closeable interface, the close()
 * method is also invoked.</li>
 * <li>The AnalyzerBean object is dereferenced and garbage collected</li>
 * </ul>
 * 
 * @see org.eobjects.analyzer.lifecycle.LifeCycleState
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface AnalyzerBean {

	/**
	 * The display name of the AnalyzerBean. The display name should be humanly
	 * readable and is presented to the user in User Interfaces.
	 * 
	 * @return the name of the AnalyzerBean
	 */
	String value();
}
