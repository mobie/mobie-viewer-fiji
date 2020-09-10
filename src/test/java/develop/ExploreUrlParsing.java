package develop;

import java.net.URI;
import java.net.URISyntaxException;

public class ExploreUrlParsing
{
	public static void main( String[] args ) throws URISyntaxException
	{
		URI uri = new URI("http://www.stackoverflow.com/path/to/something");

		URI parent = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");

		System.out.println( parent.toString());
	}
}
