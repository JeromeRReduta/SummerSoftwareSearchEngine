import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Extension of SimpleJsonWriter; this one is focused on Search Engine data structures
 * @author JRRed
 *
 */
public class SearchJsonWriter extends SimpleJsonWriter {
	/**
	 * Writes the elements as a pretty JSON object with the following format: <br>
	 * <pre>Map&lt;String, ? extends Map &lt;String, ? extends Collection &lt;Integer&gt;&gt;&gt;</pre><br>
	 * @param elements elements to write
	 * @param writer writer to use
	 * @param level initial indent level
	 * @throws IOException if an IO Error occurs
	 */
	public static void asStringMapStringMapIntCollection(Map<String, ? extends Map<String, ? extends Collection<Integer>>> elements,
			Writer writer, int level) throws IOException {
		
		if (elements == null) return;
		
		var entries = elements.entrySet().iterator();
		
		writer.write("{"); // Start of list and head value
		if (entries.hasNext()) {
			var entry = entries.next();
			writer.write( indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asNestedArray(entry.getValue(), writer, level + 1);
		}
		
		while (entries.hasNext()) { // All other values
			var entry = entries.next();
			writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asNestedArray(entry.getValue(), writer, level + 1);
		}
		
		writer.write("\n"); // End of list
		indent("}", writer, level);
	}
	
	/**
	 * Writes the elements as a pretty JSON object, where that object is a collection of SearchResults
	 * @param elements elements to write
	 * @param writer writer to use
	 * @param level initial indent level
	 * @throws IOException in case of IO Error
	 */
	private static void asSearchResultCollection(Collection<InvertedIndex.SearchResult> elements, Writer writer, int level) throws IOException {
		if (elements == null) return;
		var entries = elements.iterator();
		
		writer.write("["); // Start of list and head value
		if (entries.hasNext()) {
			writer.write( "\n" + "\t".repeat(level + 1 ));
			entries.next().toJson(writer,  level + 1);
		}
		
		while (entries.hasNext()) { // All other values
			writer.write( ",\n" + "\t".repeat(level + 1 ));
			entries.next().toJson(writer,  level + 1);
		}
		writer.write("\n"); // End of list
		indent("]", writer, level);
	}
	
	/**
	 * Writes the elements as a pretty JSON object, where that object is a map of search results, organized by the stem set used to search them by
	 * @param elements elements to write
	 * @param writer writer to use
	 * @param level initial indent level
	 * @throws IOException in case of IO Error
	 */
	public static void asSearchResultMap(Map<String, Collection<InvertedIndex.SearchResult>>elements, Writer writer, int level) throws IOException {
		if (elements == null) return;
		
		var entries = elements.entrySet().iterator();
		
		writer.write("{"); // Start of list and head value
		if (entries.hasNext()) {
			var entry = entries.next();
			writer.write( indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asSearchResultCollection(entry.getValue(), writer, level + 1 );
		}
		
		while (entries.hasNext()) { // All other values
			var entry = entries.next();
			writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asSearchResultCollection(entry.getValue(), writer, level + 1 );
		}
		
		writer.write("\n"); // End of list
		indent("}", writer, level);
	}
	
	/**
	 * {@link #asStringMapStringMapIntCollection(Map, Writer, int)} for outputting to a file
	 * @param elements elements to write
	 * @param path path to output to
	 * @throws IOException if an IO error occurs
	 */
	public static void asStringMapStringMapIntCollection(Map<String, ? extends Map<String, ? extends Collection<Integer>>> elements,
			Path path) throws IOException {
		FunctionalWriter.writeToFile(elements, path, (elem, writer) -> asStringMapStringMapIntCollection(elem, writer, 0));
	}
	
	/**
	 * {@link #asStringMapStringMapIntCollection(Map, Writer, int)} for outputting as String
	 * @param elements elements to write
	 * @return MapMapCollection, as a String in JSON format
	 */
	public static String asStringMapStringMapIntCollection(Map<String, ? extends Map<String, ? extends Collection<Integer>>> elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asStringMapStringMapIntCollection(elem, writer, 0));
	}
	
	/**
	 * {@link #asSearchResultMap(Map)} for outputting to file
	 * @param elements elements to write
	 * @param path output path
	 * @throws IOException in case of IO Error
	 */
	public static void asSearchResultMap(Map<String, Collection<InvertedIndex.SearchResult>>elements, Path path) throws IOException {
		FunctionalWriter.writeToFile(elements, path, (elem, writer) -> asSearchResultMap(elem, writer, 0));
	}
	
	/**
	 * {@link #asSearchResultMap(Map)} for outputting as String
	 * @param elements elements to write
	 * @return SearchResultMap, as a String in JSON format
	 */
	public static String asSearchResultMap(Map<String, Collection<InvertedIndex.SearchResult>>elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asSearchResultMap(elem, writer, 0));
	}
}
