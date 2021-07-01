import java.io.IOException;
import java.nio.file.Path;

/**
 * Class whose sole responsibility is to represent a search engine, with an InvertedIndex for storing data, a WordStemCollector for populating that index,
 * and an SearchResultCollector for searching that index.
 * @author JRRed
 *
 */
public class SearchEngine {
	/** Inverted index for storing data */
	private final InvertedIndex index;
	
	/** WordStemCollector for collecting stems and storing them into the index */
	private final WordStemCollector stemCollector;
	
	/** SearchResultCollector to search index with */
	private final SearchResultCollector searcher;
	
	/**
	 * Constructor
	 * @param argMap Argument map for parsing arguments
	 * @note Putting argMap here and not just the exact boolean for future-proofing for P3 and P4, when I will also have to deal with threads and crawling maybe
	 */
	public SearchEngine(ArgumentMap argMap) {
		
		if (argMap.hasFlag("-html")) {
			int threads = argMap.getInteger("-threads", WorkQueue.DEFAULT);
			int safeThreads = threads > 0 ? threads : WorkQueue.DEFAULT;
			
			System.out.println("Threads: " + safeThreads);
			WorkQueue queue = new WorkQueue(safeThreads);
			
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			this.index = threadSafe;
			this.stemCollector = new WebCrawler(threadSafe, queue, argMap.getInteger("-max", 1));
			this.searcher = new MultiThreadedSearchCollector(threadSafe, argMap.hasFlag("-exact"), queue);
			
		}
		else if (argMap.hasFlag("-threads")) {
			WorkQueue queue = new WorkQueue( argMap.getInteger("-threads", WorkQueue.DEFAULT) );
			this.index = new ThreadSafeInvertedIndex();
			assert this.index instanceof ThreadSafeInvertedIndex;
			
			ThreadSafeInvertedIndex threadSafe = (ThreadSafeInvertedIndex)this.index; // TODO downcast
			
			/* TODO 
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			this.index = threadSafe;
			*/
			
			this.stemCollector = new MultiThreadedStemCollector(threadSafe, queue);
			this.searcher = new MultiThreadedSearchCollector(threadSafe, argMap.hasFlag("-exact"), queue);
		}
		else {
			this.index = new InvertedIndex();
			this.stemCollector = new SingleThreadedStemCollector( this.index );
			this.searcher = new SingleThreadedSearchCollector( index, argMap.hasFlag("-exact") );
		}
	}
	
	/**
	 * Parses files from a directory or file path
	 * @param seed a directory or file path
	 * @throws IOException in case of IO Error
	 */
	public void parseFilesFrom(String seed) throws IOException {
		stemCollector.collectStemsFrom(seed);
	}
	
	/**
	 * Searches the engine's index using a search query file 
	 * @param queryPath query file path
	 * @throws IOException in case of IO Error
	 */
	public void searchFrom(Path queryPath) throws IOException {
		searcher.search(queryPath);
	}
	
	
	/**
	 * Outputs the search engine's Inverted Index (in JSON format) to an output file
	 * @param path output file path
	 * @throws IOException in case of IO Error
	 */
	public void outputIndexTo(Path path) throws IOException {
		index.toJson(path);
	}
	
	/**
	 * Outputs the search engine's word counts (in JSON format) to an output file
	 * @param path output file path
	 * @throws IOException in case of IO Error
	 */
	public void outputWordCountsTo(Path path) throws IOException {
		index.countsToJson(path);
	}
	
	/**
	 * Outputs the search engine's search results (in JSON format) to an output file
	 * @param path output file path
	 * @throws IOException in case of IO Error
	 */
	public void outputResultsTo(Path path) throws IOException {
		searcher.outputToFile(path);
	}
}
