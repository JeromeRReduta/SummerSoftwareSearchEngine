import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
	public Set<String> get() {
		return Collections.unmodifiableSet( map.keySet() );
	}
	
	/**
	 * Returns an unmodifiable view of all the locations that contain the given string
	 * @param str string
	 * @return An unmodifiable view of all the locations containing the given string
	 */
	public Set<String> get(String str) {
		return contains(str)
				? Collections.unmodifiableSet( map.get(str).keySet() ) : Collections.emptySet();
	}
	
	/**
	 * Returns an unmodifiable view of all the positions where a string shows up in a given location
	 * @param str string
	 * @param location location
	 * @return An unmodifiable view of all the positions where a string shows up in a given location
	 */
	public Set<Integer> get(String str, String location) {
		return contains(str, location)
				? Collections.unmodifiableSet( map.get(str).get(location) ) : Collections.emptySet();
	}
	
	/**
	 * Returns an unmodifiable view of the number of times each string appears in the index
	 * @return An unmodifiable view of the number of times each string appears in the index
	 */
	public Map<String, Integer> getCounts() {
		return Collections.unmodifiableMap(stringCount);
	}

	/**
	 * Returns size of counts map, which is the number of strings in this index
	 * @return Size of counts map, which is the number of strings in this index
	 */
	public int countsSize() {
		return stringCount.size();
	}
	
	/**
	 * Returns how many times a given string appears in the index
	 * @param location location
	 * @return How many times a given string appears in the index
	 */
	public int countsSize(String location) {
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
	public int size() {
		return map.size();
	}
	
	/**
	 * Returns the number of locations containing a given string
	 * @param str string
	 * @return The number of locations containing a given string. If there's no mapping, returns 0.
	 */
	public int size(String str) {
		return contains(str) ? map.get(str).size() : 0;
	}
	
	/**
	 * Returns the number of times a given string appears in a given location
	 * @param str string
	 * @param location location
	 * @return The number of times a given string appears in a given location. If there's no mapping, returns 0.
	 */
	public int size(String str, String location) {
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
	public String countsToJson() {
		return SimpleJsonWriter.asObject(stringCount);
	}
	
	/**
	 * Outputs a JSON version of the string count map to a file
	 * @param path file path
	 * @throws IOException In case IO Error occurs
	 */
	public void countsToJson(Path path) throws IOException {
		SimpleJsonWriter.asObject(stringCount, path);
	}
	
	// Note for next time: Make this one or two funcs
	/**
	 * Attempts to merge the contents of another inverted index to this index
	 * @param other other inverted index
	 */
	public void attemptMergeWith(InvertedIndex other) {
		if (this.equals(other)) return; // check that we're not trying to merge index with itself
		
		mergeMapWith(other);
		other.stringCount.forEach((key, value) -> stringCount.merge(key, value, Math::max));
	}
	
	/**
	 * Merges map of another InvertedIndex with this one's
	 * @param other other InvertedIndex
	 */
	private void mergeMapWith(InvertedIndex other) {
		
		Set<String> otherKeys = other.map.keySet();

		for (String otherKey : otherKeys) {
			if (map.containsKey(otherKey)) {
				mergePositions(other, otherKey);
			}
			else {
				map.put(otherKey, other.map.get(otherKey));
			}
		}
		
	}
	
	/**
	 * Merges positions of this index's map and other index's map
	 * @param other other index
	 * @param otherKey key in other index's keyset
	 */
	private void mergePositions(InvertedIndex other, String otherKey) {
		
		TreeMap<String, TreeSet<Integer>> innerMap = map.get(otherKey);
		TreeMap<String, TreeSet<Integer>> otherInnerMap = other.map.get(otherKey);
		Set<String> otherPaths = otherInnerMap.keySet();
		
		for (String otherPath : otherPaths) {
			TreeSet<Integer> positions = innerMap.get(otherPath);
			TreeSet<Integer> otherPositions = otherInnerMap.get(otherPath);
			
			if (innerMap.containsKey(otherPath)) {
				positions.addAll(otherPositions);
			}
			else {
				innerMap.put(otherPath,  otherPositions);
			}
		}
		
	}
	
	/**
	 * Searches the index for each given stem and returns its results
	 * @param stems stems
	 * @return Results from this search
	 */
	public List<SearchResult> exactSearch(Set<String> stems) {
		List<SearchResult> results = new ArrayList<>();
		Map<String, SearchResult> lookup = new HashMap<>();
	
		for (String query : stems) {
			if ( !contains(query) ) continue;
			updateResults(query, lookup, results);
		}
		Collections.sort(results);
		return results;
	}
	
	/**
	 * Searches the index for each given partial stem in stems and returns its results
	 * @param stems stems
	 * @return Results from this search
	 * @note X is defined as a partial stem of Y if X starts with Y. Note that common partial stems are counted
	 * twice. E.g. if the partial stems would be ("yourselv", "yourself", "your", "yourself", "yourselv"), then
	 * "yourselv" and "yourself" are each counted twice
	 */

	public List<SearchResult> partialSearch(Set<String> stems) {
		List<SearchResult> results = new ArrayList<>();
		Map<String, SearchResult> lookup = new HashMap<>();
		
		for (String query : stems) {
			var it = map.tailMap(query).keySet().iterator();
			String current;
			
			while (it.hasNext() && (current = it.next()).startsWith(query)) {
				if ( !contains(current) ) continue;
				updateResults(current,  lookup,  results);
			}
		}
		Collections.sort(results);
		return results;
	}
	
	/**
	 * Updates a list of search results based on a query string
	 * @param query query string
	 * @param lookup lookup map, used to make sure only one SearchResult per location per search exists
	 * @param results list of search results. This function updates this list.
	 * @note Precondition: contains(query) == true (i.e. map.get(query) != null)
	 */
	private void updateResults(String query, Map<String, SearchResult> lookup, List<SearchResult> results) {
		Set<String> locations = map.get(query).keySet();
		for (String location : locations) {
			if (!lookup.containsKey(location)) {
				SearchResult result = new SearchResult(location);
				lookup.put(location, result);
				results.add(result);
			}
			lookup.get(location).update(query);
		}
	}
	
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
		private SearchResult(String location) {
			this.location = location;
			this.count = 0;
			this.score = 0;
		}
		
		/**
		 * Updates this SearchResult based on the string
		 * @param str string
		 * @note Precondition: contains(match, location) must return true
		 */
		private void update(String str) {
			this.count  += map.get(str).get(location).size();
			this.score = (double)this.count / stringCount.get(location);
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
