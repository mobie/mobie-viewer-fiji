package de.embl.cba.platynereis;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.Metadata;
import de.embl.cba.tables.image.SourceAndMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import javax.print.attribute.standard.Media;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.platynereis.utils.FileUtils.getFiles;

public class PlatynereisImageSourcesModel implements ImageSourcesModel
{
	public static final String LABELS_FILE_ID = "-labels" ;
	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "em-raw-";
	public static final String tablesFolder = "tables";

	private Map< String, SourceAndMetadata< ? > > imageIdToSourceAndMetadata;
	private final File dataFolder;

	public PlatynereisImageSourcesModel( File dataFolder )
	{
		this.dataFolder = dataFolder;
		addSources();
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


	private void addSources()
	{
		imageIdToSourceAndMetadata = new HashMap<>();
		List< File > imageFiles = getFiles( dataFolder, ".*.xml" );
		for ( File imageFile : imageFiles )
			addSource( imageFile );
	}

	private Metadata getMetadata( File imageSourceFile )
	{
		final String imageId = imageId( imageSourceFile );
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

			final File table = new File( getTablePath( imageId ) );
			if ( table.exists() )
				metadata.segmentsTable = table;
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
		else
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 1000.0D;
		}
	}

	private String getTablePath( String sourceName )
	{
		return dataFolder + File.separator + tablesFolder + File.separator + sourceName + ".csv";
	}

	private static String imageId( File file )
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

	private void addSource( File file )
	{
		final String imageId = imageId( file );
		final LazySpimSource lazySpimSource = new LazySpimSource( imageId, file );
		final Metadata metadata = getMetadata( file );
		imageIdToSourceAndMetadata.put( imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
	}

}
