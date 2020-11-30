package de.embl.cba.mobie.n5.zarr;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.mobie.n5.S3Authentication;
import de.embl.cba.mobie.n5.source.Sources;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Cast;

import java.io.IOException;
import java.util.List;

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

	public SpimData read( String key ) throws IOException
	{
		N5S3OMEZarrImageLoader imageLoader = new N5S3OMEZarrImageLoader( serviceEndpoint, signingRegion, bucketName, key, S3Authentication.Anonymous );
		SpimData spimData = new SpimData( null, Cast.unchecked( imageLoader.getSequenceDescription() ), imageLoader.getViewRegistrations() );
		return spimData;
	}

	public static void main( String[] args ) throws IOException
	{
		int practical = 1;

		switch ( practical )
		{
			case 0: // show myosin
				showMyosin();
				break;
			case 1: // show myosin and add em and labels
				showAll();
				break;
		}
	}

	public static void showAll() throws IOException
	{
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
		SpimData myosin = reader.read( "prospr-myosin.ome.zarr" );
		List< BdvStackSource< ? > > myosinBdvSources = BdvFunctions.show( myosin );
		SpimData em = reader.read( "em-raw.ome.zarr" );
		List< BdvStackSource< ? > > sources = BdvFunctions.show( em, BdvOptions.options().addTo( myosinBdvSources.get( 0 ).getBdvHandle() ) );
		Sources.showAsLabelMask( sources.get( 1 ) );
	}

	public static void showMyosin() throws IOException
	{
		// N5OMEZarrImageLoader.debugLogging = true;
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
		SpimData myosin = reader.read( "prospr-myosin.ome.zarr" );
		BdvFunctions.show( myosin );
	}
}
