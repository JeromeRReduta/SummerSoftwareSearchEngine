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
	
	/* NOTE: Shouldn't make a builder here, b/c the point isn't to create
	a new thing with many complex things, but to populate an already existing
	thing with data */
	
	/**
	 * Adds a string with a given position num from a given filename
	 * @param str string
	 * @param location name of place where str was found
	 * @param position the string's "position" in the text file (1st string = pos. 1, nth string = pos. n)
	 */
	public void add(String str, String location, int position) {
		map.putIfAbsent(str,  new TreeMap<>());
		map.get(str).putIfAbsent(location,  new TreeSet<>());
		map.get(str).get(location).add(position);
	}
	
	// Note: Map<String, ? extends Map<String, ? extends Iterable<Integer>>> is generic type for this nested map
	
	/**
	 * Returns an unmodifiable view of all the strings in the index
	 * @return An unmodifiable view of all the strings in the index
	 * @note Shouldn't use Collections.unmodifiableMap(), b/c unmodifiableX() only makes outermost layer of X unmodifiable - inner structures are still mutable
	 */
	public Set<String> getStrings() {
		return Collections.unmodifiableSet( map.keySet() );
	}
	
	/**
	 * Returns an unmodifiable view of all the locations that contain the given string
	 * @param str string
	 * @return An unmodifiable view of all the locations containing the given string
	 */
	public Set<String> getLocationsContaining(String str) {
		return contains(str) ?
				Collections.unmodifiableSet( map.get(str).keySet() ) : Collections.emptySet();
	}
	
	/**
	 * Returns an unmodifiable view of all the positions where a string shows up in a given location
	 * @param str string
	 * @param location location
	 * @return An unmodifiable view of all the positions where a string shows up in a given location
	 */
	public Set<Integer> getPositionsOfStringInLocation(String str, String location) {
		return contains(str, location) ?
				Collections.unmodifiableSet( map.get(str).get(location) ) : Collections.emptySet();
	}
	
	/**
	 * Returns whether the index contains a given string
	 * @param str string
	 * @return Whether the index contains a given string
	 */
	public boolean contains(String str) {
		return map.get(str) != null;
	}
	
	/**
	 * Returns whether the index contains a given string and a given file that holds it
	 * @param str string
	 * @param location name of place where str was found
	 * @return Whether the index contains a given string and a given file that holds that string
	 */
	public boolean contains(String str, String location) {
		return contains(str) && map.get(str).get(location) != null;
	}
	
	/**
	 * Returns whether the index contains a given position num, in a given file, that holds a given string
	 * @param str string
	 * @param location name of place where str was found
	 * @param position position num of the given string
	 * @return Whether the index contains a given position num, in a given file, that holds a given string
	 */
	public boolean contains(String str, String location, int position) {
		return contains(str, location) && map.get(str).get(location).contains(position);
	}
	
	/**
	 * Returns the number of strings in the index
	 * @return The number of strings in the index
	 */
	public int numOfStrings() {
		return map.size();
	}
	
	/**
	 * Returns the number of locations containing a given string
	 * @param str string
	 * @return The number of locations containing a given string. If there's no mapping, returns 0.
	 */
	public int numOfLocationsContainingStrings(String str) {
		return contains(str) ? map.get(str).size() : 0;
	}
	
	/**
	 * Returns the number of times a given string appears in a given location
	 * @param str string
	 * @param location location
	 * @return The number of times a given string appears in a given location. If there's no mapping, returns 0.
	 */
	public int numOfTimesStringAppearsInLocation(String str, String location) {
		return contains(str, location) ? map.get(str).get(location).size() : 0;
	}
	
	@Override
	public String toString() {
		return map.toString();
	}
	
	/**
	 * Creates a JSON version of this index, output to a string
	 * @return A JSON version of this index, output to a string
	 */
	public String toJson() {
		return SearchJsonWriter.asStringMapStringMapIntCollection(map);
	}
	
	/**
	 * Creates a JSON version of this index, output to a path
	 * @param path path
	 * @throws IOException In case IOError occurs
	 */
	public void toJson(Path path) throws IOException {
		SearchJsonWriter.asStringMapStringMapIntCollection(map, path);
	}
}
