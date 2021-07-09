import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Multi-threaded implementation of SearchResultCollector
 * @author JRRed
 *
 */
public class MultiThreadedSearchCollector implements SearchResultCollector {
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
	public MultiThreadedSearchCollector(Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc, WorkQueue queue) {
		this.searchResultMap = new TreeMap<>();
		this.searchFunc = searchFunc;
		this.queue = queue;
	}
	
	@Override
	public void search(Path path) throws IOException {
		SearchResultCollector.super.search(path); // SearchResultCollector.super.methodName calls the static methodName() from SearchResultCollector, the "super" of this class's interface
		queue.finish();
	}
	
	@Override
	public void searchLine(String line) {
		queue.execute( new SearchLineTask(line) );
	}
	
	@Override
	public void outputToFile(Path path) throws IOException {
		synchronized(searchResultMap) {
			SearchJsonWriter.asSearchResultMap(searchResultMap, path);
		}
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