import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A specialized version of {@link HttpsFetcher} that follows redirects and
 * returns HTML content if possible.
 *
 * @see HttpsFetcher
 * 
 * @author CS 212 Software Development
 * @author University of San Francisco
 * @version Summer 2021
 */
public class HtmlFetcher {
	
	/** For convenience and consistency */
	private static final String CONTENT_TYPE = "Content-Type";
	
	/** For convenience and consistency */
	private static final String TEXT_HTML = "text/html";
	
	/** For convenience and consistency */
	private static final String LOCATION = "Location";
	
	// Note: Do we need to put content into headers, or can we just return HttpsFetcher.getContent(...) as is? Check this again when adding this to project
	// private static final String CONTENT = "Content";
	
	/**
	 * Returns {@code true} if and only if there is a "Content-Type" header and
	 * the first value of that header starts with the value "text/html"
	 * (case-insensitive).
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isHtml(Map<String, List<String>> headers) {
		List<String> type = headers.get(CONTENT_TYPE);
		return type != null
				&& type.get(0).toLowerCase().startsWith( TEXT_HTML.toLowerCase() );
	}

	/**
	 * Parses the HTTP status code from the provided HTTP headers, assuming the
	 * status line is stored under the {@code null} key.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return the HTTP status code or -1 if unable to parse for any reasons
	 */
	public static int getStatusCode(Map<String, List<String>> headers) {
		List<String> statusCodeList = headers.get(null);
		if (statusCodeList == null || statusCodeList.get(0) == null) return -1;
		
		String code = statusCodeList.get(0).split("\s+")[1]; // Split by whitespace, then get the element w/ index 1 (the actual code number)
		return Integer.parseInt(code);
	}

	/**
	 * Returns {@code true} if and only if the HTTP status code is between 300 and
	 * 399 (inclusive) and there is a "Location" header with at least one value.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isRedirect(Map<String, List<String>> headers) {
		int statusCode = getStatusCode(headers);
		boolean statusCodeIsRedirect = statusCode >= 300 && statusCode <= 399;
		
		List<String> location = headers.get(LOCATION);
		boolean hasLocation = location != null && location.size() >= 1;
		
		return statusCodeIsRedirect && hasLocation;
	}

	/**
	 * Fetches the resource at the URL using HTTP/1.1 and sockets. If the status
	 * code is 200 and the content type is HTML, returns the HTML as a single
	 * string. If the status code is a valid redirect, will follow that redirect
	 * if the number of redirects is greater than 0. Otherwise, returns
	 * {@code null}.
	 *
	 * @param url the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see HttpsFetcher#openConnection(URL)
	 * @see HttpsFetcher#printGetRequest(PrintWriter, URL)
	 * @see HttpsFetcher#getHeaderFields(BufferedReader)
	 * @see HttpsFetcher#getContent(BufferedReader)
	 *
	 * @see String#join(CharSequence, CharSequence...)
	 *
	 * @see #isHtml(Map)
	 * @see #isRedirect(Map)
	 */
	public static String fetch(URL url, int redirects) {
		if (redirects < 0) return null;
		/*
		try (
				Socket socket = HttpsFetcher.openConnection(url);
				PrintWriter request = new PrintWriter(socket.getOutputStream());
				InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				BufferedReader response = new BufferedReader(input);
		) {
			
			// Note: According to below link, might be that socket is closed while there is still writing happening
			// https://www.edureka.co/community/7308/how-does-java-net-socketexception-connection-reset-happen
			HttpsFetcher.printGetRequest(request, url); // Note: Not a print-to-console func - DO NOT DELETE
			
			System.out.println(Thread.currentThread().getId() + " - After printGetRequest - This is good so far");
			Map<String, List<String>> headers = HttpsFetcher.getHeaderFields(response);

			
			System.out.println(Thread.currentThread().getId() + " - After getHeaderFields - This is good so far");
			*/
		try {
			Map<String, List<String>> headers = HttpsFetcher.fetchURL(url);

			
		
			if ( isRedirect(headers) ) {
				String newLocation = headers.get(LOCATION).get(0);
				
				System.out.println(Thread.currentThread().getId() + " - returning fetch");
				return fetch(newLocation, redirects - 1);
			}
			else if ( isHtml(headers) && getStatusCode(headers) == 200 ) {
				// Note: Do we need to put content into headers, or can we just return HttpsFetcher.getContent(...) as is? Check this again when adding this to project
				/*
				headers.put(CONTENT,  HttpsFetcher.getContent(response));
				return String.join("\n", headers.get(CONTENT));
				*/
				
				System.out.println(Thread.currentThread().getId() + " - returning fetch join");
				//return String.join( "\n",  HttpsFetcher.getContent(response) );
				return String.join( "\n",  headers.get("Content") );
			}
		}
		catch (Exception e) {
			System.err.println(Thread.currentThread().getId() + " - Error - HTMLFetcher - could not fetch html:%n" );
			e.printStackTrace();
		}
		
		System.out.println(Thread.currentThread().getId() + " - returning null");
		return null;
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)}.
	 *
	 * @param url the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url, int redirects) {
		try {
			return fetch(new URL(url), redirects);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url) {
		return fetch(url, 0);
	}

	/**
	 * Calls {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 */
	public static String fetch(URL url) {
		return fetch(url, 0);
	}
}
