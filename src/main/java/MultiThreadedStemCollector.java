import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Multi-threaded implementation of WordStemCollector. Uses a work queue.
 * @author JRRed
 *
 */
public class MultiThreadedStemCollector extends WordStemCollector {
	/** work queue */
	private final WorkQueue queue;
	
	/**
	 * Constructor
	 * @param threadSafe thread-safe index
	 * @param queue work queue
	 */
	public MultiThreadedStemCollector(ThreadSafeInvertedIndex threadSafe, WorkQueue queue) {
		super(threadSafe);
		this.queue = queue;
	}
	
	@Override
	public void collectStemsFrom(Path seed) throws IOException {
		if (Files.isDirectory(seed)) { // Case: Directory - call parseFile() for each text file in directory
			List<Path> filePaths = TextFileFinder.list(seed);
			
			for (Path filePath : filePaths) { // Case: one file - call parseFile() just for this file
				queue.execute( new ParseFileTask(filePath) );
			}
		}
		else if (Files.isRegularFile(seed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
			queue.execute( new ParseFileTask(seed) );
		}
		
		queue.finish();
	}
}
