import java.io.IOException;
import java.nio.file.Path;

/**
 * Class whose sole responsibility is to represent a search engine, with an InvertedIndex for storing data, a WordStemCollector for populating that index,
 * and an SearchResultCollector for searching that index.
 * @author JRRed
 *
 */
public class SearchEngine {
	private final String seed;
	
	/** Inverted index for storing data */
	private final InvertedIndex index;
	
	/** WordStemCollector for collecting stems and storing them into the index */
	private final WordStemCollector stemCollector;
	
	/** SearchResultCollector to search index with */
	private final SearchResultCollector searcher;

	/** Work queue. Will be shared among all the data structures this search engine uses */
	private final WorkQueue queue;
	
	private SearchEngine(String seed, InvertedIndex index, WorkQueue queue, WordStemCollector stemCollector, SearchResultCollector searcher) {
		this.seed = seed;
		this.index = index;
		this.queue = queue;
		this.stemCollector = stemCollector;
		this.searcher = searcher;
		
	}
	
	public static class Factory {
		public static SearchEngine create(ArgumentMap argMap) {
			if (argMap.hasFlag("-html")) {
				return createWeb(argMap);
			}
			else if (argMap.hasFlag("-threads")) {
				return createMultiThreaded(argMap);
			}
			return createSingleThreaded(argMap);
			
		}
		
		private static SearchEngine createWeb(ArgumentMap argMap) {
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			WorkQueue queue = new WorkQueue(argMap.getInteger("-threads", WorkQueue.DEFAULT));
			
			return new SearchEngine(
					argMap.getString("-html"),
					threadSafe,
					queue,
					new WebCrawler(threadSafe, queue, argMap.getInteger("-max", 1)),
					new MultiThreadedSearchCollector(argMap.hasFlag("-exact") ? threadSafe::exactSearch : threadSafe::partialSearch,
							queue));
		}
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
	 * Constructor
	 * @param argMap Argument map for parsing arguments
	 * @note Putting argMap here and not just the exact boolean for future-proofing for P3 and P4, when I will also have to deal with threads and crawling maybe
	 */
	public SearchEngine(ArgumentMap argMap) {
		if (argMap.hasFlag("-html")) {
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			this.queue = new WorkQueue(argMap.getInteger("-threads", WorkQueue.DEFAULT));
			this.index = threadSafe;
			this.seed = argMap.getString("-html");
			
			this.stemCollector = new WebCrawler(threadSafe, queue, argMap.getInteger("-max",  1));
			this.searcher = new MultiThreadedSearchCollector(
					argMap.hasFlag("-exact") ? threadSafe::exactSearch : threadSafe::partialSearch,
					queue);
			
		}
		else if (argMap.hasFlag("-threads")) {
			ThreadSafeInvertedIndex threadSafe = new ThreadSafeInvertedIndex();
			this.queue = new WorkQueue(argMap.getInteger("-threads", WorkQueue.DEFAULT));
			this.index = threadSafe;
			this.seed = argMap.getString("-text");
			
			this.stemCollector = new MultiThreadedStemCollector(threadSafe, queue); 
			
			this.searcher = new MultiThreadedSearchCollector(
					argMap.hasFlag("-exact") ? threadSafe::exactSearch : threadSafe::partialSearch,
					queue);
		}
		else {
			this.queue = null;
			this.index = new InvertedIndex();
			this.seed = argMap.getString("-text");
			this.stemCollector = new WordStemCollector.Default(index);
			
			this.searcher = new SearchResultCollector.Default(
					argMap.hasFlag("-exact") ? this.index::exactSearch : this.index::partialSearch);
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
	
	public void getStems() throws IOException {
		stemCollector.collectStemsFrom(seed);
	}
	
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
	 * If the search engine contains a work queue, runs queue.join()
	 */
	public final void joinQueue() {
		if (queue != null) queue.join();
	}
}