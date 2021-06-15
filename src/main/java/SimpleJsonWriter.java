import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Outputs several simple data structures in "pretty" JSON format where newlines
 * are used to separate elements and nested elements are indented using tabs.
 *
 * Warning: This class is not thread-safe. If multiple threads access this class
 * concurrently, access must be synchronized externally.
 *
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class SimpleJsonWriter {
	/**
	 * Writes the elements as a pretty JSON array.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param level the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asArray(Collection<Integer> elements, Writer writer, int level) throws IOException {
		Iterator<Integer> it = elements.iterator();
		
		writer.write("["); // Start of array and head value
		if (it.hasNext()) {
			writer.write( indentStringBy(it.next().toString(), level + 1) );
		}
		
		while (it.hasNext()) { // All other values
			writer.write( "," + indentStringBy(it.next().toString(), level + 1) );
		}
		
		writer.write("\n"); // End of list
		indent("]", writer, level);
	}

	/**
	 * Writes the elements as a pretty JSON object.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param level the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asObject(Map<String, Integer> elements, Writer writer, int level) throws IOException {
		var entries = elements.entrySet().iterator();
		
		writer.write("{"); // Start of list 
		if (entries.hasNext()) {
			var entry = entries.next();
			writer.write( indentStringBy('"' + entry.getKey() + '"' + ": " + entry.getValue().toString(),
					level + 1 ));
		}
		
		while (entries.hasNext()) { // All other values
			var entry = entries.next();
			writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": " + entry.getValue().toString(),
					level + 1 ));
		}
		
		writer.write("\n"); // End of list
		indent("}", writer, level);
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays. The
	 * generic notation used allows this method to be used for any type of map
	 * with any type of nested collection of integer objects.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param level the initial indent level
	 * @throws IOException if an IO error occurs
	 */
	public static void asNestedArray(Map<String, ? extends Collection<Integer>> elements, Writer writer, int level) throws IOException {
		var entries = elements.entrySet().iterator();
		
		writer.write("{"); // Start of list
		if (entries.hasNext()) { 
			var entry = entries.next();
			writer.write( indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asArray(entry.getValue(), writer, level + 1);
		}
		
		while (entries.hasNext()) { // All other values
			var entry = entries.next();
			writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asArray(entry.getValue(), writer, level + 1);
		}
		
		writer.write("\n"); // End of list
		indent("}", writer, level);
	}

	/**
	 * Writes the elements as a pretty JSON array to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static void asArray(Collection<Integer> elements, Path path) throws IOException {
		FunctionalWriter.writeToFile(elements, path, (elem, writer) -> asArray(elem, writer, 0));
	}

	/**
	 * Writes the elements as a pretty JSON object to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asObject(Map, Writer, int)
	 */
	public static void asObject(Map<String, Integer> elements, Path path) throws IOException {
		FunctionalWriter.writeToFile(elements, path, (elem, writer) -> asObject(elem, writer, 0));
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see #asNestedArray(Map, Writer, int)
	 */
	public static void asNestedArray(Map<String, ? extends Collection<Integer>> elements, Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			asNestedArray(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asArray(Collection, Writer, int)
	 */
	public static String asArray(Collection<Integer> elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asArray(elem, writer, 0));
	}

	/**
	 * Returns the elements as a pretty JSON object.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asObject(Map, Writer, int)
	 */
	public static String asObject(Map<String, Integer> elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asObject(elem, writer, 0));
	}

	/**
	 * Returns the elements as a pretty JSON object with nested arrays.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see #asNestedArray(Map, Writer, int)
	 */
	public static String asNestedArray(Map<String, ? extends Collection<Integer>> elements) {
		return FunctionalWriter.writeToString(elements, (elem, writer) -> asNestedArray(elem, writer, 0));
	}

	/**
	 * Indents and then writes the String element.
	 *
	 * @param element the element to write
	 * @param writer the writer to use
	 * @param level the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void indent(String element, Writer writer, int level) throws IOException {
		writer.write("\t".repeat(level));
		writer.write(element);
	}

	/**
	 * My own indent function, which plays well with string concatenation
	 * @param element element to indent
	 * @param level number of times to indent
	 * @return element, indented level times
	 * @throws IOException in case of IO error
	 */
	public static String indentStringBy(String element, int level) throws IOException {
		return "\n" + "\t".repeat(level) + element;
	}

	/**
	 * Indents and then writes the text element surrounded by {@code " "}
	 * quotation marks.
	 *
	 * @param element the element to write
	 * @param writer the writer to use
	 * @param level the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void quote(String element, Writer writer, int level) throws IOException {
		writer.write("\t".repeat(level));
		writer.write('"');
		writer.write(element);
		writer.write('"');
	}
	
	/**
	 * Private interface whose  sole responsibility is to hold convenience methods for SimpleJsonWriter
	 * @author JRRed
	 *
	 * @param <T> elements type
	 */
	interface FunctionalWriter<T> {
		/**
		 * Abstract writing function. Overridden in the static functions to call whatever writing function is needed.
		 * @param elements elements to write
		 * @param writer writer
		 * @throws IOException in case of IO error
		 */
		void write(T elements, Writer writer) throws IOException;
		
		/**
		 * Writes given elements, using the given function, as a string
		 * @param <T> type of elements
		 * @param elements elements to write
		 * @param funcWriter implementation of funcWriter interface, which comes down to defining which asX function writeToString will use
		 * @return string representation of elements, in JSON format
		 */
		public static <T> String writeToString(T elements, FunctionalWriter<T> funcWriter) {
			try (StringWriter writer = new StringWriter()){
				funcWriter.write(elements,  writer);
				return writer.toString();
			}
			catch (IOException e) {
				return null;
			}
		}
		
		/**
		 * Writes given elements, using the given function, to a file
		 * @param <T> type of elements
		 * @param elements elements to write
		 * @param path path of output file
		 * @param funcWriter implementation of funcWriter interface, which comes down to defining which as X function writeToString will use
		 * @throws IOException in case of IO error
		 */
		public static <T> void writeToFile(T elements, Path path, FunctionalWriter<T> funcWriter) throws IOException {
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				funcWriter.write(elements,  writer);
			}
		}
	}
}
