package de.embl.cba.platynereis;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.utils.FileUtils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatynereisImageSourcesModel implements ImageSourcesModel
{
	public static final String LABELS_FILE_ID = "-labels" ;
	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "-raw";
	public static final String MASK_FILE_ID = "mask-";

	private Map< String, SourceAndMetadata< ? > > imageIdToSourceAndMetadata;
	private final String tableDataLocation;

	public PlatynereisImageSourcesModel(
			String imageDataLocation,
			String tableDataLocation )
	{
		this.tableDataLocation = tableDataLocation;

		imageIdToSourceAndMetadata = new HashMap<>();

		if ( imageDataLocation.startsWith( "http" ) )
		{
			addSources( imageDataLocation );
		}
		else
		{
			final ArrayList< String > imageTypes = new ArrayList<>();
			imageTypes.add( "images" );
			imageTypes.add( "segmentations" );

			for ( String imageType : imageTypes )
			{
				final String folder = FileUtils.combinePath( imageDataLocation, imageType );
				addSources( folder );
			}
		}

	}

	@Override
	public Map< String, SourceAndMetadata< ? > > sources()
	{
		return imageIdToSourceAndMetadata;
	}

	@Override
	public boolean is2D()
	{
		return false;
	}


	private void addSources( String imageDataLocation )
	{
		List< String > imagePaths = getFilePaths( imageDataLocation );

		for ( String path : imagePaths )
			addSource( path );
	}

	private List< String > getFilePaths( String imageDataLocation )
	{
		if ( imageDataLocation.startsWith( "http" ) )
			return FileUtils.getUrls( imageDataLocation );
		else
			return FileUtils.getFiles( new File( imageDataLocation ), ".*.xml" );
	}

	private Metadata createMetadata( String path )
	{
		final String imageId = imageId( path );
		final Metadata metadata = new Metadata( imageId );
		metadata.numSpatialDimensions = 3;
		metadata.displayName = imageId;
		setDisplayRange( imageId, metadata );
		setFlavour( imageId, metadata );
		return metadata;
	}

	private void setFlavour( String imageId, Metadata metadata )
	{
		if ( imageId.contains( LABELS_FILE_ID ) )
		{
			metadata.flavour = Metadata.Flavour.LabelSource;
			metadata.segmentsTablePath = getTablePath( imageId );
		}
		else
		{
			metadata.flavour = Metadata.Flavour.IntensitySource;
		}
	}

	private void setDisplayRange( String imageId, Metadata metadata )
	{
		if ( imageId.contains( EM_RAW_FILE_ID ) )
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 255.0D;
		}
		else if ( imageId.contains( MASK_FILE_ID ))
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 1.0D;
		}
		else
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 1000.0D;
		}

	}

	private String getTablePath( String sourceName )
	{
		final String tablePath = FileUtils.combinePath( tableDataLocation, sourceName, "default.csv" );
		return tablePath;
	}

	private static String imageId( String path )
	{
		File file = new File( path );

		String dataSourceName = file.getName().replaceAll( BDV_XML_SUFFIX, "" );

		dataSourceName = getProSPrName( dataSourceName );

		return dataSourceName;
	}

	private static String getProSPrName( String dataSourceName )
	{
		return dataSourceName;
	}

	private SpimData openSpimData( File file )
	{
		try
		{
			SpimData spimData = new XmlIoSpimData().load( file.toString() );
			return spimData;
		}
		catch ( SpimDataException e )
		{
			System.out.println( file.toString() );
			e.printStackTrace();
			return null;
		}
	}

	private void addSource( String path )
	{
		final String imageId = imageId( path );
		final LazySpimSource lazySpimSource = new LazySpimSource( imageId, path );
		final Metadata metadata = createMetadata( path );
		imageIdToSourceAndMetadata.put( imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
	}

}
