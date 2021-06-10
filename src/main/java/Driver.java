import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class Driver {

	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		// store initial start time
		Instant start = Instant.now();

		ArgumentMap argMap = new ArgumentMap(args);
		InvertedIndex invIndex = new InvertedIndex();
		
		if (argMap.hasFlag("-text")) {
			final Path TEXT = argMap.getPath("-text");
			System.out.printf("TEXT IS %s\n", TEXT);
			
			try {
				WordStemCollector collector = new WordStemCollector.Builder()
						.readingFrom(TEXT).savingStemsTo(invIndex).build();
				
				collector.collectStems();
			}
			catch (NullPointerException e) {
				System.err.println("IOException or NullPointerException");
			}
			catch (Exception e) {
				System.err.println("eh");
			}
		}
		if (argMap.hasFlag("-index")) {
			final Path INDEX = argMap.getPath("-index", Path.of("index.json"));
			try {
				SearchJsonWriter.asInvertedIndex(invIndex, INDEX);
			}
			catch (IOException e) {
				System.err.println("Ah");
			}
			catch(Exception e) {
				System.err.println("Oh");
			}
		}
		
		

		// calculate time elapsed and output
		Duration elapsed = Duration.between(start, Instant.now());
		double seconds = (double) elapsed.toMillis() / Duration.ofSeconds(1).toMillis();
		System.out.printf("Elapsed: %f seconds%n", seconds);
	}

	/*
	 * Generally, "Driver" classes are responsible for setting up and calling
	 * other classes, usually from a main() method that parses command-line
	 * parameters. Generalized reusable code are usually placed outside of the
	 * Driver class. They are sometimes called "Main" classes too, since they 
	 * usually include the main() method. 
	 * 
	 * If the driver were only responsible for a single class, we use that class
	 * name. For example, "TaxiDriver" is what we would name a driver class that
	 * just sets up and calls the "Taxi" class.
	 *
	 * TODO: Delete this after reading.
	 */
}
