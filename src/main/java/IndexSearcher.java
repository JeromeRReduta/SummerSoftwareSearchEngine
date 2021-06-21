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


public class IndexSearcher {

	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	private final InvertedIndex index;
	private Consumer<Collection<String>> searchFunc;
	
	
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
	
	public void search(final Path queryPath, boolean exact) throws IOException {
	
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				searchFunc.accept( TextFileStemmer.uniqueStems(line) );
			}
		}
	}
	
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}
	
	
	
	
	private class OneStemSetSearchTask {
		
		Map<String, InvertedIndex.SearchResult> lookup;
		List<InvertedIndex.SearchResult> results;
		Collection<String> stemSet;
		
		private OneStemSetSearchTask(Collection<String> stemSet) {
			this.lookup = new TreeMap<>();
			this.results = new ArrayList<>();
			this.stemSet = stemSet;
		}
		
		private void exactSearch() {
			updateMatches(stemSet, (pathName, query) -> lookup.get(pathName).update(query));
		}
		
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
		
		private void partialSearch() {
			Map<String, Integer> partialStemFreqMap = createPartialStemFreqMap();
			updateMatches(partialStemFreqMap.keySet(),
					(pathName, query) -> lookup.get(pathName).update(query, partialStemFreqMap.get(query)));
			
		}
		
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
			
			Collections.sort(results);
			searchResultMap.put( String.join(" ",  stemSet), results );
		}

	}
	

}
