import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

public class WebCrawler extends WordStemCollector {
	private static Logger log = LogManager.getLogger();
	
	private final Set<URL> lookup;
	private final List<URL> links;
	private final ThreadSafeInvertedIndex index;
	private final WorkQueue queue;
	private final int max;
	
	// TODO: single-threaded implementation for debugging - get workqueue version working after you confirm this one works
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		super(index);
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.links = new ArrayList<>();
		this.lookup = new HashSet<>();
	}
	
	public class CrawlURLTask extends Thread {
		
		private String linkName;
		private String html;
		private final List<URL> linksFound;
		private int position;
		private final InvertedIndex commonIndex;
		
		public CrawlURLTask(String link) throws MalformedURLException {
			
			synchronized(lookup) {
				links.add(new URL(link));
				lookup.add(new URL(link));
			}
				
				
			this.linkName = link;
			this.html = processHtmlFrom(link);
			this.linksFound = this.html != null ? LinkParser.getValidLinks(new URL(link), this.html)
					: null;
			
			this.position = 1;
			this.commonIndex = new InvertedIndex();
		}
		
		public String processHtmlFrom(String link) {
			String html = HtmlFetcher.fetch(link, 3); // Supposed to do 3 redirects
			
			if (html == null) return null;
			
			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);
			return html;
		}
		
		@Override
		public void run() {
			if (html == null) return;
			
			try {
				for (URL link : linksFound) {
					
					synchronized(lookup) {
						if (links.size() == max) {
							break;
						}
						if (lookup.contains(link)) continue;
					}
					queue.execute(new CrawlURLTask(link.toString()));
				}
				
				html = HtmlCleaner.stripTags(html);// strip tags before strip entities, or else "<normal text, not tag>" will be counted and removed
				html = HtmlCleaner.stripEntities(html);
				
				
				String[] parsedHtml = TextParser.parse(html);
				
				Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
				for (String word : parsedHtml) {
					String stemmed = stemmer.stem( word.toLowerCase() ).toString();
					if (stemmed.isBlank()) continue;
					commonIndex.add(stemmed, linkName, position++);
				}
			
				index.attemptMergeWith(commonIndex);
			}
			catch(Exception e) {
				System.out.println("CrawlURLTask - something messed up ");
				e.printStackTrace();
			}
		
		}
	}
	
	/* Steps:
	 * 1. Fetch w/ 3 redirects
	 * 		- If you can't fetch it (i.e. not 200 OK or not HTML) then do nothing
	 * 		- If you can fetch it, set String location =  original html link
	 * 
	 * 2. Strip HTML comments & block elements
	 * 
	 * 3. Find all URLS w/ LinkParser and save to ArrayList called links
	 * 4. For each UNIQUE link found, as long as max # of links hasn't been reached, create new task for crawling
	 * 5. Unescape html, remove html entities, remove rest of tags (including <a tags>, which will be done in same method)
	 * 6. Then stem and add text to index (use location = original html link)
	 */
	
	/*
	public void crawlFrom(String seed) {
		try {
			this.links.add(new URL(seed));
			lookup.add(new URL(seed));
			
			String html = HtmlFetcher.fetch(seed, 3); // Supposed to do 3 redirects
			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);

			List<URL> linksFound = LinkParser.getValidLinks(new URL(seed), html);
			
			for (URL link : linksFound) {
				if (this.links.size() == max) {
					break;
				
				}
				if (lookup.contains(link)) continue;
				crawlFrom( link.toString() );
			}
			html = HtmlCleaner.stripTags(html);// strip tags before strip entities, or else "<normal text, not tag>" will be counted and removed
			html = HtmlCleaner.stripEntities(html);

			
			String[] parsedHtml = TextParser.parse(html);
			
			int position = 1;
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			
			
			List<String> stemmedHtml = Stream.of(parsedHtml).map(word -> {
				return stemmer.stem( word.toLowerCase() ).toString(); // TODO: antelope w/ special symbols -> antelp, should be antelop
			}).collect(Collectors.toList());
			
			for (String word : parsedHtml) {
				String stemmed = stemmer.stem( word.toLowerCase() ).toString();
				if (stemmed.isBlank()) continue;
				index.add(stemmed, seed, position++);
			}
			
		}
		catch (Exception e) {
			System.err.println("oh no" + e);
		}
		
	}
	
	*/
	
	public void crawlFrom(String seed) {
		
		System.out.println(seed);
		

	}

	@Override
	public void collectStemsFrom(String seed) throws IOException {
		try {
			queue.execute(new CrawlURLTask(seed));
			queue.finish();
			System.out.printf("Links(%d): %s%n", links.size(), links.toString()); // TODO: AbstractPrefs not showing up
			System.out.println("Index is now: " + index);
			
			
			
		}
		catch (Exception e) {
			System.out.println("Uhoh - webcrawler"
			+ e);
		}
		
	}

}
