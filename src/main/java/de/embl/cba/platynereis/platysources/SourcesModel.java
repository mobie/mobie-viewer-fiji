package de.embl.cba.platynereis.platysources;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import net.imglib2.type.numeric.ARGBType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.platynereis.utils.Utils.createRandom;

public class SourcesModel implements ImageSourcesModel
{
	public static final String SEGMENTATION_ID = "-segmented" ;
	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "-raw";
	public static final String EM_ID = "em-";
	public static final String XRAY_ID = "xray-";
	public static final String MASK_FILE_ID = "mask-";

	private Map< String, SourceAndMetadata< ? > > imageIdToSourceAndMetadata;
	private final String imageDataLocation;
	private final String tableDataLocation;
	private GlasbeyARGBLut glasbeyARGBLut;
	private String storageModality;
	private String imageRootLocation;

	public SourcesModel(
			String imageDataLocation,
			String tableDataLocation )
	{
		this.imageDataLocation = imageDataLocation;
		this.tableDataLocation = tableDataLocation;

		imageIdToSourceAndMetadata = new HashMap<>();
		glasbeyARGBLut = new GlasbeyARGBLut();

		addSources( imageDataLocation );
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
		final String imagesJsonLocation = FileAndUrlUtils.combinePath( imageDataLocation, "images/images.json" );

		try
		{
			InputStream is = FileAndUrlUtils.getInputStream( imagesJsonLocation );

			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
			addSourcesFromJson( imageDataLocation, reader );
			reader.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new UnsupportedOperationException();
		}
	}

	private void addSourcesFromJson( String imageDataLocation, JsonReader reader )
	{
		storageModality = imageDataLocation.startsWith( "http" ) ? "remote" : "local";
		imageRootLocation = FileAndUrlUtils.combinePath( imageDataLocation, "images" );

		GsonBuilder builder = new GsonBuilder();
		LinkedTreeMap imageIdsToMetadata = builder.create().fromJson(reader, Object.class);

		final Set< String > imageIds = imageIdsToMetadata.keySet();
		for ( String imageId : imageIds )
		{
			LinkedTreeMap imageAttributes = ( LinkedTreeMap ) imageIdsToMetadata.get( imageId );

			final LinkedTreeMap storage = (LinkedTreeMap) imageAttributes.get( "Storage" );
			if ( ! storage.keySet().contains( storageModality ) ) continue;

			final Metadata metadata = getMetadata( imageId, imageAttributes );

			final LazySpimSource lazySpimSource = new LazySpimSource( imageId, metadata.xmlLocation );
			imageIdToSourceAndMetadata.put( imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
			Sources.sourceToMetadata.put( lazySpimSource, metadata );
		}

		if ( imageIdToSourceAndMetadata.size() == 0)
		{
			throw new UnsupportedOperationException( "No image data found in: "
					+ FileAndUrlUtils.combinePath( imageDataLocation, "images", storageModality ) );
		}

		Utils.log("Found " + imageIdToSourceAndMetadata.size() + " image sources." );

	}

	public Metadata getMetadata( String imageId, LinkedTreeMap imageAttributes )
	{
		final Metadata metadata = new Metadata( imageId );
		metadata.numSpatialDimensions = 3;
		metadata.displayName = imageId;
		setImageModality( imageId, metadata );

		final Set< String > metadataKeys = imageAttributes.keySet();
		for ( String key : metadataKeys )
			addImageMetadata( metadata, key, imageAttributes.get( key ) );

		return metadata;
	}

	public void addImageMetadata( Metadata metadata, String key, Object value )
	{
		if ( key.equals( "TableFolder" ) )
		{
			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, (String) value, "default.csv");
		}
		else if ( key.equals( "Color" ) )
		{
			final String colorString = ( String ) value;

			if ( colorString.equals("RandomFromGlasbey") )
			{
				metadata.color = ColorUtils.getColor(
						new ARGBType( glasbeyARGBLut.getARGB(
								createRandom( metadata.imageId ) ) ) );
			}
			else
			{
				metadata.color = Utils.getColor( colorString );
			}
		}
		if ( key.equals( "Tables" ) )
		{
			final ArrayList< String > tableNames = ( ArrayList< String > ) value ;
			for ( String tableName : tableNames )
				metadata.additionalSegmentTableNames.add( tableName );
		}
		else if ( key.equals( "ColorMap" ) )
		{
			metadata.colorMap = (String) value;
		}
		else if ( key.equals( "ColorByColumn" ) )
		{
			metadata.colorByColumn = (String) value;
		}
		else if ( key.equals( "Type" ) )
		{
			metadata.type = Metadata.Type.valueOf( (String) value );
		}
		else if ( key.equals( "MinValue" ) )
		{
			metadata.displayRangeMin = (double) value;
		}
		else if ( key.equals( "MaxValue" ) )
		{
			metadata.displayRangeMax = (double) value;
		}
		else if ( key.equals( "ColorMapMinValue" ) )
		{
			metadata.colorMapMin = (double) value;
		}
		else if ( key.equals( "ColorMapMaxValue" ) )
		{
			metadata.colorMapMax = (double) value;
		}
		else if ( key.equals( "Storage" ) )
		{
			final LinkedTreeMap treeMap = ( LinkedTreeMap ) value;
			metadata.xmlLocation = FileAndUrlUtils.combinePath( imageRootLocation, (String) treeMap.get( storageModality ) );
		}
		else if ( key.equals( "SelectedLabelIds" ) )
		{
			metadata.selectedSegmentIds = ( ArrayList<Double> ) value;
		}
		else if ( key.equals( "ShowSelectedSegmentsIn3d" ) )
		{
			metadata.showSelectedSegmentsIn3d = (boolean) value;
		}
		else if ( key.equals( "ShowImageIn3d" ) )
		{
			metadata.showImageIn3d = (boolean) value;
		}
		else
		{
			// skip unknown key
		}
	}


	@Deprecated
	public void addImageMetadataOldStyle( JsonReader reader, Metadata metadata ) throws IOException
	{
		final String nextName = reader.nextName();
		if ( nextName.equals( "TableFolder" ) )
		{
			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, reader.nextString(), "default.csv");
		}
		else if ( nextName.equals( "Color" ) )
		{
			metadata.color = Utils.getColor( reader.nextString() );
		}
		else if ( nextName.equals( "MinValue" ) )
		{
			metadata.displayRangeMin = reader.nextDouble();
		}
		else if ( nextName.equals( "MaxValue" ) )
		{
			metadata.displayRangeMax = reader.nextDouble();
		}
		else if ( nextName.equals( "PainteraProject" ) )
		{
			reader.nextString();
		}
		else
		{
			reader.nextNull();
			throw new UnsupportedOperationException( "Unexpected key in images.json: " + nextName );
		}
	}

	private void setImageModality( String imageId, Metadata metadata )
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
