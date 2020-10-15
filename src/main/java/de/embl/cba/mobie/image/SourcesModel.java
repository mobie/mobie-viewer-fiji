package de.embl.cba.mobie.image;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.utils.Enums;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourcesModel implements ImageSourcesModel
{
	public static final String SEGMENTATION_ID = "-segmented" ;
	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "-raw";
	public static final String EM_ID = "em-";
	public static final String XRAY_ID = "xray-";
	public static final String MASK_FILE_ID = "mask-";
	private final MoBIEOptions.ImageDataStorageModality imageDataStorageModality;

	private Map< String, SourceAndMetadata< ? > > nameToSourceAndDefaultMetadata;
	private final String tableDataLocation;
	private GlasbeyARGBLut glasbeyARGBLut;
	private String storageModality;
	private String imageRootLocation;

	public SourcesModel( String imageDataLocation, MoBIEOptions.ImageDataStorageModality imageDataStorageModality, String tableDataLocation )
	{
		this.imageDataStorageModality = imageDataStorageModality;
		this.tableDataLocation = tableDataLocation;

		nameToSourceAndDefaultMetadata = new HashMap<>();
		glasbeyARGBLut = new GlasbeyARGBLut();

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

	private void addSources( String imageDataLocation, Map< String, ImageProperties > nameToImageProperties )
	{
		storageModality = imageDataStorageModality.equals( MoBIEOptions.ImageDataStorageModality.S3 ) ? "remote" : "local";
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
					+ FileAndUrlUtils.combinePath( imageDataLocation, "images", storageModality ) );
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
		if ( storageModality == "remote")
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

	// TODO: remove this. Should an LayerPropertiesToMetadataAdaptor
	public Metadata getMetadata( String imageId, LinkedTreeMap imageAttributes )
	{
		final Metadata metadata = initMetadata( imageId );
		setModality( imageId, metadata );

		final Set< String > metadataKeys = imageAttributes.keySet();
		for ( String key : metadataKeys )
			addImageMetadata( metadata, key, imageAttributes.get( key ) );

		return metadata;
	}

	@Deprecated
	public void addImageMetadata( Metadata metadata, String key, Object value )
	{
//		if ( key.equals( "TableFolder" ) )
//		{
//			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, (String) value, "default.csv");
//		}
//		else if ( key.equals( "Color" ) )
//		{
//			final String colorString = ( String ) value;
//
//			if ( colorString.equals("RandomFromGlasbey") )
//			{
//				metadata.color = ColorUtils.getColor(
//						new ARGBType( glasbeyARGBLut.getARGB(
//								createRandom( metadata.imageId ) ) ) );
//			}
//			else
//			{
//				metadata.color = Utils.getColor( colorString );
//			}
//		}
//		if ( key.equals( "Tables" ) )
//		{
//			final ArrayList< String > tableNames = ( ArrayList< String > ) value ;
//			for ( String tableName : tableNames )
//				metadata.additionalSegmentTableNames.add( tableName );
//		}
//		else if ( key.equals( "ColorMap" ) )
//		{
//			metadata.colorMap = (String) value;
//		}
//		else if ( key.equals( "ColorByColumn" ) )
//		{
//			metadata.colorByColumn = (String) value;
//		}
//		else if ( key.equals( "Type" ) )
//		{
//			metadata.type = Metadata.Type.valueOf( (String) value );
//		}
//		else if ( key.equals( "MinValue" ) )
//		{
//			metadata.displayRangeMin = (double) value;
//		}
//		else if ( key.equals( "MaxValue" ) )
//		{
//			metadata.displayRangeMax = (double) value;
//		}
//		else if ( key.equals( "ColorMapMinValue" ) )
//		{
//			metadata.colorMapMin = (double) value;
//		}
//		else if ( key.equals( "ColorMapMaxValue" ) )
//		{
//			metadata.colorMapMax = (double) value;
//		}
//		else if ( key.equals( "Storage" ) )
//		{
//			final LinkedTreeMap treeMap = ( LinkedTreeMap ) value;
//			metadata.xmlLocation = FileAndUrlUtils.combinePath( imageRootLocation, (String) treeMap.get( storageModality ) );
//		}
//		else if ( key.equals( "SelectedLabelIds" ) )
//		{
//			metadata.selectedSegmentIds = ( ArrayList<Double> ) value;
//		}
//		else if ( key.equals( "ShowSelectedSegmentsIn3d" ) )
//		{
//			metadata.showSelectedSegmentsIn3d = (boolean) value;
//		}
//		else if ( key.equals( "ShowImageIn3d" ) )
//		{
//			metadata.showImageIn3d = (boolean) value;
//		}
//		else
//		{
//			// skip unknown key
//		}
	}


	@Deprecated
	public void addImageMetadataOldStyle( JsonReader reader, Metadata metadata ) throws IOException
	{
//		final String nextName = reader.nextName();
//		if ( nextName.equals( "TableFolder" ) )
//		{
//			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, reader.nextString(), "default.csv");
//		}
//		else if ( nextName.equals( "Color" ) )
//		{
//			metadata.color = Utils.getColor( reader.nextString() );
//		}
//		else if ( nextName.equals( "MinValue" ) )
//		{
//			metadata.displayRangeMin = reader.nextDouble();
//		}
//		else if ( nextName.equals( "MaxValue" ) )
//		{
//			metadata.displayRangeMax = reader.nextDouble();
//		}
//		else if ( nextName.equals( "PainteraProject" ) )
//		{
//			reader.nextString();
//		}
//		else
//		{
//			reader.nextNull();
//			throw new UnsupportedOperationException( "Unexpected key in images.json: " + nextName );
//		}
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
