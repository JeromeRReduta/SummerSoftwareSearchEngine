import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class whose sole responsibility is to parse the stems from one text file and save them to an inverted index
 * @author JRRed
 *
 */
public class OneFileStemCollector {
	/** InvertedIndex this collector will store its stems to */
	private final InvertedIndex index;
	
	/** Stemmer this collector will use */
	private final Stemmer stemmer;
	
	/** the one file path this collector will read from */
	private final Path filePath;
	
	/** tells us we're on the nth stem (e.g. if position = 5, we're on the 5th stem) */
	private int position;
	
	/**
	 * Constructor - please use builder to create new instances of this class
	 * @param builder this class's builder pattern
	 */
	private OneFileStemCollector(OneFileStemCollector.Builder builder) {
		index = builder.index;
		stemmer = builder.stemmer;
		filePath = builder.filePath;
		position = builder.position;
	}
	
	/**
	 * Builder pattern
	 * @author JRRed
	 *
	 */
	public static class Builder {
		/** InvertedIndex this collector will store its stems to */
		private InvertedIndex index;
		
		/** Stemmer this collector will use */
		private Stemmer stemmer;
		
		/** the one file path this collector will read from */
		private Path filePath;
		
		/** tells us we're on the nth stem (e.g. if position = 5, we're on the 5th stem) */
		private int position;
		
		/**
		 * Constructor. The stemmer and position are always the same.
		 */
		public Builder() {
			stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
			position = 1;
		}
		
		/**
		 * Sets the builder's file path
		 * @param filePath file path the OneFileStemCollector will be reading from
		 * @return the builder
		 */
		public Builder readingFrom(Path filePath) {
			this.filePath = filePath;
			return this;
		}
		
		/**
		 * Sets the builder's InvertedIndex
		 * @param index the InvertedIndex that the OneFileStemCollector will be saving its stems to
		 * @return the builder
		 */
		public Builder savingStemsTo(InvertedIndex index) {
			this.index = index;
			return this;
		}
		
		/**
		 * Creates a OneFileStemCollector with information from this builder
		 * @return a OneFileStemCollector with information from this builder
		 */
		public OneFileStemCollector build() {
			return new OneFileStemCollector(this);
		}
	}
	
	/**
	 * Parses one file, collecting its word stems and saving it to the InvertedIndex
	 * @throws IOException In case of IOError while reading
	 */
	public void parseFile() throws IOException {
		try ( BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8) ) {
			String line;
			while ( (line = reader.readLine()) != null ) {
				String[] parsedLine = TextParser.parse(line);
				
				for (String word : parsedLine) {
					index.add( stemmer.stem(word).toString(), filePath.toString(), position++ );
				}
			}
		}
	}
}
