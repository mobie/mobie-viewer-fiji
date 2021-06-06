package de.embl.cba.mobie.n5;

import de.embl.cba.mobie.n5.open_organelle.OpenOrganelleN5S3ImageLoader;
import de.embl.cba.mobie.n5.open_organelle.OpenOrganelleS3Reader;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class S3Reader {
    private static boolean logChunkLoading;

    private final String serviceEndpoint;
    private final String signingRegion;
    private final String bucketName;

    public S3Reader( String serviceEndpoint, String signingRegion, String bucketName )
    {
        this.serviceEndpoint = serviceEndpoint;
        this.signingRegion = signingRegion;
        this.bucketName = bucketName;
    }

    public static void setLogChunkLoading( boolean logChunkLoading )
    {
        S3Reader.logChunkLoading = logChunkLoading;
        if ( logChunkLoading ) IJ.run("Console");
    }

    public SpimData readKey(String key ) throws IOException
    {
        S3Reader.logChunkLoading = true;
        OpenOrganelleN5S3ImageLoader imageLoader = new OpenOrganelleN5S3ImageLoader( serviceEndpoint, signingRegion, bucketName, key );
        return new SpimData( null, Cast.unchecked( imageLoader.getSequenceDescription() ), imageLoader.getViewRegistrations() );
    }

    public static SpimData readURL( String url ) throws IOException
    {
        final String[] split = url.split( "/" );
        String serviceEndpoint = Arrays.stream( split ).limit( 3 ).collect( Collectors.joining( "/" ) );
        String signingRegion = "us-west-2";
        String bucketName = split[ 3 ];
        final String key = Arrays.stream( split ).skip( 4 ).collect( Collectors.joining( "/") );
        S3Reader.logChunkLoading = true;
        final OpenOrganelleS3Reader reader = new OpenOrganelleS3Reader( serviceEndpoint, signingRegion, bucketName );
        return reader.readKey( key );
    }
}
