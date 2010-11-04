package org.eobjects.analyzer.storage;

import java.util.HashSet;
import java.util.Set;

import org.eobjects.analyzer.data.InputRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RowAnnotationFactory that is able to switch strategies/implementations
 * based on a threshold amount of annotated rows. This is useful for having
 * small amounts of rows stored in memory and large amounts stored on disk.
 * 
 * @author Kasper Sørensen
 */
public class ThresholdRowAnnotationFactory implements RowAnnotationFactory {

	private static final Logger logger = LoggerFactory.getLogger(ThresholdRowAnnotationFactory.class);

	private final Set<RowAnnotation> _persistentAnnotations = new HashSet<RowAnnotation>();
	private final int _rowCountThreshold;
	private final InMemoryRowAnnotationFactory _inMemoryDelegate;
	private final RowAnnotationFactory _persistentDelegate;

	public ThresholdRowAnnotationFactory(RowAnnotationFactory persistentDelegate) {
		this(500, persistentDelegate);
	}

	public ThresholdRowAnnotationFactory(int rowCountThreshold, RowAnnotationFactory persistentDelegate) {
		_rowCountThreshold = rowCountThreshold;
		_persistentDelegate = persistentDelegate;
		_inMemoryDelegate = new InMemoryRowAnnotationFactory();
	}

	@Override
	public synchronized void reset(RowAnnotation annotation) {
		if (_persistentAnnotations.contains(annotation)) {
			_persistentDelegate.reset(annotation);
		} else {
			_inMemoryDelegate.reset(annotation);
		}
	}

	@Override
	public synchronized void annotate(InputRow row, int distinctCount, RowAnnotation annotation) {
		if (_persistentAnnotations.contains(annotation)) {
			_persistentDelegate.annotate(row, distinctCount, annotation);
		} else {
			_inMemoryDelegate.annotate(row, distinctCount, annotation);

			if (_inMemoryDelegate.getRowCount(annotation) >= _rowCountThreshold) {
				makePersistent(annotation);
			}
		}
	}

	private void makePersistent(RowAnnotation annotation) {
		logger.info("Making persistent storage for annotation {}", annotation);
		int correctRowCount = annotation.getRowCount();
		InputRow[] rows = _inMemoryDelegate.getRows(annotation);
		_inMemoryDelegate.reset(annotation);

		for (InputRow row : rows) {
			_persistentDelegate.annotate(row, 1, annotation);
		}

		int remainingRowCount = correctRowCount - annotation.getRowCount();
		if (remainingRowCount > 0) {
			((RowAnnotationImpl) annotation).incrementRowCount(remainingRowCount);
		}

		_persistentAnnotations.add(annotation);
	}

	@Override
	public RowAnnotation createAnnotation() {
		return new RowAnnotationImpl();
	}

	@Override
	public InputRow[] getRows(RowAnnotation annotation) {
		if (_persistentAnnotations.contains(annotation)) {
			return _persistentDelegate.getRows(annotation);
		} else {
			return _inMemoryDelegate.getRows(annotation);
		}
	}
}
