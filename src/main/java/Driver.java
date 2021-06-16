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

	// Note: THIS IS PROJECT 2 BRANCH - SWITCH TO MAIN ONE ONCE DESIGN PASSES
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
		IndexSearcher searcher = new IndexSearcher(invIndex);
		
		if (argMap.hasFlag("-text")) { // Collect stems from file(s): argMap.getPath("-text") and store in invertedIndex
			final Path TEXT = argMap.getPath("-text");
			
			try {
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
		
		if (argMap.hasFlag("-query")) {
			final Path QUERY = argMap.getPath("-query");
			
			try {
				
				
				searcher.search(QUERY,  true);
					/*
					Collection<String> exactStemsFromLine = TextFileStemmer.uniqueStems(line);
					
					Collection<String> partialStemsFromLine = TextFileStemmer.uniqueStems(line).stream()
							.flatMap( invIndex::getStemsStartingWith )
							.collect( Collectors.toCollection(TreeSet::new) );
					*/
				
				
			}
			catch (NullPointerException e) {
				System.err.printf("Error: query path is missing or invalid: %s%n", QUERY);
				e.printStackTrace();
			}
			catch (Exception e) {
				System.out.println(e);
				System.err.printf("Error: Could not search inverted index with path: %s%n", QUERY);
			}
		}
		
		if (argMap.hasFlag("-index")) { // Print InvertedIndex data to file (in JSON format)
			final Path INDEX = argMap.getPath( "-index", Path.of("index.json") );
			
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
		
		if (argMap.hasFlag("-counts")) { // Prints file string count data to file (in JSON format)
			final Path COUNTS = argMap.getPath( "-counts", Path.of("counts.json") );
			
			try {
				invIndex.stringCountsToJson(COUNTS);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", COUNTS);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output string count data to file: %s%n", COUNTS);
			}
		}
		
		if (argMap.hasFlag("-results")) {
			final Path RESULTS = argMap.getPath( "-results", Path.of("results.json") );
			
			try {
				searcher.outputToFile(RESULTS);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealign with path: %s%n", RESULTS);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not output search result data to file: %s%n", RESULTS);
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
