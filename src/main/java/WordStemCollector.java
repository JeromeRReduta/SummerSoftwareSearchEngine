import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class whose sole responsibility is to collect word stems from a file and put them into an inverted index
 * @author JRRed
 *
 */
public abstract class WordStemCollector {
	
	/** InvertedIndex to store word stems into */
	private final InvertedIndex index;
	
	/**
	 * Constructor
	 * @param index inverted index
	 */
	public WordStemCollector(InvertedIndex index) {
		this.index = index;
	}
	
	/**
	 * Collects stems from a file or directory path and stores them in its invertedIndex
	 * @param seed file or directory path
	 * @throws IOException in case of IO Error
	 */
	public abstract void collectStemsFrom(String seed) throws IOException;
	
	/**
	 * Class whose sole responsibility is to represent the task: "Given the path to one file, read through the file,
	 * stemming each line, storing those stems into an inverted index. After, merge the contents of this index into
	 * a common index.
	 * @author JRRed
	 *
	 */
	protected class ParseFileTask extends Thread {
		/** file path to read */
		private final Path path;
		
		/** an index to store local results into */
		private final ThreadSafeInvertedIndex localIndex;
		
		/** stemmer for turning lines into word stems */
		private final SnowballStemmer stemmer;
		
		/**
		 * Constructor
		 * @param path file path
		 */
		public ParseFileTask(Path path) {
			this.path = path;
			this.localIndex = new ThreadSafeInvertedIndex(); // TODO Make a normal index
			this.stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
		}
		
		@Override
		public void run() {
			String location = path.toString();
			int position = 1;
			
			try ( BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8) ) {
				String line;
				while ( (line = reader.readLine()) != null ) {
					String[] parsedLine = TextParser.parse(line);
					
					for (String word : parsedLine) {
						localIndex.add( stemmer.stem(word).toString(), location, position++ );
					}
				}
				index.attemptMergeWith(localIndex);
			}
			catch(Exception e) {
				System.err.println("ERROR - TASK W/ LOCATION " + location + " HAS HAD IO ERROR");
			}
		}
	}
}
