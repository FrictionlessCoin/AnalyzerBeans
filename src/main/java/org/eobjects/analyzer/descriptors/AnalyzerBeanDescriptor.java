package org.eobjects.analyzer.descriptors;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eobjects.analyzer.annotations.AnalyzerBean;
import org.eobjects.analyzer.annotations.Close;
import org.eobjects.analyzer.annotations.Configured;
import org.eobjects.analyzer.annotations.Initialize;
import org.eobjects.analyzer.annotations.Provided;
import org.eobjects.analyzer.annotations.Result;
import org.eobjects.analyzer.beans.ExploringAnalyzer;
import org.eobjects.analyzer.beans.RowProcessingAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerBeanDescriptor extends AbstractBeanDescriptor {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String displayName;
	private boolean exploringAnalyzer;
	private boolean rowProcessingAnalyzer;

	private List<ResultDescriptor> resultDescriptors = new LinkedList<ResultDescriptor>();

	public AnalyzerBeanDescriptor(Class<?> analyzerClass)
			throws DescriptorException {
		super(analyzerClass);

		rowProcessingAnalyzer = AnnotationHelper.is(analyzerClass,
				RowProcessingAnalyzer.class);
		exploringAnalyzer = AnnotationHelper.is(analyzerClass,
				ExploringAnalyzer.class);

		if (!rowProcessingAnalyzer && !exploringAnalyzer) {
			throw new DescriptorException(analyzerClass
					+ " does not implement either "
					+ RowProcessingAnalyzer.class.getName() + " or "
					+ ExploringAnalyzer.class.getName());
		}

		AnalyzerBean analyzerAnnotation = analyzerClass
				.getAnnotation(AnalyzerBean.class);
		if (analyzerAnnotation == null) {
			throw new DescriptorException(analyzerClass
					+ " doesn't implement the AnalyzerBean annotation");
		}
		displayName = analyzerAnnotation.value();
		if (displayName == null || displayName.trim().equals("")) {
			displayName = AnnotationHelper.explodeCamelCase(
					analyzerClass.getSimpleName(), false);
		}

		Field[] fields = analyzerClass.getDeclaredFields();
		for (Field field : fields) {

			Configured configuredAnnotation = field
					.getAnnotation(Configured.class);
			if (configuredAnnotation != null) {
				if (!field.isAnnotationPresent(Inject.class)) {
					logger.warn(
							"No @Inject annotation found for @Configured field: {}",
							field);
				}
				configuredDescriptors.add(new ConfiguredDescriptor(field,
						configuredAnnotation));
			}

			Provided providedAnnotation = field.getAnnotation(Provided.class);
			if (providedAnnotation != null) {
				if (!field.isAnnotationPresent(Inject.class)) {
					logger.warn(
							"No @Inject annotation found for @Provided field: {}",
							field);
				}
				providedDescriptors.add(new ProvidedDescriptor(field,
						providedAnnotation));
			}
		}

		if (AnnotationHelper.isCloseable(analyzerClass)) {
			try {
				Method method = analyzerClass.getMethod("close",
						new Class<?>[0]);
				closeDescriptors.add(new CloseDescriptor(method));
			} catch (NoSuchMethodException e) {
				// This is impossible since all closeable's have a no-arg close
				// method
				assert false;
			}
		}

		Method[] methods = analyzerClass.getDeclaredMethods();
		for (Method method : methods) {
			Configured configuredAnnotation = method
					.getAnnotation(Configured.class);
			if (configuredAnnotation != null) {
				if (!method.isAnnotationPresent(Inject.class)) {
					logger.warn(
							"No @Inject annotation found for @Configured method: {}",
							method);
				}
				configuredDescriptors.add(new ConfiguredDescriptor(method,
						configuredAnnotation));
			}

			Provided providedAnnotation = method.getAnnotation(Provided.class);
			if (providedAnnotation != null) {
				if (!method.isAnnotationPresent(Inject.class)) {
					logger.warn(
							"No @Inject annotation found for @Provided method: {}",
							method);
				}
				providedDescriptors.add(new ProvidedDescriptor(method,
						providedAnnotation));
			}

			Initialize initializeAnnotation = method
					.getAnnotation(Initialize.class);
			if (initializeAnnotation != null) {
				initializeDescriptors.add(new InitializeDescriptor(method,
						initializeAnnotation));
			}

			// @PostConstruct is a valid substitution for @Initialize
			PostConstruct postConstructAnnotation = method
					.getAnnotation(PostConstruct.class);
			if (postConstructAnnotation != null) {
				initializeDescriptors.add(new InitializeDescriptor(method,
						postConstructAnnotation));
			}

			Result resultAnnotation = method.getAnnotation(Result.class);
			if (resultAnnotation != null) {
				resultDescriptors.add(new ResultDescriptor(method,
						resultAnnotation));
			}

			Close closeAnnotation = method.getAnnotation(Close.class);
			if (closeAnnotation != null) {
				closeDescriptors.add(new CloseDescriptor(method,
						closeAnnotation));
			}

			// @PreDestroy is a valid substitution for @Close
			PreDestroy preDestroyAnnotation = method
					.getAnnotation(PreDestroy.class);
			if (preDestroyAnnotation != null) {
				closeDescriptors.add(new CloseDescriptor(method,
						preDestroyAnnotation));
			}
		}

		if (resultDescriptors.isEmpty()) {
			throw new DescriptorException(analyzerClass
					+ " doesn't define any @Result annotated methods");
		}

		if (rowProcessingAnalyzer) {
			int numConfiguredColumns = 0;
			int numConfiguredColumnArrays = 0;
			for (ConfiguredDescriptor cd : configuredDescriptors) {
				if (cd.isInputColumn()) {
					if (cd.isArray()) {
						numConfiguredColumnArrays++;
					} else {
						numConfiguredColumns++;
					}
				}
			}
			int totalColumns = numConfiguredColumns + numConfiguredColumnArrays;
			if (totalColumns == 0) {
				throw new DescriptorException(
						analyzerClass
								+ " does not define a @Configured InputColumn or InputColumn-array");
			}
			if (totalColumns > 1) {
				throw new DescriptorException(
						analyzerClass
								+ " defines multiple @Configured InputColumns, cannot determine which one to use for row processing");
			}
		}

		// Make the descriptor lists read-only
		closeDescriptors = Collections.unmodifiableList(closeDescriptors);
		configuredDescriptors = Collections
				.unmodifiableList(configuredDescriptors);
		initializeDescriptors = Collections
				.unmodifiableList(initializeDescriptors);
		providedDescriptors = Collections.unmodifiableList(providedDescriptors);
		resultDescriptors = Collections.unmodifiableList(resultDescriptors);
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isExploringAnalyzer() {
		return exploringAnalyzer;
	}

	public boolean isRowProcessingAnalyzer() {
		return rowProcessingAnalyzer;
	}

	public List<ResultDescriptor> getResultDescriptors() {
		return resultDescriptors;
	}
	
	@Override
	public ConfiguredDescriptor getConfiguredDescriptorForInput() {
		if (isRowProcessingAnalyzer()) {
			return super.getConfiguredDescriptorForInput();
		}
		return null;
	}
}