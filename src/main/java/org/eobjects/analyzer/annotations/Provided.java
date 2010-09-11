package org.eobjects.analyzer.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Methods and fields with the @Provided annotation are used to let
 * AnalyzerBeans and ResultProducers retrieve service-objects such as persistent
 * collections, the current SchemaNavigator or DataContext.
 * 
 * This features ensures separation of concerns: The AnalyzerBeans framework
 * will make sure that persistence is handled and the bean-developer will not
 * have to worry about memory problems related to his/her collection(s).
 * 
 * Additionally Analyzerbeans can use the @Provided annotation to inject a
 * SchemaNavigator in order to perform metadata-based analysis. AnalyzerBeans
 * can also inject a DataContext, but this is generally discouraged for normal
 * use-cases since it will either be provided with the run(...)-method if the
 * AnalyzerBean is an ExploringAnalyzer or be out of scope if the AnalyzerBean
 * is a RowProcessingAnalyzer. For some use-cases it is helpful though, for
 * example initialization that requires some simple querying.
 * 
 * Valid types for @Provided annotated fields and method arguments are:
 * <ul>
 * <li>List</li>
 * <li>Map</li>
 * <li>SchemaNavigator</li>
 * <li>DataContext</li>
 * </ul>
 * Generic/parameterized types for the List or Map can be any of:
 * <ul>
 * <li>Boolean</li>
 * <li>Byte</li>
 * <li>Short</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>Character</li>
 * <li>String</li>
 * <li>Byte[] or byte[]</li>
 * </ul>
 * 
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD })
@Documented
@Inherited
@Qualifier
public @interface Provided {
}
