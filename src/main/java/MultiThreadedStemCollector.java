import java.io.IOException;
import java.nio.file.Path;

/**
 * Multi-threaded implementation of WordStemCollector
 * @author JRRed
 *
 */
public class MultiThreadedStemCollector implements WordStemCollector {
	/** ThreadSafeInvertedIndex to store stems into */
	private final ThreadSafeInvertedIndex threadSafe;
	
	/** Work queue */
	private final WorkQueue queue;
	
	/**
	 * Constructor
	 * @param threadSafe thread safe inverted index
	 * @param queue work queue
	 */
	public MultiThreadedStemCollector(ThreadSafeInvertedIndex threadSafe, WorkQueue queue) {
		this.threadSafe = threadSafe;
		this.queue = queue;
	}
	
	@Override
	public void collectStemsFrom(Path seed) throws IOException {
		WordStemCollector.super.collectStemsFrom(seed);
		queue.finish();
	}
	
	@Override
	public void parseFile(Path path) throws IOException {
		queue.execute( new ParseFileTask(path) );
	}
	
	/**
	 * Class whose sole responsibility is to represent the task: "parse one file, collect the stems into a local index, and merge its contents
	 * into a common index"
	 * @author JRRed
	 *
	 */
	private class ParseFileTask implements Runnable { 
		/** file path */
		private final Path path;
		
		/** local InvertedIndex to store stems into */
		private final InvertedIndex localIndex;
		
		/**
		 * Constructor
		 * @param path path
		 */
		private ParseFileTask(Path path) {
			this.path = path;
			this.localIndex = new InvertedIndex();
		}
		
		@Override
		public void run() {
			try {
				WordStemCollector.parseFile(path, localIndex);
				threadSafe.attemptMergeWith(localIndex);
			}
			catch (Exception e) {
				System.err.println("ERROR - WordStemCollector.ParseFileTask");
			}
		}
	}
	
}