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
	
	// TODO: Move this to SearchResult when you make SearchResult data private
	private static void asSearchResult(InvertedIndex.SearchResult element, Writer writer, int level) throws IOException { // TODO: Turn this and all other search result JSON funcs private once done testing
		writer.write("{");
		writer.write( indentStringBy('"' + "where" + '"' + ": " + '"' + element.location + '"' +  ",",
				level + 1));
		writer.write( indentStringBy('"' + "count" + '"' + ": " + element.count + ",",
				level + 1));
		writer.write( indentStringBy('"' + "score" + '"' + ": " + String.format("%.8f", element.score) + "\n",
				level + 1));
		indent("}", writer, level);
	}
	
	private static void asSearchResultCollection(Collection<InvertedIndex.SearchResult> elements, Writer writer, int level) throws IOException {
		if (elements == null) return; // Note: Deal w/ elements.isEmpty() filtering in indexSearcher
		var entries = elements.iterator();
		
		writer.write("["); // Start of list and head value
		if (entries.hasNext()) {
			writer.write( "\n" + "\t".repeat(level + 1 ));
			asSearchResult( entries.next(), writer, level + 1 );
		}
		
		while (entries.hasNext()) { // All other values
			writer.write( ",\n" + "\t".repeat(level + 1 ));
			asSearchResult( entries.next(), writer, level + 1 );
		}
		writer.write("\n"); // End of list
		indent("]", writer, level);
	}
	
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
	
	public static void asSearchResultMap(Map<String, Collection<InvertedIndex.SearchResult>>elements, Path path) throws IOException {
		FunctionalWriter.writeToFile(elements, path, (elem, writer) -> asSearchResultMap(elem, writer, 0));
	}
		
	public static String asSearchResultMap(Map<String, Collection<InvertedIndex.SearchResult>>elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asSearchResultMap(elem, writer, 0));
	}
}
