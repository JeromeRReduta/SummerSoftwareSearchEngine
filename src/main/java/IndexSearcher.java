import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

public class IndexSearcher {

	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	private final InvertedIndex index;
	private Function<Collection<String>, Collection<String>> stemGettingFunc;
	
	
	public IndexSearcher(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		
		if (exact) {
			stemGettingFunc = stemSet -> stemSet;
		}
		else {
			stemGettingFunc = stemSet -> index.getPartialStemsFrom(stemSet);
		}
		
	}
	
	public void search(final Path queryPath, boolean exact) throws IOException {
	
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				Collection<String> stemSet = TextFileStemmer.uniqueStems(line);
				
				if (stemSet != null && !stemSet.isEmpty()) {
					searchResultMap.put( String.join(" ",  stemSet), index.exactSearch( stemGettingFunc.apply(stemSet) ) );
				}
				
			}
		}
	}
	
	public void exactSearch(Set<String> stemSet) {
		
		// Running: actual\search-exact-guten.json... Elapsed: 3.113000 seconds ( w/o matching stems func)
		// Running: actual\search-exact-guten.json... Elapsed: 3.632000 seconds ( w/ matching stems func)
		 
	}
	
	public void partialSearch(Set<String> stemSet) {
		if (stemSet != null && !stemSet.isEmpty()) {
			searchResultMap.put( String.join(" ",  stemSet), index.exactSearch( index.getPartialStemsFrom(stemSet) ) );
		}
		
		
	}
	
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}

}
