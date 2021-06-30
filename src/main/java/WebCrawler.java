import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

public class WebCrawler {
	
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
			
			String html = HtmlFetcher.fetch(seed, 3); // Supposed to do 3 redirects
			System.out.println(html);
			html = HtmlCleaner.stripComments(html);
			html = HtmlCleaner.stripBlockElements(html);
			
			System.out.println("After stripping comments & block elements:\n" + html);
			List<URL> linksFound = LinkParser.getValidLinks(LinkParser.normalize(new URL(seed)), html);
			System.out.println("Parsed links: " + linksFound);
			
			
			for (URL link : linksFound) {
				if (this.links.size() >= max) return; // TODO: for multi-threaded approach, just return for both if-statements
				if (lookup.contains(link)) continue;
				
				this.links.add(link);
				crawlFrom( link.toString() );
			}
			
			html = HtmlCleaner.stripEntities(html);
			html = HtmlCleaner.stripTags(html);
			System.out.println("After stripping entities & tags:\n" + html);
			
			String[] splitHtml = html.split("\r?\n");
			
			System.out.println(Arrays.toString(splitHtml));
			

			
			int position = 1;
			Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			
			
			List<String> stemmedHtml = Stream.of(splitHtml).map(word -> {
				return stemmer.stem(word.replaceAll("(\\W)|(\\d)", "")).toString(); // TODO: antelope w/ special symbols -> antelp, should be antelop
			}).collect(Collectors.toList());
			
			
			System.out.println("antelöpé stemmed: " + stemmer.stem("antelöpé").toString());
			System.out.println("STEMMED HTML: " + stemmedHtml);
			for (String word : splitHtml) {
				
				System.out.println("Word: '" + word + "' isBlank? " + word.isBlank());
				if (word == null || word.isBlank()) continue;
				index.add( stemmer.stem(word.strip()).toString(), seed, position++);
			}
			
			System.out.println("Index: " + index.toJson());
			
			
			
			
			
			
			
			
		}
		catch (Exception e) {
			System.err.println("oh no");
		}
		
	}
	
	

}
