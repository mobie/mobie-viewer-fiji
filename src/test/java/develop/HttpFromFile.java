package develop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpFromFile
{
    public static void main( String[] args ) throws IOException, URISyntaxException
    {
        System.out.println( new URI( "https://s3.embl.de/ik2-2022/image.ome.zarr"  ) );
        print( new File( "https://s3.embl.de/ik2-2022/image.ome.zarr" ) );
        print( new File( "/Volumes/tischi/image.ome/zarr" ) );
    }

    private static void print( File file ) throws IOException
    {
        System.out.println( "String : " + file.toString());
        System.out.println( "URI: " + file.toURI());
        System.out.println( "Abs Path: " + file.getAbsolutePath());
        System.out.println( "Canonical Path: " + file.getCanonicalPath());
    }
}
