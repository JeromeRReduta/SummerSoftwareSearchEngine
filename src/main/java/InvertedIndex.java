import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class whose sole responsibility is to store data in an InvertedIndex format
 * InvertedIndex is a String to (Map: String to (TreeSet: Integer)) pair
 * @author JRRed
 *
 */
public class InvertedIndex {
	
	/**
	 * Index data structure - "innermap" is TreeMap: String to (TreeSet: Integer)
	 */
	private final TreeMap<String, TreeMap<String, TreeSet<Integer>>> map;
	
	/**
	 * Constructor
	 */
	public InvertedIndex() {
		map = new TreeMap<>();
	}
	
	public void add(String stem, String pathName, int position) {
		map.putIfAbsent(stem,  new TreeMap<>());
		map.get(stem).putIfAbsent(pathName,  new TreeSet<>());
		map.get(stem).get(pathName).add(position);
	}
	
	public Set<String> get() {
		return Collections.unmodifiableSet( map.keySet() );
	}
	
	public Set<String> get(String stem) {
		return map.get(stem) != null ?
				Collections.unmodifiableSet( map.get(stem).keySet() ) : Collections.emptySet();
	}
	
	public Set<Integer> get(String stem, String pathName) {
		return map.get(stem) != null && map.get(stem).get(pathName) != null
				? Collections.unmodifiableSet( map.get(stem).get(pathName) ) : Collections.emptySet();
	}
	
	public boolean contains(String stem) {
		return map.containsKey(stem);
	}
	
	public boolean contains(String stem, String pathName) {
		return contains(stem) && map.get(stem) != null && map.get(stem).containsKey(pathName);
	}
	
	public boolean contains(String stem, String pathName, int position) {
		return contains(stem, pathName) && map.get(stem).get(pathName) != null &&
				map.get(stem).get(pathName).contains(position);
	}
	
	public int size() {
		return map.size();
	}
	
	public int innerMapSize(String stem) {
		return contains(stem) ? map.get(stem).size() : 0;
	}
	
	public int locationListSize(String stem, String pathName) {
		return contains(stem, pathName) ? map.get(stem).get(pathName).size() : 0;
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	public void toFile(final Path path) throws IOException {
		//TODO: add toFile
	}
	
	//TODO: Search logic comes later
	
	
}