import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Class whose sole responsibility is to search an already-populated inverted index for a set of word stem queries
 * @author JRRed
 *
 */
public class IndexSearcher {

	/** map of search results, organized by their original query set */
	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	
	/** Inverted Index to search from */
	private final InvertedIndex index;
	
	/** Search function to use */
	private Consumer<Collection<String>> searchFunc;
	
	/**
	 * Constructor
	 * @param index index to search from
	 * @param exact whether to use exact search (true) or partial search (false)
	 */
	public IndexSearcher(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		
		if (exact) {
			searchFunc = (stemSet) -> {
				if (stemSet != null && !stemSet.isEmpty()) {
					new OneStemSetSearchTask(stemSet).exactSearch();
				}
			};
		}
		else {
			searchFunc = (stemSet) -> {
				if (stemSet != null && !stemSet.isEmpty()) {
					new OneStemSetSearchTask(stemSet).partialSearch();
				}
			};
		}
	}
	
	/**
	 * General search function. Applies this searcher's search function over a text file
	 * @param queryPath path of query stems
	 * @throws IOException in case of IO error
	 */
	public void search(final Path queryPath) throws IOException {
	
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				searchFunc.accept( TextFileStemmer.uniqueStems(line) );
			}
		}
	}
	
	/**
	 * Outputs search result map to a file, in JSON format
	 * @param path output file path
	 * @throws IOException in case of IO Error
	 */
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}
	
	/**
	 * Inner class whose sole responsibility is to represent a task for searching the index from one set of stems
	 * @author JRRed
	 *
	 */
	private class OneStemSetSearchTask {
		
		/** Lookup map, to make sure we create a given search result only onc e*/
		Map<String, InvertedIndex.SearchResult> lookup;
		
		/** List of search results. @note This is a list, not a treeset, b/c SearchResults are mutable. Any search func will sort this list at the end */
		List<InvertedIndex.SearchResult> results;
		
		/** set of stems to search the index for */
		Collection<String> stemSet;
		
		/**
		 * Constructor
		 * @param stemSet set of stems to search the index for
		 */
		private OneStemSetSearchTask(Collection<String> stemSet) {
			this.lookup = new TreeMap<>();
			this.results = new ArrayList<>();
			this.stemSet = stemSet;
		}
		
		/**
		 * Searches the index for only the stems in the stemset
		 */
		private void exactSearch() {
			updateMatches(stemSet, (pathName, query) -> lookup.get(pathName).update(query));
			Collections.sort(results);
			searchResultMap.put( String.join(" ",  stemSet), results );
		}
		
		/**
		 * Creates a map storing how often a partial stem should be counted in the index
		 * @return A partial stem frequency map
		 */
		private Map<String, Integer> createPartialStemFreqMap() {
			Map<String, Integer> partialStemFreqMap = new TreeMap<>();
			
			for (String stem : stemSet) {
				var it = index.getStringsStartingWith(stem).iterator();
				
				String current;
				while ( it.hasNext() && (current = it.next()).startsWith(stem) ) {
					partialStemFreqMap.compute(current,  (k, v) -> v == null ? 1 : v + 1); // Got this implementation from https://www.baeldung.com/java-word-frequency
				}
			}
			
			return partialStemFreqMap;
		}
		
		/**
		 * Searches the index for all the partial stems in the stem set. Given a stem X in a set of stems,
		 * stem Y is a partial stem of X if Y starts with X. E.g. "yourselv" starts with "your", so
		 * "yourselv" is a partial stem of "your". This function is designed to double-count common partial stems,
		 * e.g. if all the partial stems in a set would be "yourselv, yourself, your, yourselv, yourself", we count it
		 * as five partial stems.
		 */
		private void partialSearch() {
			Map<String, Integer> partialStemFreqMap = createPartialStemFreqMap();
			updateMatches(partialStemFreqMap.keySet(),
					(pathName, query) -> lookup.get(pathName).update(query, partialStemFreqMap.get(query)));
			Collections.sort(results);
			searchResultMap.put( String.join(" ",  stemSet), results );
			
		}
		
		/**
		 * Updates this class's collection of search results based off the query set and update function
		 * @param querySet set of queries (exact queries or partial queries)
		 * @param updateFunc the function to update this class's collection of search results with
		 */
		private void updateMatches(Collection<String> querySet, BiConsumer<String, String> updateFunc) {
			for (String query : querySet) {
				if ( index.contains(query) ) {
					for (String pathName : index.getLocationsContaining(query)) {
						if (!lookup.containsKey(pathName)) {
							InvertedIndex.SearchResult result = index.new SearchResult(pathName);
							lookup.put(pathName, result);
							results.add(result);
						}
						updateFunc.accept(pathName,  query);
					}
				}
			}
		}
	}
}
