import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class Driver {
	
	/** Default port */
	public static final int PORT = 8080;
	
	/** Logger */
	public static Logger log = LogManager.getLogger();
	
	/** Static representation of commandline args, for use w/ server */
	private static String[] args;

	// Note: THIS IS PROJECT 4 BRANCH - SWITCH TO MAIN ONE ONCE DESIGN PASSES
	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		// store initial start time
		Instant start = Instant.now();

		// Creating objects
		ArgumentMap argMap = new ArgumentMap(args);
		SearchEngine searchEngine = SearchEngine.Factory.create(argMap);
		Server server = null;
		
		/* turn this section into searchEngine.getStems():
		 * have a couple different search engines:
		 * 
		 * Single-threaded w/ String seed (turned to Path)
		 * Multi-threaded w/ String seed (turned to Path)
		 * Web search engine w/ String seed (turned to URL)
		 * 
		 * Then when running the searchEngine funcs, just change String to Path/URL as needed
		 * 
		 */
		if (argMap.hasFlag("-server")) {
			log.info("OH GOD IT'S SERVER TIME");
			server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setHost("localhost");
			connector.setPort(argMap.getInteger("-server", PORT));
			
			ServletHandler handler = new ServletHandler();
			handler.addServletWithMapping(SearchEngineServlet.class, "/search");
			
			server.addConnector(connector);
			server.setHandler(handler);
			try {
				Driver.args = args;
				server.start();
				log.info("Server started: {}", server.getState());

			}
			catch (Exception e) {
				System.out.println("Oh no - server time");
			}
			
		}
		
		
		
		try {
			searchEngine.getStems();
		}
		catch (Exception e) {
			System.err.printf( "Could not get stems from path: %s%n", searchEngine.getSeed() );
		}
		

		
		if (argMap.hasFlag("-query")) {
			final Path query = argMap.getPath("-query");
			
			try {
				searchEngine.searchFrom(query);
			}
			catch (NullPointerException e) {
				System.err.printf("Error: query path is missing or invalid: %s%n", query);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not search inverted index with path: %s%n", query);
			}
			
		}
		
		if (argMap.hasFlag("-index")) { // Print InvertedIndex data to file (in JSON format)
			final Path index = argMap.getPath( "-index", Path.of("index.json") );
			
			try {
				searchEngine.outputIndexTo(index);
			}
			catch (IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", index);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output inverted index data to file: %s%n", index);
			}
		}
		
		if (argMap.hasFlag("-counts")) { // Prints file string count data to file (in JSON format)
			final Path counts = argMap.getPath( "-counts", Path.of("counts.json") );
			
			try {
				searchEngine.outputWordCountsTo(counts);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealing with path: %s%n", counts);
			}
			catch(Exception e) {
				System.err.printf("Error: Could not output string count data to file: %s%n", counts);
			}
			
		}
		
		if (argMap.hasFlag("-results")) {
			final Path results = argMap.getPath( "-results", Path.of("results.json") );
			
			try {
				searchEngine.outputResultsTo(results);
			}
			catch(IOException e) {
				System.err.printf("Error: Error occurred while dealign with path: %s%n", results);
			}
			catch (Exception e) {
				System.err.printf("Error: Could not output search result data to file: %s%n", results);
			}
		}
		
		
		searchEngine.joinQueue();
		
		try {
			if (server != null) server.join();
		}
		catch (Exception e) {
			System.err.println("Error - server time");
		}
		
		// calculate time elapsed and output
		Duration elapsed = Duration.between(start, Instant.now());
		double seconds = (double) elapsed.toMillis() / Duration.ofSeconds(1).toMillis();
		System.out.printf("Elapsed: %f seconds%n", seconds);
	}

	/**
	 * Search Engine servlet
	 * @author JRRed
	 *
	 */
	public static class SearchEngineServlet extends HttpServlet{
		/** Unused ID */
		private static final long serialVersionUID = 1L;
		
		/** For convenience */
		public static final String TITLE = "Search";
		
		/** For convenience */
		public static final String QUERY = "query";
		
		
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
			
			ArgumentMap argMap = new ArgumentMap(Driver.args);
			String defaultSeed = argMap.getString("-html");
			int safeThreads = argMap.getInteger("-threads",  WorkQueue.DEFAULT);
			safeThreads = safeThreads <= 0 ? WorkQueue.DEFAULT : safeThreads;
			int base = 0;
			
			out.printf("%s<html>%n", "\t".repeat(base));
			out.printf("%s<head>%n", "\t".repeat(base + 1));
			out.printf("%s<meta charset=%s>%n", "\t".repeat(base + 2), "\"utf-8\"");
			out.printf("%s<title>%s</title>%n", "\t".repeat(base + 2), TITLE);
			printCSS(request, out, base);
			out.printf("%s</head>%n", "\t".repeat(base + 1));
			
			out.printf("%s<body>%n", "\t".repeat(base + 1));
			printPTag(out, "<b>Search engine</b>", base + 2);
			printPTag(out, "Searching from seed html: <a href=\"" + defaultSeed + "\">" + defaultSeed + "</a>", base + 2);
			printPTag(out, "Crawling up to " + argMap.getInteger("-max", 1) + " unique links", base + 2);
			printPTag(out, "Using " + safeThreads + " threads", base + 2);
			printPTag(out, "Handled by thread: " + Thread.currentThread().getId(), base + 2);

			if (request.getParameter(QUERY) != null) {
				printSearchResults(request, out, base + 2, argMap);
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
		 * @param argMap ArgumentMap
		 */
		private void printSearchResults(HttpServletRequest request, PrintWriter out, int indent, ArgumentMap argMap) {

			String safeInput = StringEscapeUtils.escapeHtml4( request.getParameter(QUERY) );
			if ( safeInput.isBlank() ) return;
			
			
			
			printPTag(out, "Query is: " + safeInput, indent);
			
			if (request.getParameter(EXACT) != null && !argMap.hasFlag("-exact")) {
				argMap.parse(new String[] {"-exact"});
			}
			
			SearchEngine searchEngine = SearchEngine.Factory.create(argMap);
			
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

	
	
}
