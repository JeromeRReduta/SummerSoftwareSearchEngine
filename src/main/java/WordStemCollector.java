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

	private final InvertedIndex index;
	private final List<Path> filePaths;
	
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
		
		private InvertedIndex index;
		private List<Path> filePaths;
		
		public Builder() {
		}
		
		public Builder readingFrom(Path inputPath) {
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
				return this;
			}
			catch (IOException e) {
				System.err.println("IOException - WordStemCollector.Builder()");
				filePaths = null;
				return this;
			}
			catch(Exception e) {
				System.err.println("Unknown exception - WordStemCollector.Builder()");
				filePaths = null;
				return this;
			}
		}
		
		public Builder savingStemsTo(InvertedIndex index) {
			this.index = index;
			return this;
		}
		
		public WordStemCollector build() {
			return new WordStemCollector(this);
		}
	}
	
	public void testStuff() {
		System.out.println("INDEX: " + index);
		System.out.println("FILE PATHS: " + filePaths);
	}
	
	public void collectStems() {
		for (Path filePath : filePaths) {

			try ( BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8) ) {
				
				/** Turn this whole region into an object */
				String location = filePath.toString();
				Stemmer stemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH);
				int position = 1;
				String line;
				
				while ((line = reader.readLine()) != null) {
					String[] parsedLine = TextParser.parse(line);
					for (String word : parsedLine) {
						index.add( stemmer.stem(word).toString(), location,  position++ );
					}
				}
				
				/* End region */
			}
			catch(IOException e) {
				System.err.println("Oops - IO Error - WordStemCollector");
				
			}
			catch(Exception e)  {
				System.err.println("Oops - unknown error - WordStemCollector");
			}
			
		}
	
		// TODO: Do rest of stem collection
	}
}
