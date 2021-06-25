import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Maintains a pair of associated locks, one for read-only operations and one
 * for writing. The read lock may be held simultaneously by multiple reader
 * threads, so long as there are no writers. The write lock is exclusive. The
 * active writer is able to acquire read or write locks as long as it is active.
 *
 * <!-- simplified lock used for this class -->
 * @see SimpleLock
 *
 * <!-- built-in Java locks that are similar (but more complex) -->
 * @see Lock
 * @see ReentrantLock
 * @see ReadWriteLock
 * @see ReentrantReadWriteLock
 *
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class SimpleReadWriteLock {
	/** The conditional lock used for reading. */
	private final SimpleLock readerLock;

	/** The conditional lock used for writing. */
	private final SimpleLock writerLock;

	/** The number of active readers. */
	private int readers;

	/** The number of active writers; */
	private int writers;

	/** The thread that holds the write lock. */
	private Thread activeWriter;

	/** The log4j2 logger. */
	private static final Logger log = LogManager.getLogger();

	/**
	 * The lock object used for synchronized access of readers and writers. For
	 * security reasons, a separate private final lock object is used.
	 *
	 * @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/LCK00-J.+Use+private+final+lock+objects+to+synchronize+classes+that+may+interact+with+untrusted+code">
	 *      SEI CERT Oracle Coding Standard for Java</a>
	 */
	private final Object lock;

	/**
	 * Initializes a new simple read/write lock.
	 */
	public SimpleReadWriteLock() {
		readerLock = new SimpleReadLock();
		writerLock = new SimpleWriteLock();

		lock = new Object();

		readers = 0;
		writers = 0;

		activeWriter = null;
	}
	
	/**
	 * Synchronizes a supplier method with locks
	 * @param <O> output type
	 * @param method supplier method
	 * @param write true: method is a write operation; false: method is a read operation
	 * @return the output of the supplier method
	 */
	public <O> O synchronizeWithSupplier(Supplier<O> method, boolean write) {
		
		SimpleLock lock = write ? writerLock : readerLock;
		
		lock.lock();
		try {
			return method.get();
		}
		finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Synchronizes a function method with locks
	 * @param <I> input type
	 * @param <O> output type
	 * @param method function method
	 * @param input input
	 * @param write true: method is a write operation; false: method is a read operation
	 * @return the output of the function method
	 */
	public <I, O> O synchronizeWithFunction(Function<I, O> method, I input, boolean write) {
		
		SimpleLock lock = write ? writerLock : readerLock;
		
		lock.lock();
		try {
			return method.apply(input);
		}
		finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Synchronizes a BiFunction method with locks
	 * @param <I> input1 type
	 * @param <J> input2 type
	 * @param <O> output type
	 * @param method BiFunction method
	 * @param input1 first input
	 * @param input2 second input
	 * @param write true: method is a write operation; false: method is a read operation
	 * @return the output of the function method
	 */
	public <I, J, O> O synchronizeWithBiFunction(BiFunction<I, J, O> method, I input1, J input2, boolean write) {
		
		SimpleLock lock = write ? writerLock : readerLock;
		
		lock.lock();
		try {
			return method.apply(input1, input2);
		}
		finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Synchronizes a consumer method with locks
	 * @param <I> input type
	 * @param method consumer type
	 * @param input input
	 * @param write true: method is a write operation; false: method is a read operation
	 */
	public <I> void synchronizeWithConsumer(Consumer<I> method, I input, boolean write) {
		
		SimpleLock lock = write ? writerLock : readerLock;

		lock.lock();
		try {
			method.accept(input);
		}
		finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Synchronizes a Runnable method with locks
	 * @param method Runnable method
	 * @param write true: method is a write operation; false: method is a read operation
	 */
	public void synchronizeWithRunnable(Runnable method, boolean write) {
		
		SimpleLock lock = write ? writerLock : readerLock;
		
		lock.lock();
		try {
			method.run();
		}
		finally {
			lock.unlock();
		}
		
	}

	/**
	 * Returns the reader lock.
	 *
	 * @return the reader lock
	 */
	public SimpleLock readLock() {
		return readerLock;
	}

	/**
	 * Returns the writer lock.
	 *
	 * @return the writer lock
	 */
	public SimpleLock writeLock() {
		return writerLock;
	}

	/**
	 * Returns the number of active readers.
	 *
	 * @return the number of active readers
	 */
	public int readers() {
		synchronized (lock) {
			return readers;
		}
	}

	/**
	 * Returns the number of active writers.
	 *
	 * @return the number of active writers
	 */
	public int writers() {
		synchronized (lock) {
			return writers;
		}
	}

	/**
	 * Determines whether the thread running this code and the writer thread are in
	 * fact the same thread.
	 *
	 * @return true if the thread running this code and the writer thread are not
	 *         null and are the same thread
	 *
	 * @see Thread#currentThread()
	 */
	public boolean isActiveWriter() {
		synchronized (lock) {
			return Thread.currentThread().equals(activeWriter);
		}
	}

	/**
	 * Used to maintain simultaneous read operations.
	 */
	private class SimpleReadLock implements SimpleLock {
		/**
		 * Controls access to the read lock. The active thread is forced to wait
		 * while there are any active writers and it is not the active writer
		 * thread. Once safe, the thread is allowed to acquire a read lock by
		 * incrementing the number of active readers.
		 */
		@Override
		public void lock() {
			/*
			 * Note: This starts with the basic implementation from lecture (plus some
			 * logging). You will eventually need to modify it to check for whether
			 * there is an active writer.
			 * 
			 * Note: Wanna check this w/ Professor Sophie to make sure this is right
			 */

			log.debug("Acquiring read lock...");

			try {
				synchronized (lock) {
					
					// From https://docs.oracle.com/en/java/javase/15/docs/api/java.base/java/util/concurrent/locks/ReentrantReadWriteLock.WriteLock.html#lock():
					// "If current thread already holds write lock then hold count incremented by one and method returns immediately"
					if ( Thread.currentThread().equals(activeWriter) ) {
						readers++;
						return;
					}
					
					while (writers > 0) {
						log.debug("Waiting for read lock...");
						
						lock.wait();
						
						

					}

					log.debug("Woke up waiting for read lock...");
					assert writers == 0;
					readers++;
				}

				log.debug("Acquired read lock.");
			}
			catch (InterruptedException ex) {
				log.catching(Level.DEBUG, ex);
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Will decrease the number of active readers and notify any waiting threads
		 * if necessary.
		 *
		 * @throws IllegalStateException if no readers to unlock
		 */
		@Override
		public void unlock() throws IllegalStateException {

			synchronized(lock) {
				if (readers == 0) throw new IllegalStateException("trying to unlock read lock but there are 0 readers");
				
				readers--;
				if (readers == 0) {
					lock.notifyAll();
				}
			}
		}
	}

	/**
	 * Used to maintain exclusive write operations.
	 */
	private class SimpleWriteLock implements SimpleLock {
		/**
		 * Controls access to the write lock. The active thread is forced to wait
		 * while there are any active readers or writers, and it is not the active
		 * writer thread. Once safe, the thread is allowed to acquire a write lock
		 * by incrementing the number of active writers and setting the active
		 * writer reference.
		 */
		@Override
		public void lock() {
			
			try {
				synchronized (lock) {
					
					if (Thread.currentThread().equals(activeWriter)) {
						lock.notifyAll();
						writers++;
						return; // same logic as readlock.lock()
							
					}
					
					while (readers + writers > 0 ) {
						lock.wait();
					}
					
					writers++;
					
					activeWriter = Thread.currentThread();
				}
			}
			catch (InterruptedException ex) {
				log.catching(Level.DEBUG, ex);
				Thread.currentThread().interrupt();
			}
			
		}

		/**
		 * Will decrease the number of active writers and notify any waiting threads
		 * if necessary. Also unsets the active writer if appropriate.
		 *
		 * @throws IllegalStateException if no writers to unlock
		 * @throws ConcurrentModificationException if there are writers but unlock
		 *         is called by a thread that does not hold the write lock
		 */
		@Override
		public void unlock() throws IllegalStateException, ConcurrentModificationException {

			synchronized(lock) {
				try {
					if (writers == 0) {
						throw new IllegalStateException("Out of writers");
					}
					else if ( !( Thread.currentThread().equals(activeWriter) ) ) {
						throw new ConcurrentModificationException("Attempting to unlock from wrong writer");
					}
					
					writers--;
					
					if (writers == 0) {
						activeWriter = null;
					}
				}
				finally {
					lock.notifyAll();
				}
			}
		}
	}
}