import java.io.IOException;
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
public abstract class SearchResultCollector {
	
	/** map of search results, organized by their original query set */
	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	
	/** Inverted Index to search from */
	private final InvertedIndex index;
	
	/** Search function to use */
	private final Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc;
	
	/**
	 * Constructor
	 * @param index InvertedIndex
	 * @param exact whether to use exact search
	 */
	public SearchResultCollector(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		this.searchFunc = exact ? this.index::exactSearch : this.index::partialSearch;
	}
	
	/**
	 * Searches a file of query stems and adds its results onto a map, if necessary. How that search is done and how
	 * the collector determines when searching is necessary is implementation-specific.
	 * @param queryPath path of query file
	 * @throws IOException in case of IO Error
   */
	public abstract void search(final Path queryPath) throws IOException;
	
	/**
	 * Outputs search result map to a file, in JSON format
	 * @param path output file path
	 * @throws IOException in case of IO Error
	 */
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}
	
	/**
	 * Class whose sole responsibility is to represent the task: "Given a line, decide if it is necessary
	 * to search the index. If it is, search the index and add the results to a common map"
	 * @author JRRed
	 *
	 */
	public class SearchLineTask extends Thread {
		/** line to search */
		private String line;
		
		/**
		 * Constructor
		 * @param line line to search
		 */
		public SearchLineTask(String line) {
			this.line = line;
		}
		
		@Override
		public void run() {
			TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
			String searchLine = String.join(" ", uniqueStems);
			
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
