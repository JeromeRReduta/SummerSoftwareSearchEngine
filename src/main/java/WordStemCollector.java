import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class whose sole responsibility is to collect word stems from a file and put them into an inverted index.
 * This class has a simple, single-threaded implementation that has been provided by default.
 * @author JRRed
 *
 */
public interface WordStemCollector {
	/**
	 * Collects stems from a file or directory path and stores them in its InvertedIndex
	 * @param seed file or directory path
	 * @throws IOException in case of IO Error
	 */
	default void collectStemsFrom(Path seed) throws IOException {
		if (Files.isDirectory(seed)) { // Case: Directory - call parseFile() for each text file in directory
			List<Path> filePaths = TextFileFinder.list(seed);
			
			for (Path filePath : filePaths) { // Case: one file - call parseFile() just for this file
				parseFile(filePath);
			}
		}
		else if (Files.isRegularFile(seed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
			parseFile(seed);
		}
	}
	
	/**
	 * Collects stems from one file and stores them in its InvertedIndex
	 * @param path file path
	 * @throws IOException in case of IO Error
	 */
	void parseFile(Path path) throws IOException;
	
	/**
	 * Parses stems from one file and collects them to an InvertedIndex
	 * @param path path
	 * @param index InvertedIndex
	 * @throws IOException in case of IOError
	 */
	static void parseFile(Path path, InvertedIndex index) throws IOException {
		Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		String location = path.toString();
		int position = 1;
		
		try ( BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8) ) {
			String line;
			while ( (line = reader.readLine()) != null ) {
				String[] parsedLine = TextParser.parse(line);
				
				for (String word : parsedLine) {
					index.add( stemmer.stem(word).toString(), location, position++ );
				}
			}
		}
	}
	
	/**
	 * Simple, single-threaded implementation of WordStemCollector.
	 * @author JRRed
	 *
	 */
	public class Default implements WordStemCollector {
		/** InvertedIndex to store word stems into */
		private final InvertedIndex index;
		
		/**
		 * Constructor
		 * @param index InvertedIndex
		 */
		public Default(InvertedIndex index) {
			this.index = index;
		}
		
		@Override
		public void parseFile(Path path) throws IOException {
			WordStemCollector.parseFile(path, index);
		}
	}
}