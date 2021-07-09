import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Class whose sole responsibility is to represent something that searches an InvertedIndex and collects its results into a list,
 * which can then be output into a file. This class has a simple, single-threaded, implementation that has been provided by default.
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
	default void search(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
      
			while ( (line = reader.readLine()) != null ) {
				searchLine(line);
			}
		}
	}
	
	/**
	 * Searches a given index, using a given line of queries, with a given search function,
	 * and saves results to a given search result map
	 * @param line line of queries
	 */
	void searchLine(String line);
	
	/**
	 * Outputs the SearchResultCollector's search result map to a path
	 * @param path path
	 * @throws IOException in case of IO Error
	 */
	void outputToFile(Path path) throws IOException;
	
	/**
	 * Outputs the SearchResultCollector's search result map in a web-friendly JSON format
	 * @param start Instant, for timing the search
	 * @return The SearchResultCollector's search result map in a web-friendly JSON format
	 */
	String outputToWeb(Instant start);
	
	/**
	 * Single-threaded implementation of SearchResultCollector
	 * @author JRRed
	 *
	 */
	public class Default implements SearchResultCollector {
		/** map of search results, organized by their original query set */
		private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
		
		/** Search function to use */
		private final Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc;
		
		/**
		 * Constructor
		 * @param searchFunc search function to use
		 */
		public Default(Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc) {
			this.searchResultMap = new TreeMap<>();
			this.searchFunc = searchFunc;
		}
		
		@Override
		public void searchLine(String line) {
			TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
			String searchLine = String.join(" ",  uniqueStems);
			if ( uniqueStems.isEmpty() || searchResultMap.containsKey(searchLine) ) return;
			
			searchResultMap.put(searchLine,  searchFunc.apply(uniqueStems) );
		}
		
		@Override
		public void outputToFile(Path path) throws IOException {
			SearchJsonWriter.asSearchResultMap(searchResultMap, path);
		}
		
		@Override
		public String outputToWeb(Instant start) {
			return SearchJsonWriter.asWebResults(searchResultMap, start);
		}
	}
}