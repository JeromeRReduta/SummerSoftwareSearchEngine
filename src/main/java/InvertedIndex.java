import java.util.Collections;
import java.util.Map;
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
	
	/* NOTE: Shouldn't make a builder here, b/c the point isn't to create
	a new thing with many complex things, but to populate an already existing
	thing with data */
	
	/**
	 * Adds a stem with a given position num from a given filename
	 * @param stem stem
	 * @param pathName file name
	 * @param position the stem's "position" in the text file (1st stem = pos. 1, nth stem = pos. n)
	 */
	public void add(String stem, String pathName, int position) {
		map.putIfAbsent(stem,  new TreeMap<>());
		map.get(stem).putIfAbsent(pathName,  new TreeSet<>());
		map.get(stem).get(pathName).add(position);
	}
	
	/*
	 * TODO
	 * 
	 * get() --> return an unmodifiable keyset (all the words)
	 * 
	 * get(String word) --> return the inner map keyset (all the locations for a word)
	 * 
	 * get(String word, String location) --> return the unmodifiable inner treeset
	 */
	
	// TODO Remove, breaks encapsulation
	/**
	 * Returns an unmodifiable view of the index data
	 * @return An unmodifiable view of the index's data
	 */
	public Map<String, TreeMap<String, TreeSet<Integer>>> get() {
		return Collections.unmodifiableMap(map);
	}
	
	// TODO Remove, replace with a safer version
	/**
	 * Returns an unmodifiable view of the inner map containing every file that has the given word stem
	 * 
	 * @param stem word stem
	 * @return An unmodifiable view of the inner map containing every file that has the given word stem
	 */
	public Map<String, TreeSet<Integer>> get(String stem) {
		return map.get(stem) != null ?
				Collections.unmodifiableMap( map.get(stem) ) : Collections.emptyMap();
	}
	
	// TODO Maybe reuse your contains(stem, pathName) to reuse some of that code
	/**
	 * Returns an unmodifiable view of the set of position nums a given file has for a given word stem
	 * @param stem word stem
	 * @param pathName file name
	 * @return An unmodfiiable view of the set of position nums a given file has for a given word stem
	 */
	public Set<Integer> get(String stem, String pathName) {
		return map.get(stem) != null && map.get(stem).get(pathName) != null ?
				Collections.unmodifiableSet( map.get(stem).get(pathName) ) : Collections.emptySet();
	}
	
	/**
	 * Returns whether the index contains a given word stem
	 * @param stem word stem
	 * @return Whether the index contains a given word stem
	 */
	public boolean contains(String stem) {
		return map.containsKey(stem);
	}
	
	/**
	 * Returns whether the index contains a given word stem and a given file that holds that word stem
	 * @param stem word stem
	 * @param pathName file name
	 * @return Whether the index contains a given word stem and a given file that holds that word stem
	 */
	public boolean contains(String stem, String pathName) {
		return contains(stem) && map.get(stem) != null && map.get(stem).containsKey(pathName);
	}
	
	/**
	 * Returns whether the index contains a given position num, in a given file, that holds a given word stem
	 * @param stem word stem
	 * @param pathName file name
	 * @param position position num of the given stem
	 * @return Whether the index contains a given positoin num, in a given file, that holds a given word stem
	 */
	public boolean contains(String stem, String pathName, int position) {
		return contains(stem, pathName) && map.get(stem).get(pathName) != null &&
				map.get(stem).get(pathName).contains(position);
	}
	
	/*
	 * TODO Either give each method a different meaningful name or keep it the 
	 * same with the different params
	 * 
	 * size()
	 * size(String stem(
	 * size(String stem, String pathName)
	 * 
	 * -or-
	 * 
	 * numWords()
	 * numLocations(String word)
	 * numPositions(String word, String location)
	 * 
	 * Careful about "stem" vs word
	 * and "pathName" vs location
	 */
	
	/**
	 * Returns the number of stems in the index
	 * @return The number of stems in the index
	 */
	public int size() {
		return map.size();
	}
	
	/**
	 * Returns the number of file that have a given word stem
	 * @param stem word stem
	 * @return The number of files that have a given word stem
	 */
	public int innerMapSize(String stem) {
		return contains(stem) ? map.get(stem).size() : 0;
	}
	
	/**
	 * Returns the number of times a given stem occurs in a given file that has that stem
	 * @param stem word stem
	 * @param pathName file name
	 * @return The number of times a given stem occurs in a given file that has that stem
	 */
	public int locationListSize(String stem, String pathName) {
		return contains(stem, pathName) ? map.get(stem).get(pathName).size() : 0;
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	/* TODO Try this instead
	public void toJson(Path path) throw IOException {
		SearchJsonWriter.asInvertedIndex(this.map, path);
	}
	*/
}
