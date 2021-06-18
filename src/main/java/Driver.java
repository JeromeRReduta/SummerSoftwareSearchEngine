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

		// Creating objects
		ArgumentMap argMap = new ArgumentMap(args);
		InvertedIndex invIndex = new InvertedIndex();
		WordStemCollector stemCollector = new WordStemCollector(invIndex);
		
		if (argMap.hasFlag("-text")) { // Collect stems from file(s): argMap.getPath("-text") and store in invertedIndex
			final Path text = argMap.getPath("-text");
			
			try {
				stemCollector.collectStemsFrom(text);
			}
			catch (NullPointerException e) {
				System.err.printf("Error: path is missing or invalid: %s%n", text);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not build inverted index from path: %s%n", text);
			}
		}

		if (argMap.hasFlag("-index")) { // Print InvertedIndex data to file (in JSON format)
			final Path index = argMap.getPath( "-index", Path.of("index.json") );
			
			try {
				invIndex.toJson(index);
			}
			catch (IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", index);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output inverted index data to file: %s%n", index);
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
	 * Note: Keeping this for future reference
	 */
}
