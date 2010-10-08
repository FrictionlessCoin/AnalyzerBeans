package org.eobjects.analyzer.job.concurrent;

import org.eobjects.analyzer.job.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SingleThreadedTaskRunner implements TaskRunner {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void run(final Task task, final ErrorReporter errorReporter) {
		logger.debug("run({})", task);
		TaskRunnable taskRunnable = new TaskRunnable(task, errorReporter);
		taskRunnable.run();
	}

	@Override
	public void shutdown() {
		logger.info("shutdown() called, nothing to do");
	}
}
