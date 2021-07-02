import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class whose sole responsibility is to collect word stems from a file and put them into an inverted index
 * @author JRRed
 *
 */
public interface WordStemCollector {
	/**
	 * Collects stems from a file or directory path and stores them in its invertedIndex
	 * @param seed file or directory path
	 * @throws IOException in case of IO Error
	 */
	void collectStemsFrom(Path seed) throws IOException;
	
	/**
	 * Parses stems from one file and collects them to an InvertedIndex
	 * @param path path
	 * @param index InvertedIndex
	 * @throws IOException in case of IOError
	 */
	static void parseFile(Path path, InvertedIndex index) throws IOException {
		Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		String location = path.toString();
		int position = 1;
		
		try ( BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8) ) {
			String line;
			while ( (line = reader.readLine()) != null ) {
				String[] parsedLine = TextParser.parse(line);
				
				for (String word : parsedLine) {
					index.add( stemmer.stem(word).toString(), location, position++ );
				}
			}
		}
	}
	
	/**
	 * Single-threaded implementation of WordStemCollector
	 * @author JRRed
	 *
	 */
	public class SingleThreaded implements WordStemCollector {
		/** InvertedIndex to store word stems into */
		private final InvertedIndex index;
		
		/**
		 * Constructor
		 * @param index InvertedIndex
		 */
		public SingleThreaded(InvertedIndex index) {
			this.index = index;
		}
		
		@Override
		public void collectStemsFrom(Path seed) throws IOException {
			if (Files.isDirectory(seed)) { // Case: Directory - call parseFile() for each text file in directory
				List<Path> filePaths = TextFileFinder.list(seed);
				
				for (Path filePath : filePaths) { // Case: one file - call parseFile() just for this file
					parseFile(filePath, index);
				}
			}
			else if (Files.isRegularFile(seed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
				parseFile(seed, index);
			}
		}
	}
	
	/**
	 * Multi-threaded implementation of WordStemCollector
	 * @author JRRed
	 *
	 */
	public class MultiThreaded implements WordStemCollector {
		/** ThreadSafeInvertedIndex to store stems into */
		private final ThreadSafeInvertedIndex threadSafe;
		
		/** Work queue */
		private final WorkQueue queue;
		
		/**
		 * Constructor
		 * @param threadSafe thread safe inverted index
		 * @param queue work queue
		 */
		public MultiThreaded(ThreadSafeInvertedIndex threadSafe, WorkQueue queue) {
			this.threadSafe = threadSafe;
			this.queue = queue;
		}
		
		@Override
		public void collectStemsFrom(Path seed) throws IOException {
			if (Files.isDirectory(seed)) { // Case: Directory - call parseFile() for each text file in directory
				List<Path> filePaths = TextFileFinder.list(seed);
				
				for (Path filePath : filePaths) { // Case: one file - call parseFile() just for this file
					queue.execute( new ParseFileTask(filePath) );
				}
			}
			else if (Files.isRegularFile(seed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
				queue.execute( new ParseFileTask(seed) );
			}
			queue.finish();
		}
		
		/**
		 * Class whose sole responsibility is to represent the task: "parse one file, collect the stems into a local index, and merge its contents
		 * into a common index"
		 * @author JRRed
		 *
		 */
		private class ParseFileTask extends Thread {
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
					parseFile(path, localIndex);
					threadSafe.attemptMergeWith(localIndex);
				}
				catch (Exception e) {
					System.err.println("ERROR - WordStemCollector.ParseFileTask");
				}
			}
		}
		
	}
}
