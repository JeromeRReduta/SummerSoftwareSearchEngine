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

	// Note: THIS IS PROJECT 4 BRANCH - SWITCH TO MAIN ONE ONCE DESIGN PASSES
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
		SearchEngine searchEngine = new SearchEngine(argMap);
		
		
		if (argMap.hasFlag("-text") || argMap.hasFlag("-html")) { // Collect stems from file(s): argMap.getPath("-text") and store in invertedIndex
			final String text = argMap.hasFlag("-html") ? argMap.getString("-html") : argMap.getString("-text");
			System.out.println("TEXT: " + text);
			try {
				searchEngine.parseFilesFrom(text);
			}
			catch (NullPointerException e) {
				System.err.printf("Error: path is missing or invalid: %s%n", text);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not build inverted index from path: %s%n", text);
			}
			
		}
		
		
		if (argMap.hasFlag("-query")) {
			final Path query = argMap.getPath("-query");
			
			try {
				searchEngine.searchFrom(query);
			}
			catch (NullPointerException e) {
				System.err.printf("Error: query path is missing or invalid: %s%n", query);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not search inverted index with path: %s%n", query);
			}
			
		}
		
		if (argMap.hasFlag("-index")) { // Print InvertedIndex data to file (in JSON format)
			final Path index = argMap.getPath( "-index", Path.of("index.json") );
			
			try {
				searchEngine.outputIndexTo(index);
			}
			catch (IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", index);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output inverted index data to file: %s%n", index);
			}
		}
		
		if (argMap.hasFlag("-counts")) { // Prints file string count data to file (in JSON format)
			final Path counts = argMap.getPath( "-counts", Path.of("counts.json") );
			
			try {
				searchEngine.outputWordCountsTo(counts);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", counts);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output string count data to file: %s%n", counts);
			}
		}
		
		if (argMap.hasFlag("-results")) {
			final Path results = argMap.getPath( "-results", Path.of("results.json") );
			
			try {
				searchEngine.outputResultsTo(results);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealign with path: %s%n", results);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not output search result data to file: %s%n", results);
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
