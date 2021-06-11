import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Class whose sole responsibility is to parse text files for word stems and store them in an inverted index
 * @author JRRed
 *
 */
public class WordStemCollector {

	/** InvertedIndex this collector will store its data to */
	private final InvertedIndex index;
	
	/** list of file paths this collector will read from */
	private final List<Path> filePaths;
	
	/**
	 * Constructor - use builder instead
	 * @param builder
	 */
	private WordStemCollector(WordStemCollector.Builder builder) {
		index = builder.index;
		filePaths = builder.filePaths;
	}
	
	/**
	 * Builder pattern for WordStemCollector
	 * @author JRRed
	 *
	 */
	public static class Builder {
		
		/** InvertedIndex the collector will store its data to */
		private InvertedIndex index;
		
		/** list of file paths this collector will read from */
		private List<Path> filePaths;
		
		/**
		 * Constructor. Please use the builder methods to set data for WordStemCollector.
		 */
		public Builder() {
		}
		
		/**
		 * If inputPath is a directory, sets filePaths to all files under that directory. If inputPath is a file, sets filePaths to
		 * just that file. Else, sets filePaths to null.
		 * @param inputPath a path that may be a file or directory
		 * @return the builder
		 */
		public Builder readingAllFilesFrom(Path inputPath) {
			try {
				if (Files.isDirectory(inputPath)) {
					filePaths = TextFileFinder.list(inputPath);
				}
				else if (Files.isRegularFile(inputPath,  java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
					filePaths = List.of(inputPath);
				}
				else {
					filePaths = null;
				}
			}
			catch (IOException e) {
				System.err.println("IOException - WordStemCollector.Builder()");
				filePaths = null;
			}
			catch(Exception e) {
				System.err.println("Unknown exception - WordStemCollector.Builder() " + e);
				filePaths = null;
			}
			
			return this;
		}
		
		/**
		 * Sets the builder's invertedIndex
		 * @param index InvertedIndex that the WordStemCollector will save its stems to
		 * @return the builder
		 */
		public Builder savingStemsTo(InvertedIndex index) {
			this.index = index;
			return this;
		}
		
		/**
		 * Creates a WordStemCollector using this builder
		 * @return A WordStemCollector using this builder
		 */
		public WordStemCollector build() {
			return new WordStemCollector(this);
		}
	}
	
	/**
	 * Collects stems from all files the collector has received, and saves it to its InvertedIndex
	 */
	public void collectStems() {
		for (Path filePath : filePaths) {
			try {
				OneFileStemCollector oneFileCollector = new OneFileStemCollector.Builder()
						.readingFrom(filePath).savingStemsTo(index).build();
				
				oneFileCollector.parseFile();
			}
			catch(IOException e) {
				System.err.println("IOException - WordStemCollector");
			}
			catch(Exception e)  {
				System.err.println("Unknown exception - WordStemCollector " + e);
			}
		}
	}
}
