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
 * Class whose sole responsibility is to search an already-populated inverted index for a set of word stem queries
 * @author JRRed
 *
 */
public class SearchResultCollector {

	/** map of search results, organized by their original query set */
	private final Map<String, Collection<InvertedIndex.SearchResult>> searchResultMap;
	
	/** Inverted Index to search from */
	private final InvertedIndex index;
	
	/** Search function to use */
	private final Function<Set<String>, Collection<InvertedIndex.SearchResult>> searchFunc;
	
	/**
	 * Constructor
	 * @param index index to search from
	 * @param exact whether to use exact search (true) or partial search (false)
	 */
	public SearchResultCollector(InvertedIndex index, boolean exact) {
		this.index = index;
		this.searchResultMap = new TreeMap<>();
		this.searchFunc = exact ? this.index::exactSearch : this.index::partialSearch;
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
				searchIfNecessary(line);
			}
		}
	}
	
	/**
	 * If the given line can be turned into stems, and the search result map hasn't already searched this string, searches the stems. Else, does nothing.
	 * @param line line
	 */
	public void searchIfNecessary(String line) {
		TreeSet<String> uniqueStems = TextFileStemmer.uniqueStems(line);
		String searchLine = String.join(" ", uniqueStems);
		boolean needToSearch = !uniqueStems.isEmpty() && !searchResultMap.containsKey(searchLine);
		if ( needToSearch ) {
			searchResultMap.put( searchLine,  searchFunc.apply(uniqueStems) );
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
}
