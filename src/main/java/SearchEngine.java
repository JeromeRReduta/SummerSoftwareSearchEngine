import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Class whose sole responsibility is to represent a search engine, with an InvertedIndex for storing data, a WordStemCollector for populating that index,
 * and an SearchResultCollector for searching that index.
 * @author JRRed
 *
 */
public class SearchEngine {
	
	/**
	 * Seed string; usually a file path, directory path, or URL. This search engine will start parsing from this seed string.
	 */
	private final String seed;
	
	/** Inverted index for storing data */
	private final InvertedIndex index;
	
	/** StemCrawler for collecting stems and storing them into the index */
	private final StemCrawler collector;
	
	/** SearchResultCollector to search index with */
	private final SearchResultCollector searcher;

	/** Work queue. Will be shared among all the data structures this search engine uses */
	private final WorkQueue queue;
	
	/**
	 * Constructor
	 * @param seed seed
	 * @param index index
	 * @param queue Work Queue
	 * @param collector Stem Crawler
	 * @param searcher Search Result Collector
	 */
	private SearchEngine(String seed, InvertedIndex index, WorkQueue queue, StemCrawler collector, SearchResultCollector searcher) {
		this.seed = seed;
		this.index = index;
		this.queue = queue;
		this.collector = collector;
		this.searcher = searcher;
		
	}
	
	/**
	 * Factory pattern
	 * @author JRRed
	 *
	 */
	public static class Factory {
		
		/**
		 * Creates a Search Engine, built based off what's in the ArgumentMap
		 * @param argMap ArgumentMap
		 * @return a Search Engine, built based off what's in the ArgumentMap
		 */
		public static SearchEngine create(ArgumentMap argMap) {
			if (argMap.hasFlag("-html")) {
				return createWeb(argMap);
			}
			else if (argMap.hasFlag("-threads")) {
				return createMultiThreaded(argMap);
			}
			return createSingleThreaded(argMap);
			
		}
		
		/**
		 * Creates a Search Engine with a web crawler. Multi-threaded by default.
		 * @param argMap ArgumentMap
		 * @return A Search Engine with a web crawler, multi-threaded by default.
		 */
		private static SearchEngine createWeb(ArgumentMap argMap) {
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(argMap.getInteger("-threads", WorkQueue.DEFAULT));
			
			System.out.println("MAX IS: " + argMap.getInteger("-max", 1));
			System.out.println("USING PARTIAL SEARCH: " + !argMap.hasFlag("-exact"));
			return new SearchEngine(
					argMap.getString("-html"),
					threadSafe,
					queue,
					new WebCrawler(threadSafe, queue, argMap.getInteger("-max", 1)),
					new MultiThreadedSearchCollector(argMap.hasFlag("-exact") ? threadSafe::exactSearch : threadSafe::partialSearch,
							queue));
		}
		
		/**
		 * Creates a multi-threaded search engine.
		 * @param argMap ArgumentMap
		 * @return A multi-threaded search engine
		 */
		private static SearchEngine createMultiThreaded(ArgumentMap argMap) {
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(argMap.getInteger("-threads", WorkQueue.DEFAULT));
			
			return new SearchEngine(
					argMap.getString("-text"),
					threadSafe,
					queue,
					new MultiThreadedStemCollector(threadSafe, queue),
					new MultiThreadedSearchCollector(argMap.hasFlag("-exact") ? threadSafe::exactSearch : threadSafe::partialSearch,
							queue));
		}
		
		/**
		 * Creates a single-threaded search engine
		 * @param argMap ArgumentMap
		 * @return A single-threaded search engine
		 */
		private static SearchEngine createSingleThreaded(ArgumentMap argMap) {
			InvertedIndex index = new InvertedIndex();
			
			return new SearchEngine(
					argMap.getString("-text"),
					index,
					null,
					new WordStemCollector.Default(index),
					new SearchResultCollector.Default(argMap.hasFlag("-exact") ? index::exactSearch : index::partialSearch));
		}
	}
	
	/**
	 * Gets stems based off the seed
	 * @throws IOException in case of IO Error
	 */
	public void getStems() throws IOException {
		collector.collectStemsFrom(seed);
	}
	
	/**
	 * Returns the seed this search engine uses
	 * @return The seed this search engine uses
	 */
	public String getSeed() {
		return this.seed;
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
	 * Searches the engine's index with one line of queries. Mainly used for the Web Search Engine (on servers).
	 * @param line line of queries
	 */
	public void searchFrom(String line) {
		searcher.searchLine(line);
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
	
	/**
	 * Outputs the search engine's search results (in a web-friendly JSON format)
	 * @param start Instant for timing how long this search engine's search takes
	 * @return The search engine's search results (in a web-friendly JSON format)
	 */
	public String outputResultsToWeb(Instant start) {
		return searcher.outputToWeb(start);
	}
	
	/**
	 * If the search engine contains a work queue, runs queue.join()
	 */
	public final void joinQueue() {
		if (queue != null) queue.join();
	}
}
