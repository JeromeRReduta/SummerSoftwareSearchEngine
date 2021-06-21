import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class IndexSearcher {

	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	private final InvertedIndex index;
	private Consumer<Collection<String>> searchFunc;
	
	
	public IndexSearcher(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		this.searchFunc = exact ? this::exactSearch : this::partialSearch;
	}
	
	public void search(final Path queryPath, boolean exact) throws IOException {
	
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				searchFunc.accept( TextFileStemmer.uniqueStems(line) );
			}
		}
	}
	
	public void exactSearch(Collection<String> stemSet) {
		if (stemSet != null && !stemSet.isEmpty()) {
			index.exactSearchAndSaveTo(stemSet, searchResultMap);
		}
		// Running: actual\search-exact-guten.json... Elapsed: 3.113000 seconds ( w/o matching stems func)
		// Running: actual\search-exact-guten.json... Elapsed: 3.632000 seconds ( w/ matching stems func)
		 
	}
	
	public void partialSearch(Collection<String> stemSet) {
		if (stemSet != null && !stemSet.isEmpty()) {
			index.partialSearchAndSaveTo(stemSet, searchResultMap);
		}
	}
	
	public void outputToFile(final Path path) throws IOException {
		SearchJsonWriter.asSearchResultMap(searchResultMap, path);
	}

}
