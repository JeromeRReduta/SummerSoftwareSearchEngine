import java.io.IOException;

/**
 * Basic interface for crawling stems
 * @author JRRed
 *
 */
public interface StemCrawler {
	/**
	 * Collects stems from the seed
	 * @param seed seed
	 * @throws IOException in case of IO Error
	 */
	void collectStemsFrom(String seed) throws IOException;
}
