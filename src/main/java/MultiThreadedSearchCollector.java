import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Multi-threaded implementation of the search result collector. Uses a work queue.
 * @author JRRed
 *
 */
public class MultiThreadedSearchCollector extends SearchResultCollector {
	/** work queue */
	private final WorkQueue queue;
	
	/**
	 * Constructor
	 * @param threadSafe thread-safe inverted index
	 * @param exact whether to use exact or partial search
	 * @param queue work queue
	 */
	public MultiThreadedSearchCollector(ThreadSafeInvertedIndex threadSafe, boolean exact, WorkQueue queue) {
		super(threadSafe, exact);
		this.queue = queue;
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
				queue.execute( new SearchLineTask(line) );
			}
			queue.finish();
		}
	}
}
