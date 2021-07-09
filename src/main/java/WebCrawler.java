import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * A class whose sole responsibility is to represent a web crawler: something that parses a seed URL and the URLs it holds, and adds its stems to an Inverted Index
 * @author JRRed
 *
 */
public class WebCrawler implements StemCrawler {
	/** A set used to keep track of unique links */
	private final Set<String> lookup;
	
	/** Thread-safe Inverted Index */
	private final ThreadSafeInvertedIndex index;
	
	/** Work queue */
	private final WorkQueue queue;
	
	/** Max num of unique URLS to crawl */
	private final int max;
	
	/**
	 * Constructor
	 * @param index Thread-Safe InvertedIndex
	 * @param queue work queue
	 * @param max max num of unique urls to crawl
	 */
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.lookup = new HashSet<>();
	}
	
	/**
	 * Given a list of URLs, attempts to add each unique URL to the lookup set, until either we get through the list or we reach the max num of links
	 * @param validLinks list of valid URLs
	 */
	public synchronized void crawlUniqueLinks(List<URL> validLinks) {
		for (URL link : validLinks) {
			if ( lookup.size() == max ) break;

			if ( !lookup.contains( link.toString() ) ) {
				lookup.add( link.toString() );
				queue.execute( new CrawlURLTask( link.toString(), HtmlFetcher.fetch(link, 3) ) );
			}
		}
	}
	
	/**
	 * Represents the task: "Parse html, then add unique links (up to the max number of links), then parse the html more,
	 * then add the parsed html into the local index, then merge the results of the local index into the common one.
	 * @author JRRed
	 *
	 */
	public class CrawlURLTask implements Runnable {
		/** link string */
		private final String linkName;
		
		/** Entire html block, saved in memory */
		private String html;
		
		/** An InvertedIndex to store results into */
		private final InvertedIndex localIndex;
		
		/**
		 * Constructor
		 * @param linkName link name
		 * @param html html block
		 */
		public CrawlURLTask(String linkName, String html) {
			this.linkName = linkName;
			this.html = html;
			this.localIndex = new InvertedIndex();
		}
		
		@Override
		public void run() {
			if (html == null) return;
			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);
			
			try {
				List<URL> validLinks = LinkParser.getValidLinks(new URL(linkName), html);
				crawlUniqueLinks(validLinks);
			}
			catch (Exception e) {
				System.out.println("Uhoh - WebCrawlerTask");
			}
			
			html = HtmlCleaner.stripTags(html);// strip tags before strip entities, or else "<normal text, not tag>" will be counted and removed
			html = HtmlCleaner.stripEntities(html);
			String[] parsedHtml = TextParser.parse(html);
			
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			int position = 1;
			for (String word : parsedHtml) {
				String stemmed = stemmer.stem( word.toLowerCase() ).toString();
				localIndex.add(stemmed, linkName, position++);
			}
			index.attemptMergeWith(localIndex);
		}
	}

	@Override
	public void collectStemsFrom(String seed) throws IOException {
		String html = HtmlFetcher.fetch(seed, 3);
		if (html == null) return;
		
		lookup.add(seed);
		queue.execute(new CrawlURLTask(seed, html));
		
		queue.finish();
	}
}
