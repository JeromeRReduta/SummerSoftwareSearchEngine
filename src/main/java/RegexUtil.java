/**
 * Class whose sole responsibility is to provide useful regexes and regex functions
 * @author JRRed
 *
 */
public class RegexUtil {
	/** Represents the case-insensitive and single-line flags. The default flags
	 * of this class */
	public final static String flags = "(?is)";
	
	/** Represents the regex /&#38;(word chars)	&#59;/ + flags */
	public final static String htmlEntity = flags + "&\\w*?;";
	
	/** Represents the regex /&#60;(anything)&#62;/ + flags */
	public final static String htmlTag = flags + "<.*?>";
	
	/** Represents the regex /&#60;!--(\s)*?(anything)(\s)*?--&#62;*/
	public final static String htmlComment = flags + "<!--(\s)*?(.)*?(\s)*?-->";
	
	/**
	 * Creates a regex with the format &#60;\\s*?name.*?&#60;/name\\s*?&#62;
	 * @param name name of element
	 * @return A regex with the format &#60;\\s*?name.*?&#60;/name\\s*?&#62;
	 */
	public static String getHtmlElement(String name) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(flags);
		buffer.append("<\\s*?");
		buffer.append(name);
		buffer.append(".*?");
		buffer.append("</");
		buffer.append(name);
		buffer.append("\\s*?>");
		return buffer.toString();
	}
}
