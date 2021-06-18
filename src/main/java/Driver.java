import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
		
		if (argMap.hasFlag("-text")) { // Collect stems from file(s): argMap.getPath("-text") and store in invertedIndex
			final Path TEXT = argMap.getPath("-text"); // TODO Fix variable name
			
			try {
				/*
				 * TODO Either use static methods (where you don't need to create a 
				 * variable here) -or- create a variable and initialize near your inverted index.
				 */
				// Note: If I multithread without making a var name here, will it still work?
				new WordStemCollector(invIndex).collectStemsFrom(TEXT);
			}
			catch (NullPointerException e) {
				System.err.printf("Error: path is missing or invalid: %s%n", TEXT);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not build inverted index from path: %s%n", TEXT);
			}
		}
		
		if (argMap.hasFlag("-index")) { // Print InvertedIndex data to file (in JSON format)
			final Path INDEX = argMap.getPath( "-index", Path.of("index.json") );  // TODO Fix variable name
			
			try {
				invIndex.toJson(INDEX);
			}
			catch (IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", INDEX);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output inverted index data to file: %s%n", INDEX);
			}
		}
		
		String line = "bubba";
		
		
		// Exact:
		Collection<String> bubba = TextFileStemmer.uniqueStems(line).stream()
			.filter( invIndex.getStrings()::contains )
			.collect( Collectors.toCollection(TreeSet::new) );
		
		TextFileStemmer.uniqueStems(line).stream().flatMap( stem -> turnStemIntoTreeSetOfPartialStems.stream() ).collect( Collectors.toCollection(TreeSet::new) );
				partialStem
				).forEach();

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
