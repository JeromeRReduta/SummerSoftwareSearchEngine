import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	a new thing with many complex things, but to populate an already existing
	thing with data
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
	
	public Stream<String> getStemsMatching(String stem) {
		Stream.of(stem).filter(w -> !map.keySet().contains(w));
		new ArrayList<>().removeIf(w -> !map.keySet().contains(w));
		return map.containsKey(stem) ? Stream.of(stem) : Stream.empty();
	}
	
	public Stream<String> getStemsStartingWith(String stem) {
		
		//return map.keySet().stream().filter( str -> str.startsWith(stem) );
		
		List<String> partialStems = new ArrayList<>();
		partialStems.add(stem);
		
		var it = map.tailMap(stem, false).keySet().iterator();
		
		String current;
		
		while ( it.hasNext() && (current = it.next()).startsWith(stem) ) {
			partialStems.add(current);
		}
		
		return partialStems.stream();
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
	
	
	
	public Collection<SearchResult> exactSearch(Set<String> queries) {
		
		/*
		var results = new ArrayList<SearchResult>();
		var lookup = new HashMap<String, SearchResult>();
		
		for (String query : queries) { // for each stem in query set
			if (map.containsKey(query)) {
				// updateResults(map.get(query).keySet(), results, lookup, query);
				
				Set<String> pathNames = map.get(query).keySet();
				for (String pathName : pathNames) {
					
					if (!lookup.containsKey(pathName)) {
						SearchResult result = new SearchResult(pathName);
						result.count++;
						lookup.put(pathName,  result);
						results.add(result);
					}
					
					.numOfTimesStringAppearsInLocation(stem, pathName)
					
					//lookup.get(pathName).updateMatches(query) // Increases matches & score numbers
				}
			}
		}
		
		Collections.sort(results);
		return results;
		
		*/
		
		/* Functional approach */
		return queries.stream()
				.flatMap( query ->contains(query) ? map.get(query).keySet().stream() : Stream.empty() )
				.distinct()
				.map( pathName -> new SearchResult(pathName, queries) )
				.collect( Collectors.toCollection( TreeSet::new ) );
		/* Imperative approach */
		
		/*
		HashSet<String> pathsContainingAQuery = new HashSet<>();
		TreeSet<SearchResult> results = new TreeSet<>();
		
		for (String query : queries) {
			if ( contains(query) ) {
				pathsContainingAQuery.addAll( map.get(query).keySet() );
			}
		}
		for (String path : pathsContainingAQuery) {
			results.add( new SearchResult(path, queries) );
		}
		
		return results;
		*/
	}
	

	/**
	 * Class whose sole responsibility is to hold data gained from searching the index
	 * @author JRRed
	 * @note All vars here are public and final (except the Formatter, which devs probably won't care about). This is because 
	 * the sole purpose of this class is to hold data that other blocks of code will want to see, and once these vars are set
	 * we will never have to change them again (if we want a new result, we'll just search again, getting a new SearchResult)
	 *
	 */
	public class SearchResult implements Comparable<SearchResult> {
		
		public final String location;
		public final int count;
		public final double score;
		
		
		public SearchResult(String location, Set<String> querySet) {
			this.location = location;
			
			int tempCount = 0;
			for (String query : querySet) {
				tempCount += numOfTimesStringAppearsInLocation(query, location);
			}
			this.count = tempCount;
			
			this.score = this.count > 0 ? (double)this.count / stringCount.get(location) : 0.0;
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

		
	}
}
