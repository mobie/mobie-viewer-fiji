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
		int practical = 3;

		switch ( practical )
		{
			case 0: // show myosin
				showMyosin();
				break;
			case 1: // show myosin and add em and labels
				showAll();
				break;
			case 2:
				readI2KGif();
				break;
			case 3:
				//  /idr/zarr/v0.1/6001237.zarr
				N5OMEZarrImageLoader.debugLogging = true;
				OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embassy.ebi.ac.uk", "us-west-2", "idr" );
				SpimData image = reader.read( "zarr/v0.1/6001237.zarr" );
				List< BdvStackSource< ? > > sources = BdvFunctions.show( image );
				sources.get( 0 ).setColor( new ARGBType( ARGBType.rgba( 0,0,255,255 ) ) );
				sources.get( 0 ).setDisplayRange( 0, 3000 );
				sources.get( 1 ).setColor( new ARGBType( ARGBType.rgba( 0,255,0,255 ) ) );
				sources.get( 1 ).setDisplayRange( 0, 3000 );
				sources.get( 2 ).setColor( new ARGBType( ARGBType.rgba( 255,0,0,255 ) ) );
				sources.get( 2 ).setDisplayRange( 0, 3000 );
				sources.get( 3 ).setColor( new ARGBType( ARGBType.rgba( 255,255,255,255 ) ) );
				sources.get( 3 ).setDisplayRange( 0, 3000 );
				//sources.get( 4 ).setDisplayRange( 0, 100 );
				Sources.showAsLabelMask( sources.get( 4 ) );
				break;
		}
	}

	public static void readI2KGif() throws IOException
	{
		// https://play.minio.io:9000/i2k2020/gif.zarr
		N5OMEZarrImageLoader.debugLogging = true;
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://play.minio.io:9000", "us-west-2", "i2k2020" );
		SpimData image = reader.read( "gif.zarr" );
		BdvFunctions.show( image );
	}

	public static void showAll() throws IOException
	{
		N5OMEZarrImageLoader.debugLogging = true;
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
		SpimData myosin = reader.read( "prospr-myosin.ome.zarr" );
		List< BdvStackSource< ? > > myosinBdvSources = BdvFunctions.show( myosin );
		SpimData em = reader.read( "em-raw.ome.zarr" );
		List< BdvStackSource< ? > > sources = BdvFunctions.show( em, BdvOptions.options().addTo( myosinBdvSources.get( 0 ).getBdvHandle() ) );
		Sources.showAsLabelMask( sources.get( 1 ) );
	}

	public static void showMyosin() throws IOException
	{
		N5OMEZarrImageLoader.debugLogging = true;
		OMEZarrS3Reader reader = new OMEZarrS3Reader( "https://s3.embl.de", "us-west-2", "i2k-2020" );
		SpimData myosin = reader.read( "prospr-myosin.ome.zarr" );
		BdvFunctions.show( myosin );
	}
}
