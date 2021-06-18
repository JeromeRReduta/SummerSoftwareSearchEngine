import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class IndexSearcher {

	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	private final InvertedIndex index;
	private Function<String, Stream<String>> stemGettingFunc;
	
	
	public IndexSearcher(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		this.stemGettingFunc = null; // exact ? index::getStemsMatching : index::getStemsStartingWith;
		
	}
	
	public void search(final Path queryPath, boolean exact) throws IOException {
		System.out.println("EXACT IS: " + exact);
		Consumer<Set<String>> searchFunc = exact ? this::exactSearch : this::partialSearch;
	
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				searchFunc.accept( TextFileStemmer.uniqueStems(line) );
				
			}
		}
	}
	
	public void exactSearch(Set<String> stemSet) {
		if (stemSet != null && !stemSet.isEmpty()) {
			searchResultMap.put( String.join(" ",  stemSet), index.exactSearch(stemSet));
		}
		// Running: actual\search-exact-guten.json... Elapsed: 3.113000 seconds ( w/o matching stems func)
		// Running: actual\search-exact-guten.json... Elapsed: 3.632000 seconds ( w/ matching stems func)
		 
	}
	
	public void partialSearch(Set<String> stemSet) { // TODO: Figure out how to get partial stems (current implementation doesn't work)
		if (stemSet != null && !stemSet.isEmpty()) {
			searchResultMap.put( String.join(" ",  stemSet), index.exactSearch( index.getPartialStemsFrom(stemSet) ) );
		}
		
		
	}
	
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}

}
