import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

public class WebCrawler {
	
	private static Logger log = LogManager.getLogger();
	
	private final Set<URL> lookup;
	private final List<URL> links;
	private final ThreadSafeInvertedIndex index;
	private final WorkQueue queue;
	private final int max;
	
	// TODO: single-threaded implementation for debugging - get workqueue version working after you confirm this one works
	public WebCrawler(ThreadSafeInvertedIndex index, WorkQueue queue, int max) {
		this.index = index;
		this.max = max;
		this.queue = queue;
		this.links = new ArrayList<>();
		this.lookup = new HashSet<>();
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
	public void crawlFrom(String seed) {
		try {
			/*
			FileWriter writer = new FileWriter("A.txt");
			FileWriter writer2 = new FileWriter("B.txt");
			FileWriter writer3 = new FileWriter("C.txt");
			FileWriter writer4 = new FileWriter("D.txt");
			*/
			
			this.links.add(new URL(seed));
			lookup.add(new URL(seed));
			
			
			//writer.write("SEED: " + seed + "__________________________________________________\n");
			String html = HtmlFetcher.fetch(seed, 3); // Supposed to do 3 redirects
			
//			writer.write(html);
			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);
			
//			writer2.write("After stripping comments & block elements:\n" + html);

			List<URL> linksFound = LinkParser.getValidLinks(new URL(seed), html);
			
//			System.out.println("Parsed links: " + linksFound);
			
			for (URL link : linksFound) {
				if (this.links.size() == max) {
					break;
				
				}
				// TODO: Check to see if it should be >= max or > max
				if (lookup.contains(link)) continue;
				
				
				crawlFrom( link.toString() );
			}
			html = HtmlCleaner.stripTags(html);// strip tags before strip entities, or else "<normal text, not tag>" will be counted and removed
			html = HtmlCleaner.stripEntities(html);

			
//			System.out.println("BUBBAAAAAAAAAAAAAAAAAAAAAAAa");
//			writer3.write("After stripping entities & tags:\n" + html);
			String[] parsedHtml = TextParser.parse(html);
			
//			writer3.write("Parsed html:\n");
//			System.out.print("[");
			int i = 0;
			for (String word : parsedHtml) {
				if (i == 100) break;
//				System.out.printf("%d: %s%n", i++, word);
			}
			for (String word : parsedHtml) {
//				System.out.print("'" + word + "', ");
			}
//			System.out.print("]\n");
		
			
			int position = 1;
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			
			
			List<String> stemmedHtml = Stream.of(parsedHtml).map(word -> {
				return stemmer.stem( word.toLowerCase() ).toString(); // TODO: antelope w/ special symbols -> antelp, should be antelop
			}).collect(Collectors.toList());
			
//			System.out.println("STEMMED HTML: " + stemmedHtml);
			for (String word : parsedHtml) {
				
//				System.out.println("Word: '" + word + "' isBlank? " + word.isBlank());
				if (word.isBlank()) continue;
				if (position > 2500 && position <= 2600 ) {
//					System.out.println("Original word (" + (position) + "): " + word);
				}
				String stemmed = stemmer.stem( word.toLowerCase() ).toString();
				if (position > 2500 && position <= 2600 ) {
//					System.out.println("Stemmed: " + stemmed);
				}
				
				if (stemmed.isBlank()) continue;
				index.add(stemmed, seed, position++);
				if (position > 2500 && position <= 2600 ) {
//					System.out.println("ADDING (" + (position - 1) + ") " + stemmed);
				}
			}
			
		}
		catch (Exception e) {
			System.err.println("oh no" + e);
		}
		
	}
	
	

}
