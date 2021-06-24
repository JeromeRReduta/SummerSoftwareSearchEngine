import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class whose sole responsibility is to parse text files for word stems and store them in an inverted index
 * @author JRRed
 *
 */
public class WordStemCollector {
	/** InvertedIndex this collector will store its data to */
	private final InvertedIndex index;
	
	/**
	 * Constructor
	 * @param index InvertedIndex this collector will save its stems to
	 */
	public WordStemCollector(InvertedIndex index) {
		this.index = index;
	}
	
	/**
	 * Collects stems from a file or directory path and stores them in its invertedIndex
	 * @param seed file or directory path
	 * @throws IOException in case of IO Error
	 */
	public void collectStemsFrom(Path seed) throws IOException {
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
	 * Parses a file, collecting its stems and storing them to an inverted index
	 * @param filePath path of one file
	 * @param index InvertedIndex to store stems into
	 * @throws IOException In case of IO error
	 */
	public static void parseFile(Path filePath, InvertedIndex index) throws IOException {
		int position = 1;
		String location = filePath.toString();
		Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH); // Note: Make these a task w/ index.merge() for P3
		
		try ( BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8) ) {
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
	 * Parses a file, collecting its stems and storing them to an inverted index
	 * @param filePath path of one file
	 * @throws IOException In case of IO error
	 */
	public void parseFile(Path filePath) throws IOException {
		parseFile(filePath, this.index);
	}
}
