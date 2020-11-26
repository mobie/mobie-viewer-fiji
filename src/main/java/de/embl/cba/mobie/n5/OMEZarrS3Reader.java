package de.embl.cba.mobie.n5;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.BdvUtils;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Cast;

import java.io.IOException;

public class OMEZarrS3Reader
{
	private final String serviceEndpoint;
	private final String signingRegion;
	private final String bucketName;

	public OMEZarrS3Reader( String serviceEndpoint, String signingRegion, String bucketName )
	{
		this.serviceEndpoint = serviceEndpoint;
		this.signingRegion = signingRegion;
		this.bucketName = bucketName;
	}

	public SpimData readSpimData( String key ) throws IOException
	{
		N5S3ZarrImageLoader imageLoader = new N5S3ZarrImageLoader( serviceEndpoint, signingRegion, bucketName, key, S3Authentication.Anonymous );
		SpimData spimData = new SpimData( null, Cast.unchecked( imageLoader.getSequenceDescription() ), imageLoader.getViewRegistrations() );
		return spimData;
	}

	public static void main( String[] args ) throws IOException
	{
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
		SpimData em = reader.readSpimData( "em-raw.ome.zarr" );
		BdvHandle bdvHandle = BdvFunctions.show( em ).get( 0 ).getBdvHandle();
		SpimData myosin = reader.readSpimData( "prospr-myosin.ome.zarr" );
		BdvStackSource< ? > bdvStackSource = BdvFunctions.show( myosin, BdvOptions.options().addTo( bdvHandle ) ).get( 0 );
		bdvStackSource.setColor( new ARGBType( ARGBType.rgba( 255, 0,0, 255 ) ) );

		//SpimData myosin = reader.readSpimData( "prospr-myosin.ome.zarr" );
		//BdvFunctions.show( myosin );
	}
}
