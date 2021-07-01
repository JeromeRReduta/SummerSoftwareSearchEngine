import java.io.FileWriter;
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
	
	private final SimpleReadWriteLock lock;
	
	private final Set<URL> lookup;
	private final List<URL> links;
	private final ThreadSafeInvertedIndex index;
	private final WorkQueue queue;
	private final int max;
	private volatile int current = 0;
	
	// TODO: single-threaded implementation for debugging - get workqueue version working after you confirm this one works
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		super(index);
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.links = new ArrayList<>();
		this.lookup = new HashSet<>();
		this.lock = new SimpleReadWriteLock();
	}
	
	public class CrawlURLTask extends Thread {
		
		private String linkName;
		private int position;
		private final InvertedIndex localIndex;
		
		public CrawlURLTask(String link) {
				this.linkName = link;
				this.position = 1;
				this.localIndex = new InvertedIndex();
		}
		
		public String processHtmlFrom(String link) {
			String html = HtmlFetcher.fetch(link, 3); // Supposed to do 3 redirects
			// TODO: HttpsFetcher.openConnection() takes up one port - have to sync access to this
			if (html == null) return null;
			

			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);
			return html;
		}
		
		@Override
		public void run() {
			URL linkURL;
			try {
				linkURL = new URL(linkName);
			}
			catch (Exception e) {
				System.out.println("Dang");
				linkURL = null;
			}
			String html = null;
			try (FileWriter a = new FileWriter("newStuff.txt")) {
				
			 html = processHtmlFrom(linkName);
			if (html == null) return;
				
			a.write("After processing: " + html);
			
			}
			catch (Exception e) {
			}
			List<URL> linksFound = LinkParser.getValidLinks(linkURL, html);
			
			try (FileWriter a = new FileWriter("newStuff2.txt")) {
					a.write("Links found: " + linksFound + "\n\n");
			
					a.write("LINKS FOUND FOR " + linkName + "(" + linksFound.size() + "):\n");
					for (int i = 0; i < linksFound.size(); i++) {
						a.write(i + ": " + linksFound.get(i) + "\t");
					}
			}
			catch (Exception e) {
				System.err.println("ERROR FOR " + linkName);
			}
			
			
			// if (html == null) return; // TODO: do we need to null check tiwce?
			// TODO: is everything inside <td> </td> getting deleted?
			lock.writeLock().lock();
			//System.out.println("write locked");
			try {
				for (URL link : linksFound) {
					
					System.out.println("LINK: " + link); // TODO: add filewriter ofr this, and see which hrefs come upfirst, and which hrefs are accepted
				
				
				
					System.out.println("Links.size(): " + links.size());
					if (links.size() == max) break;
					
					
					
					if (!lookup.contains(link) && HtmlFetcher.fetch(link, 3) != null) {
						lookup.add(linkURL);
						links.add(linkURL);
					//	System.out.printf("links(%d): %s%n", links.size(), links);
						queue.execute(new CrawlURLTask(link.toString()));
					}
				}
			}
			catch (Exception e) {
				System.err.println("uh oh");
			}
			finally {
				lock.writeLock().unlock();
				System.out.println("write unlocked");
			}
					
			
				
			html = HtmlCleaner.stripTags(html);// strip tags before strip entities, or else "<normal text, not tag>" will be counted and removed
			html = HtmlCleaner.stripEntities(html);
			String[] parsedHtml = TextParser.parse(html);
			
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			for (String word : parsedHtml) {
				String stemmed = stemmer.stem( word.toLowerCase() ).toString();
				if (stemmed.isBlank()) continue;
				localIndex.add(stemmed, linkName, position++);
			}
		
			index.attemptMergeWith(localIndex); // TODO: Search for what happens to "PREFERENCES" link in javadocs
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
			URL linkURL = new URL(seed);
			//lookup.add(linkURL); // TODO: Did I forgot to add this in the first place?
			//links.add(linkURL);
			queue.execute(new CrawlURLTask(seed));
			queue.finish();
			
			System.out.println("Index is now: " + index);
		}
		catch (MalformedURLException e) {
			System.err.println("Error - webcrawler - bad URL");
		}

	}

}
