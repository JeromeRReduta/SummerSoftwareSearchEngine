import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple work queue implementation based on the IBM developerWorks article by
 * Brian Goetz. It is up to the user of this class to keep track of whether
 * there is any pending work remaining.
 *
 * @see <a href="https://www.ibm.com/developerworks/library/j-jtp0730/">
 * Java Theory and Practice: Thread Pools and Work Queues</a>
 * 
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class WorkQueue {
	/**
	 * Pool of worker threads that will wait in the background until work is
	 * available.
	 */
	private final Worker[] workers;

	/** Queue of pending work requests. */
	private final LinkedList<Runnable> queue;

	/** Used to signal the queue should be shutdown. */
	private volatile boolean shutdown;

	/** The default number of threads to use when not specified. */
	public static final int DEFAULT = 5;

	/** Logger used for this class. */
	private static final Logger log = LogManager.getLogger();
	
	/** Keeps track of how many tasks are in the queue */
	private int pending;
	
	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #WorkQueue(int)
	 */
	public WorkQueue() {
		this(DEFAULT);
	}
	
	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads number of worker threads; should be greater than 1
	 */
	public WorkQueue(int threads) {
		
		int safeThreadCount = threads > 0 ? threads : DEFAULT;
		
		this.queue = new LinkedList<Runnable>();
		this.workers = new Worker[safeThreadCount];
		this.shutdown = false;
		this.pending = 0;

		for (int i = 0; i < safeThreadCount; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		log.debug("Work queue initialized with {} worker threads.", workers.length);
	}

	/**
	 * Adds a work request to the queue. A thread will process this request when
	 * available.
	 *
	 * @param task work request (in the form of a {@link Runnable} object)
	 */
	public void execute(Runnable task) {
		incrementPending();
		synchronized (queue) {
			queue.addLast(task);
			queue.notifyAll();
		}
	}
	
	/** Safely increments pending */
	public synchronized void incrementPending() { // TODO private
		pending++;
	}
	
	/** Safely decrements pending. If after decrementing, pending == 0, notifies all waiting threasd */
	public synchronized void decrementPending() { // TODO private
		pending--;
		if (pending == 0) {
			this.notifyAll();
		}
	}
	
	/**
	 * Waits for all pending work to be finished. Does not terminate the worker
	 * threads so that the work queue can continue to be used.
	 */
	public void finish() {
		try {
			synchronized(this) {
				while (pending > 0) {
					this.wait();
				}
			}
		}
		catch (InterruptedException e) {
			log.catching(Level.DEBUG, e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work will not be finished, but
	 * threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		shutdown = true; // safe to do unsynchronized due to volatile keyword
		log.debug("Work queue triggering shutdown...");
		
		synchronized (queue) {
			queue.notifyAll();
		}
	}
	
	/**
	 * Similar to {@link Thread#join()}, waits for all the work to be finished
	 * and the worker threads to terminate. The work queue cannot be reused after
	 * this call completes.
	 */
	public void join() {
		try {
			finish();
			shutdown();

			for (Worker worker : workers) {
				worker.join();
			}
			log.debug("All worker threads terminated.");
		}
		catch (InterruptedException e) {
			System.err.println("Warning: Work queue interrupted while joining.");
			log.catching(Level.DEBUG, e);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Returns the number of worker threads being used by the work queue.
	 *
	 * @return number of worker threads
	 */
	public int size() {
		return workers.length;
	}

	/**
	 * Waits until work is available in the work queue. When work is found, will
	 * remove the work from the queue and run it. If a shutdown is detected, will
	 * exit instead of grabbing new work from the queue. These threads will
	 * continue running in the background until a shutdown is requested.
	 */
	private class Worker extends Thread {
		/**
		 * Initializes a worker thread with a custom name.
		 */
		public Worker() {
			setName("Worker" + getName());
			//incrementPending(); // ok to put pending in execute() & not constructor?
		}

		@Override
		public void run() {
			Runnable task = null;

			try {
				while (true) {
					synchronized (queue) {
						while (queue.isEmpty() && !shutdown) { // Case: queue has work and shutdown hasn't been called (this line is from lecture code)
							log.debug("Work queue worker waiting...");
							queue.wait();
						}
						
						if (shutdown) {
							log.debug("Worker detected shutdown...");
							break;
						}
						else {
							task = queue.removeFirst();
						}
					}
					
					try {
						log.debug("Work queue worker found work.");
						task.run();
					}
					catch (RuntimeException e) { // catch runtime exceptions to avoid leaking threads
						System.err.println("Warning: Work queue encountered an exception while running.");
						log.catching(Level.DEBUG, e);
					}
					
					decrementPending();
				}
				log.debug("Worker thread terminating...");
			}
			catch (InterruptedException e) {
				System.err.println("Warning: Worker thread interrupted while waiting.");
				log.catching(Level.DEBUG, e);
				Thread.currentThread().interrupt();
			}
		}
	}
}
