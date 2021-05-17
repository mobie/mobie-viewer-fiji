package de.embl.cba.mobie.image;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.mobie.ui.MoBIESettings;
import de.embl.cba.mobie.utils.Enums;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourcesModel implements ImageSourcesModel
{
	public static final String EM_ID = "em-";
	public static final String XRAY_ID = "xray-";
	private final MoBIESettings.ImageDataStorageModality imageDataStorageModality;

	private Map< String, SourceAndMetadata< ? > > nameToSourceAndDefaultMetadata;
	private final String tableDataLocation;
	private String imageStorageModality;
	private String imageRootLocation;

	public SourcesModel( String imageDataLocation, MoBIESettings.ImageDataStorageModality imageDataStorageModality, String tableDataLocation )
	{
		this.imageDataStorageModality = imageDataStorageModality;
		this.tableDataLocation = tableDataLocation;

		nameToSourceAndDefaultMetadata = new HashMap<>();

		fetchSources( imageDataLocation );
	}

	@Override
	public Map< String, SourceAndMetadata< ? > > sources()
	{
		return nameToSourceAndDefaultMetadata;
	}

	@Override
	public boolean is2D()
	{
		return false;
	}

	private void fetchSources( String imageDataLocation )
	{
		try
		{
			final ImagesJsonParser parser = new ImagesJsonParser( imageDataLocation );
			final Map< String, ImageProperties > nameToImageProperties = parser.getImagePropertiesMap();
			addSources( imageDataLocation, nameToImageProperties );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new UnsupportedOperationException();
		}
	}

	// TODO: Simplify this: directly create a List< MoBIESource >
	//   by means of JSON parsing
	private void addSources( String imageDataLocation, Map< String, ImageProperties > nameToImageProperties )
	{
		// TODO: this can probably be simplified somehow...
		imageStorageModality = imageDataStorageModality.equals( MoBIESettings.ImageDataStorageModality.S3 ) ? "remote" : "local";

		imageRootLocation = FileAndUrlUtils.combinePath( imageDataLocation, "images" );

		final Set< String > names = nameToImageProperties.keySet();

		for ( String name : names )
		{
			final ImageProperties properties = nameToImageProperties.get( name );

			final Metadata metadata = initMetadata( name );
			setModality( name, metadata );
			setXmlLocation( properties, metadata );
			setDefaultTableLocation( properties, metadata );

			metadata.type = Enums.valueOf( Metadata.Type.class, properties.type );

			final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
			adapter.setMetadataFromMutableImageProperties( metadata, properties );

			final LazySpimSource lazySpimSource = new LazySpimSource( name, metadata.xmlLocation );
			nameToSourceAndDefaultMetadata.put( name, new SourceAndMetadata( lazySpimSource, metadata ) );
			Sources.sourceToMetadata.put( lazySpimSource, metadata );
		}

		if ( nameToSourceAndDefaultMetadata.size() == 0)
		{
			throw new UnsupportedOperationException( "No image data found in: "
					+ FileAndUrlUtils.combinePath( imageDataLocation, "images", imageStorageModality ) );
		}

		Utils.log("Found " + nameToSourceAndDefaultMetadata.size() + " image sources." );
	}

	private Metadata initMetadata( String name )
	{
		final Metadata metadata = new Metadata( name );
		metadata.numSpatialDimensions = 3;
		metadata.displayName = name;
		return metadata;
	}

	private void setXmlLocation( ImageProperties properties, Metadata metadata )
	{
		if ( imageStorageModality == "remote")
			metadata.xmlLocation = FileAndUrlUtils.combinePath( imageRootLocation, properties.storage.remote );
		else
			metadata.xmlLocation = FileAndUrlUtils.combinePath( imageRootLocation, properties.storage.local );
	}

	private void setDefaultTableLocation( ImageProperties properties, Metadata metadata )
	{
		if ( properties.tableFolder != null )
		{
			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, properties.tableFolder, "default.csv" );
		}
	}

	private void setModality( String imageId, Metadata metadata )
	{
		if ( imageId.contains( EM_ID ) )
		{
			metadata.modality = Metadata.Modality.EM;
		}
		else if ( imageId.contains( XRAY_ID ) )
		{
			metadata.modality = Metadata.Modality.XRay;
		}
		else
		{
			metadata.modality = Metadata.Modality.FM;
		}
	}
}
