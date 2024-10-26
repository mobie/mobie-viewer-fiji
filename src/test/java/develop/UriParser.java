package develop;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class UriParser
{
    public static void main( String[] args ) throws URISyntaxException, MalformedURLException
    {
        parseUri( new URI( "file:/Volumes/test" ) );
        parseUri( new URI( "/Volumes/test" ) );
        parseUri( new URI( "https://fiji.sc" ) );
        parseUri( new URI("C:/test") );
    }

    public static void parseUri( URI uri ) throws MalformedURLException, URISyntaxException
    {
        if ( uri.getScheme() == null )
        {
            System.out.println("--");
            System.out.println("Missing schema, treating as file." );
            treatAsFile( new URI( "file:" + uri ) );
        }
        else if ( uri.getScheme().contains( "file" ) )
        {
            System.out.println("--");
            treatAsFile( uri );
        }
        else if ( uri.getScheme().contains( "http" ) )
        {
            System.out.println("--");
            String address = uri.toURL().toString();
            System.out.println("http address: " + address);
        }
        else
        {
            System.out.println("--");
            System.out.println("Unknown schema: " + uri.getScheme() + ", treating as File." );
            treatAsFile( new URI( "file:" + uri ) );
        }
    }

    private static void treatAsFile( URI uri ) throws MalformedURLException
    {
        String path = uri.toURL().getPath();
        File file = new File( path );
        System.out.println("File path: " + file.getAbsolutePath() );
    }
}
