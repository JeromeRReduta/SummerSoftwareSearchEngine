import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 * Extension of SimpleJsonWriter; this one is focused on Search Engine data structures
 * @author JRRed
 *
 */
public class SearchJsonWriter extends SimpleJsonWriter {
	
	public static void asInvertedIndex(InvertedIndex index, Writer writer, int level) throws IOException {
		
		if (index == null) return;
		
		var entries = index.get().entrySet().iterator();
		
		writer.write("{");
		
		if (entries.hasNext()) { // Head case
			var entry = entries.next();
			writer.write( indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asNestedArray(entry.getValue(), writer, level + 1);
		}
		while (entries.hasNext()) { // All others
			var entry = entries.next();
			writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": ",
					level + 1));
			asNestedArray(entry.getValue(), writer, level + 1);
		}
		writer.write("\n"); // Tail
		indent("}", writer, level);
	}
	
		
		/*
		public static void asNestedArray(Map<String, ? extends Collection<Integer>> elements, 
				Writer writer, int level) throws IOException {

			var entries = elements.entrySet().iterator();
			
			writer.write("{");
			
			if (entries.hasNext()) { // Head case
				var entry = entries.next();
				writer.write( indentStringBy('"' + entry.getKey() + '"' + ": ",
						level + 1));

				asArray(entry.getValue(), writer, level + 1);
			}
			while (entries.hasNext()) { // All others
				var entry = entries.next();
				writer.write( "," + indentStringBy('"' + entry.getKey() + '"' + ": ",
						level + 1));

				asArray(entry.getValue(), writer, level + 1);
			}
			writer.write("\n"); // Tail
			indent("}", writer, level);
		}
		
		*/
		
		
		
	public static void asInvertedIndex(InvertedIndex index, Path path) throws IOException {
		FunctionalWriter.writeToFile(index, path, (elem, writer) -> asInvertedIndex(elem, writer, 0));
	}
	
	public static String asInvertedIndex(InvertedIndex index) {
		return FunctionalWriter.writeToString(index, (elem, writer) -> asInvertedIndex(elem, writer, 0));
	}

}
