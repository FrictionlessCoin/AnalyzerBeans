package org.eobjects.analyzer.job.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eobjects.analyzer.job.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleThreadedTaskRunner implements TaskRunner {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private BlockingQueue<Task> _tasks = new LinkedBlockingQueue<Task>();
	private boolean _queueTasks;
	private boolean _running = false;

	public SingleThreadedTaskRunner() {
		this(true);
	}

	public SingleThreadedTaskRunner(boolean queueTasks) {
		_queueTasks = queueTasks;
	}

	@Override
	public void run(Task task) {
		logger.debug("run({})", task);
		if (_queueTasks) {
			_tasks.add(task);
			if (!_running) {
				_running = true;
				while (!_tasks.isEmpty()) {
					Task nextTask = _tasks.poll();
					exec(nextTask);
				}
				_running = false;
			}
		} else {
			exec(task);
		}
	}

	private void exec(Task task) {
		try {
			task.execute();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
