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

public class IndexSearcher {

	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	private final InvertedIndex index;
	
	
	public IndexSearcher(InvertedIndex index) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
	}
	
	public void search(final Path queryPath, boolean exact) throws IOException {
		
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
			searchResultMap.put( String.join(" ",  stemSet), index.exactSearch(stemSet) );
		}
		
		
	}
	
	public void partialSearch(Set<String> stemSet) {
		
	}
	
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}

}
