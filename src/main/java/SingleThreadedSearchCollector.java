import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class whose sole responsibility is to search an already-populated inverted index for a set of word stem queries
 * @author JRRed
 *
 */
public class SingleThreadedSearchCollector extends SearchResultCollector {
	/**
	 * Constructor
	 * @param index index to search from
	 * @param exact whether to use exact search (true) or partial search (false)
	 */
	public SingleThreadedSearchCollector(InvertedIndex index, boolean exact) {
		super(index, exact);
	}
	
	/**
	 * General search function. Applies this searcher's search function over a text file
	 * @param queryPath path of query stems
	 * @throws IOException in case of IO error
	 */
	@Override
	public void search(final Path queryPath) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(queryPath, StandardCharsets.UTF_8)) {
			String line;
			
			while ( (line = reader.readLine()) != null ) {
				new SearchLineTask(line).run();
			}
		}
	}
}
