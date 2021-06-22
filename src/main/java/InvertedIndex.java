import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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
	
	/** map tracking how many strings are in each location */
	private final Map<String, Integer> stringCount;
	
	/**
	 * Constructor
	 * @note Shouldn't make a builder here, b/c the point isn't to create
	 * a new thing with many complex things, but to populate an already existing
	 * thing with data
	 */
	public InvertedIndex() {
		map = new TreeMap<>();
		stringCount = new TreeMap<>();
	}
	
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
		
		stringCount.merge(location,  position, Math::max);
	}
	
	/**
	 * Returns an unmodifiable view of all the strings in the index
	 * @return An unmodifiable view of all the strings in the index
	 * @note Shouldn't use Collections.unmodifiableMap(), b/c unmodifiableX() only makes outermost layer of X unmodifiable - inner structures are still mutable
	 */
	public Set<String> getStrings() {
		return Collections.unmodifiableSet( map.keySet() );
	}
	
	/**
	 * Returns an unmodifiable view of the strings in the index, starting with a given string (or if that string isn't in the map,
	 * starting with the first string in the map after the given string)
	 * @param start string
	 * @return An unmodifiable view of the index. If the string exists in the map, this set will start with that given string. Otherwise,
	 * this set will start with the first string in the map that comes after the given string.
	 */
	public Set<String> getStringsStartingWith(String start) {
		return Collections.unmodifiableSet( map.tailMap(start).keySet() );
	}
	
	/**
	 * Returns an unmodifiable view of all the locations that contain the given string
	 * @param str string
	 * @return An unmodifiable view of all the locations containing the given string
	 */
	public Set<String> getLocationsContaining(String str) {
		return contains(str)
				? Collections.unmodifiableSet( map.get(str).keySet() ) : Collections.emptySet();
	}
	
	/**
	 * Returns an unmodifiable view of all the positions where a string shows up in a given location
	 * @param str string
	 * @param location location
	 * @return An unmodifiable view of all the positions where a string shows up in a given location
	 */
	public Set<Integer> getPositionsOfStringInLocation(String str, String location) {
		return contains(str, location)
				? Collections.unmodifiableSet( map.get(str).get(location) ) : Collections.emptySet();
	}
	
	/**
	 * Returns an unmodifiable view of the number of times each string appears in the index
	 * @return An unmodifiable view of the number of times each string appears in the index
	 */
	public Map<String, Integer> getStringCount() {
		return Collections.unmodifiableMap(stringCount);
	}
	
	/**
	 * Returns how many times a given string appears in the index
	 * @param location location
	 * @return How many times a given string appears in the index
	 */
	public int numOfTimesStringAppearsInLocation(String location) {
		return stringCount.get(location) != null ? stringCount.get(location) : 0;
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
	public int numOfLocationsContainingString(String str) {
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
	 * @throws IOException In case IO Error occurs
	 */
	public void toJson(Path path) throws IOException {
		SearchJsonWriter.asStringMapStringMapIntCollection(map, path);
	}
	
	/**
	 * Creates a JSON version of the string count map, as a string
	 * @return A JSON version of the string count map, as a string
	 */
	public String stringCountsToJson() {
		return SimpleJsonWriter.asObject(stringCount);
	}
	
	/**
	 * Outputs a JSON version of the string count map to a file
	 * @param path file path
	 * @throws IOException In case IO Error occurs
	 */
	public void stringCountsToJson(Path path) throws IOException {
		SimpleJsonWriter.asObject(stringCount, path);
	}
	
	/* TODO 
	
	public List<SearchResult> exactSearch(Set<String> queries) {
		List<InvertedIndex.SearchResult> results;
		Map<String, InvertedIndex.SearchResult> lookup;
		
		same logic as updateMatches except access the private data directly
		
		Collections.sort(results);
		return results;
	}
	
	public List<SearchResult> partialSearch(Set<String> queries) {
		List<InvertedIndex.SearchResult> results;
		Map<String, InvertedIndex.SearchResult> lookup;
		
		for each query
			for key in the tailMap of map
				if startswith
					update the results
					same logic as updateMatches except access the private data directly
				else
					break
		
		Collections.sort(results);
		return results;
	}
	*/

	/**
	 * Class whose sole responsibility is to hold data gained from searching the index
	 * @author JRRed
	 */
	public class SearchResult implements Comparable<SearchResult> {
		
		/** location where a match is found */
		private final String location;
		
		/** number of matches in this result's location */
		private int count;

		/** This result's score, defined as the number of matches / the number of words in the result's location */
		private double score;
		
		/**
		 * Constructor
		 * @param location location of stem
		 */
		public SearchResult(String location) { // TODO private
			this.location = location;
			this.count = 0;
			this.score = 0;
		}
		
		/**
		 * Updates a search result based on a given query
		 * @param query query
		 */
		public void update(String query) { // TODO private
			update(query, 1);
		}
		
		/**
		 * Updates a search result based on a given query, multiplied by the multiplier. This is generally used for partial stem search,
		 * which double-counts shared partial stems
		 * @param query query
		 * @param multiplier multiplier
		 */
		public void update(String query, int multiplier) { // TODO private
			count += numOfTimesStringAppearsInLocation(query,  location) * multiplier;
			score = (double)count/stringCount.get(location);
		}

		@Override
		public int compareTo(InvertedIndex.SearchResult other) {
			int sameScore = Double.compare(other.score, this.score);
			if (sameScore != 0) return sameScore;
			
			int sameCount = Integer.compare(other.count,  this.count);
			if (sameCount != 0) return sameCount;
			
			return this.location.compareToIgnoreCase(other.location);
		}
		
		@Override
		public String toString() {
			return "'Where': " + location + "\n" +
					"'Count': " + count + "\n" +
					"'Score': " + String.format("%.8f", score) + "\n";
		}
		
		/**
		 * Outputs this SearchResult to a String, to JSON format
		 * @return This SearchResult to a String, to JSON format
		 */
		public String toJson() {
			return SimpleJsonWriter.FunctionalWriter.writeToString(this, (elem, writer) -> this.toJson(writer, 0));
		}
		
		/**
		 * Writes this search result in JSON format
		 * @param writer writer to use
		 * @param level base indent level
		 * @throws IOException in case of IO Error
		 */
		public void toJson(Writer writer, int level) throws IOException {
			writer.write("{");
			writer.write( SimpleJsonWriter.indentStringBy('"' + "where" + '"' + ": " + '"' + location + '"' +  ",",
					level + 1));
			writer.write( SimpleJsonWriter.indentStringBy('"' + "count" + '"' + ": " + count + ",",
					level + 1));
			writer.write( SimpleJsonWriter.indentStringBy('"' + "score" + '"' + ": " + String.format("%.8f", score) + "\n",
					level + 1));
			SimpleJsonWriter.indent("}", writer, level);
		}
	}
}
