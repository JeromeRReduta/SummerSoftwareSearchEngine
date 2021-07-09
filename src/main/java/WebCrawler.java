import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

public class WebCrawler implements WordStemCollector {
	private static Logger log = LogManager.getLogger();
	
	private final SimpleReadWriteLock lock;
	
	private final Set<String> lookup;
	private final List<String> links;
	private final ThreadSafeInvertedIndex index;
	private final WorkQueue queue;
	private final int max;
	private volatile int current = 0;
	
	// TODO: single-threaded implementation for debugging - get workqueue version working after you confirm this one works
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.links = new ArrayList<>();
		this.lookup = new HashSet<>();
		this.lock = new SimpleReadWriteLock();
	}
	
	public synchronized void crawlUniqueLinks(List<URL> validLinks) {
		for (URL link : validLinks) {
			if (links.size() == max) break;

			if (!lookup.contains(link.toString())) {
				lookup.add(link.toString());
				links.add(link.toString());
				
				queue.execute(new BetterCrawlURLTask( link.toString(), HtmlFetcher.fetch(link, 3) )); // TODO: Take this out of loop? idk
			}
		}
	}
	
	public class BetterCrawlURLTask implements Runnable {
		
		private final String linkName;
		// html may be null
		private String html;
		private int position;
		private final InvertedIndex localIndex;
		
		
		public BetterCrawlURLTask(String linkName, String html) {
			this.linkName = linkName;
			this.html = html;
			this.position = 1;
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
		queue.execute(new BetterCrawlURLTask(seed, html));
		
		queue.finish();
		
	}

	@Override
	public void parseFile(Path path) throws IOException {
		// TODO: add parseFile stuff to here
	}

}
