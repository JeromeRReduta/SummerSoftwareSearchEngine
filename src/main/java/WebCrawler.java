import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
	
	/** list of links */
	private final List<String> links;
	
	/** Thread-Safe Inverted Index */
	private final ThreadSafeInvertedIndex index;
	
	/** work queue */
	private final WorkQueue queue;
	
	/** max num of urls to crawl */
	private final int max;
	
	/**
	 * Constructor
	 * @param index index
	 * @param queue queue
	 * @param max max num of urls to crawl
	 */
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.links = new ArrayList<>();
		this.lookup = new HashSet<>();
	}
	
	/**
	 * Adds unique links to queue
	 * @param validLinks list of links
	 */
	public synchronized void crawlUniqueLinks(List<URL> validLinks) {
		for (URL link : validLinks) {
			if (links.size() == max) break;
			String linkName = link.toString();

			if (!lookup.contains(linkName)) {
				lookup.add(linkName);
				links.add(linkName);
				
				queue.execute( new CrawlURLTask(linkName) );
			}
		}
	}
	
	/**
	 * Represents task: "Crawl URLs for stems, starting from seed URL, until the max num of URLs have been reached
	 * @author JRRed
	 *
	 */
	public class CrawlURLTask implements Runnable {
		
		/** link name */
		private final String linkName;
		
		/** html block */
		private String html;
	
		/** Inverted Index */
		private final InvertedIndex localIndex;
		
		/**
		 * Constructor
		 * @param linkName link name
		 * @param html html
		 */
		public CrawlURLTask(String linkName) {
			this.linkName = linkName;
			this.localIndex = new InvertedIndex();
		}
		
		@Override
		public void run() {
			String html = HtmlFetcher.fetch(linkName, 3);
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
			
			int position = 1;
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
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
		
		links.add(seed);
		lookup.add(seed);
		queue.execute(new CrawlURLTask(seed));
		
		queue.finish();
		
	}
}
