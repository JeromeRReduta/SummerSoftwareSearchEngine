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
		return map != null ? Collections.unmodifiableSet( map.keySet() ) :
			Collections.emptySet();
	}
	
	public Set<String> get(String stem) {
		boolean outerAndInnerMapExist = map != null && map.get(stem) != null;
		
		return outerAndInnerMapExist ? Collections.unmodifiableSet( map.get(stem).keySet() ) :
			Collections.emptySet();
	}
	
	public Set<Integer> get(String stem, String pathName) {
		boolean outerAndInnerMapExist = map != null && map.get(stem) != null;
		
		return outerAndInnerMapExist ? Collections.unmodifiableSet( map.get(stem).get(pathName) ) :
			Collections.emptySet();
	}
	
}