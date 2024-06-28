package develop;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class DevelopUriAndFile
{
    public static void main( String[] args ) throws URISyntaxException
    {
        URI uri = new URI( "/Users/tischer/Desktop/Fiji/Fiji-MoBIE.app/jars" );
        System.out.println( new File( uri ) );
    }
}
