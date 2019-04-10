package de.embl.cba.platynereis;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.modelview.images.SourceMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;
import java.util.*;

import static de.embl.cba.platynereis.utils.FileUtils.getFiles;

public class PlatynereisImageSourcesModel implements ImageSourcesModel
{
	public static final String LABELS_FILE_ID = "-labels" ;
	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "em-raw-";
	public static final String tablesFolder = "tables";

	private final Map< String, SourceAndMetadata< ? > > nameToSourceAndMetadata;
	private final File dataFolder;

	public PlatynereisImageSourcesModel( File dataFolder )
	{
		this.dataFolder = dataFolder;

		nameToSourceAndMetadata = new HashMap<>();

		List< File > imageFiles = getFiles( dataFolder, ".*.xml" );
		for ( File imageFile : imageFiles )
			addSource( imageFile );

	}

	@Override
	public Map< String, SourceAndMetadata< ? > > sources()
	{
		return nameToSourceAndMetadata;
	}

	@Override
	public boolean is2D()
	{
		return false;
	}

	private SourceMetadata metadataFromSpimData( File imageSourceFile )
	{
		final String imageId = sourceName( imageSourceFile );
		final SourceMetadata metadata = new SourceMetadata( imageId );
		metadata.displayRangeMin = 0.0D;
		metadata.displayRangeMax = 1000.0D;
		metadata.numSpatialDimensions = 3;
		metadata.displayName = imageId;

		if ( imageId.contains( EM_RAW_FILE_ID ) )
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 255.0D;
		}

		if ( imageId.contains( LABELS_FILE_ID ) )
		{
			metadata.flavour = SourceMetadata.Flavour.LabelSource;

			final File tableFile = new File( getTablePath( imageId ) );

			if ( tableFile.exists() )
				metadata.segmentsTable = tableFile;
		}
		else
		{
			metadata.flavour = SourceMetadata.Flavour.IntensitySource;
		}

		return metadata;
	}

	private String getTablePath( String sourceName )
	{
		return dataFolder + File.separator + tablesFolder + File.separator + sourceName + ".csv";
	}

	private static String sourceName( File file )
	{
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

	public void addSource( File file )
	{
		final String imageId = sourceName( file );

		final LazySpimSource lazySpimSource = new LazySpimSource( imageId, file );

		final SourceMetadata metadata = metadataFromSpimData( file );

		nameToSourceAndMetadata.put( imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
	}


}
