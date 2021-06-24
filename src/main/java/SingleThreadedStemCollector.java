import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Class whose sole responsibility is to parse text files for word stems and store them in an inverted index
 * @author JRRed
 *
 */
public class SingleThreadedStemCollector extends WordStemCollector {

	/**
	 * Constructor
	 * @param index InvertedIndex this collector will save its stems to
	 */
	public SingleThreadedStemCollector(InvertedIndex index) {
		super(index);
	}
	
	@Override
	public void collectStemsFrom(Path seed) throws IOException {
		if (Files.isDirectory(seed)) { // Case: Directory - call parseFile() for each text file in directory
			List<Path> filePaths = TextFileFinder.list(seed);
			
			for (Path filePath : filePaths) { // Case: one file - call parseFile() just for this file
				new ParseFileTask(filePath).run();
			}
		}
		else if (Files.isRegularFile(seed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
			new ParseFileTask(seed).run();
		}
	}

}
