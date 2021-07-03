import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Class whose sole responsibility is to represent something that searches an InvertedIndex and collects its results into a list,
 * which can then be output into a file. This class has a single-threaded and multi-threaded implementation.
 * @author JRRed
 *
 */
public interface SearchResultCollector {
	/**
	 * Searches a given index, using a given query file, with a given search function,
	 * and saves results to a given search result map
	 * @param path path
	 * @throws IOException in case of IO Error
	 */
	void search(Path path) throws IOException; // TODO <--- this can be made a default implementation
	
	// TODO void search(String line)
	
	/**
	 * Outputs the SearchResultCollector's search result map to a path
	 * @param path path
	 * @throws IOException in case of IO Error
	 */
	void outputToFile(Path path) throws IOException;
	
	/**
	 * Single-threaded implementation of SearchResultCollector
	 * @author JRRed
	 *
	 */
	public class SingleThreaded implements SearchResultCollector {
		/** map of search results, organized by their original query set */
		private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
		
		/** Search function to use */
		private final Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc;
		
		/**
		 * Constructor
		 * @param searchFunc search function to use
		 */
		public SingleThreaded(Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc) {
			this.searchResultMap = new TreeMap<>();
			this.searchFunc = searchFunc;
		}
		
		@Override
		public void search(Path path) throws IOException {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				String line;
				
				while ( (line = reader.readLine()) != null ) {
					TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
					String searchLine = String.join(" ",  uniqueStems);
					if ( uniqueStems.isEmpty() || searchResultMap.containsKey(searchLine) ) return;
					
					searchResultMap.put(searchLine,  searchFunc.apply(uniqueStems) );
					
					// TODO Call method within loop
				}
			}
		}
		
		/* TODO 
		public void search(String line) {
			TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
			String searchLine = String.join(" ",  uniqueStems);
			if ( uniqueStems.isEmpty() || searchResultMap.containsKey(searchLine) ) return;

			searchResultMap.put(searchLine,  searchFunc.apply(uniqueStems) );
		}
		*/
		
		@Override
		public void outputToFile(Path path) throws IOException {
			SearchJsonWriter.asSearchResultMap(searchResultMap, path);
		}
	}
	
	/**
	 * Multi-threaded implementation of SearchResultCollector
	 * @author JRRed
	 *
	 */
	public class MultiThreaded implements SearchResultCollector {
		/** map of search results, organized by their original query set */
		private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
		
		/** Search function to use */
		private final Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc;
		
		/** WorkQueue */
		private final WorkQueue queue;
		
		/**
		 * Constructor
		 * @param searchFunc search function to use
		 * @param queue work queue
		 */
		public MultiThreaded(Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc, WorkQueue queue) {
			this.searchResultMap = new TreeMap<>();
			this.searchFunc = searchFunc;
			this.queue = queue;
		}
		
		@Override
		public void search(Path path) throws IOException {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				String line;
				
				while ( (line = reader.readLine()) != null ) {
					queue.execute( new SearchLineTask(line) );

				}
				queue.join(); // TODO queue.finish();
			}
			
			/* TODO 
			SearchResultCollector.super.search(path);
			queue.finish();
			*/
		}
		
		/* TODO 
		public void search(String line) {
			queue.execute( new SearchLineTask(line) );
		}
		*/
		
		@Override
		public void outputToFile(Path path) throws IOException {
			// TODO synchronized (searchResultMap)
			SearchJsonWriter.asSearchResultMap(searchResultMap, path);
		}
		
		/**
		 * Class whose sole responsibility is to represent the task: "Search the given ThreadSafeInvertedIndex,
		 * using the given line of queries, and put the results into the given search result map.
		 * @author JRRed
		 *
		 */
		private class SearchLineTask implements Runnable {
			/** line to search */
			private final String line;
			
			/**
			 * Constructor
			 * @param line line to search
			 */
			private SearchLineTask(String line) {
				this.line = line;
			}
			
			@Override
			public void run() {
				TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
				String searchLine = String.join(" ",  uniqueStems);
				
				synchronized(searchResultMap) {
					if ( uniqueStems.isEmpty() || searchResultMap.containsKey(searchLine) ) return;
				}
				
				Collection<InvertedIndex.SearchResult> results = searchFunc.apply(uniqueStems);
				
				synchronized(searchResultMap) {
					searchResultMap.put(searchLine,  results);
				}
			}
		}
	}
}
