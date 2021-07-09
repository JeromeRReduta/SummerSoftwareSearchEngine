import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Search Engine servlet
 * @author JRRed
 *
 */
public class SearchEngineServlet extends HttpServlet{
	/** Unused ID */
	private static final long serialVersionUID = 1L;

	/** Logger */
	public static Logger log = LogManager.getLogger();
	
	/** For convenience */
	public static final String TITLE = "Search";
	
	/** For convenience */
	public static final String QUERY = "query";
	
	/** For convenience */
	public static final String defaultSeed = "https://www.cs.usfca.edu/~cs212/simple/";
	
	/** For convenience */
	public static final String DARK_MODE = "darkMode";
	
	/** For convenience */
	public static final String EXACT = "exact";

	// Start of this is copied from HeaderServer hw
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log.info("{} handling request: {}", Thread.currentThread().getName(), request.getRequestURI());
		PrintWriter out = response.getWriter();
		response.setContentType("text/html");
		
		int base = 0;
		out.printf("%s<html>%n", "\t".repeat(base));
		out.printf("%s<head>%n", "\t".repeat(base + 1));
		out.printf("%s<meta charset=%s>%n", "\t".repeat(base + 2), "\"utf-8\"");
		out.printf("%s<title>%s</title>%n", "\t".repeat(base + 2), TITLE);
		printCSS(request, out, base);
		out.printf("%s</head>%n", "\t".repeat(base + 1));
		
		out.printf("%s<body>%n", "\t".repeat(base + 1));
		printPTag(out, "<b>Search engine</b>", base + 2);
		printPTag(out, "Searching from seed html: <a href=\"" + defaultSeed + "\">" + defaultSeed + "</a> (TENTATIVE)", base + 2);
		printPTag(out, "Handled by thread: " + Thread.currentThread().getId(), base + 2);

		if (request.getParameter(QUERY) != null) {
			printSearchResults(request, out, base + 2);
		}
		
		out.printf("%s<form method=\"get\" action=\"/search\">%n", "\t".repeat(base + 2));
		printPTag(out, "<input type=\"text\" name=\"" + QUERY + "\" maxlength=\"2000\" size=\"75\"/>", base + 3);
		printPTag(out, "<input type=\"submit\" value=\"SEARCH\">", base + 3);
		printToggleButton(request, out, DARK_MODE, "Enable dark mode", base + 2);
		printToggleButton(request, out, EXACT, "Search with exact words only", base + 2);
		out.printf("%s</form>", "\t".repeat(base + 2));
		
		out.printf("%s</body>%n", "\t".repeat(base + 1));
		out.printf("%s</html>%n", "\t".repeat(base));
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Convenience method for printing p tags
	 * @param writer PrintWriter
	 * @param message message
	 * @param indent base indent level
	 */
	private void printPTag(PrintWriter writer, String message, int indent) {
		writer.printf("\t".repeat(indent) + "<p>%s</p>%n", message);
	}
	
	/**
	 * Convenience method - prints the CSS based on the request
	 * @param request HttpServletRequest
	 * @param out PrintWriter
	 * @param base base indent level
	 */
	private void printCSS(HttpServletRequest request, PrintWriter out, int base) {
		out.printf("%s<style>%n", "\t".repeat(base + 2));
	
		if (request.getParameter(DARK_MODE) != null) {
			out.printf("%sbody {color: white; background-color: #383838; font-family: Comic Sans MS;}%n", "\t".repeat(base + 2));
			out.printf("%sa:link {color: #00CED1;}", "\t".repeat(base + 2));
			out.printf("%sa:visited {color: #EE82EE;}", "\t".repeat(base + 2));
		}
		else {
			out.printf("%sbody {font-family: Comic Sans MS;}%n", "\t".repeat(base + 2));
		}
		out.printf("%s</style>%n", "\t".repeat(base + 2));
	}
	
	// 
	/**
	 * Prints a toggle button based on the request
	 * @param request HttpServletRequest
	 * @param out PrintWriter
	 * @param parameter value used
	 * @param buttonName name to output to the button
	 * @param indent base indent level
	 * 
	 * @note Found this for dark mode checkbox: https://stackoverflow.com/questions/36546775/html-checkboxes-keep-checked-after-refresh/36547079
		Checkbox: https://www.w3schools.com/tags/att_input_type_checkbox.asp
	 */
	private void printToggleButton(HttpServletRequest request, PrintWriter out, String parameter, String buttonName, int indent) {
		String text = request.getParameter(parameter) != null
				? "<input type=\"checkbox\" id=\"" + parameter + "\" name=\"" + parameter + "\" checked>"
				: "<input type=\"checkbox\" id=\"" + parameter + "\" name=\"" + parameter + "\">";
		
		out.printf("%s%s%n", "\t".repeat(indent), text);
		out.printf("%s<label for=\"" + parameter + "\">" + buttonName + "</label><br>", "\t".repeat(indent));
	}
	
	/**
	 * Prints search results
	 * @param request HttpServletRequest
	 * @param out PrintWriter
	 * @param indent base indent level
	 */
	private void printSearchResults(HttpServletRequest request, PrintWriter out, int indent) {
		String safeInput = StringEscapeUtils.escapeHtml4( request.getParameter(QUERY) );
		if ( safeInput.isBlank() ) return;
		
		printPTag(out, "Query is: " + safeInput, indent);
		String args = "-html " + defaultSeed + " -max 10 " + " -threads 5" + " -query " + safeInput;
		
		if (request.getParameter(EXACT) != null) {
			args += " -exact";
		}
		
		SearchEngine searchEngine = SearchEngine.Factory.create( new ArgumentMap( args.split(" ") ) );
		
		try {
			Instant start = Instant.now();
			searchEngine.getStems();
			searchEngine.searchFrom(safeInput);
			searchEngine.joinQueue();
			printPTag(out, "RESULTS: \n" + searchEngine.outputResultsToWeb(start), indent);
		}
		catch (Exception e) {
			log.catching(Level.ERROR, e);
		}
	}
}
