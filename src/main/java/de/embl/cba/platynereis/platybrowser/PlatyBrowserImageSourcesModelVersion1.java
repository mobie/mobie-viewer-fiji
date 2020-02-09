package de.embl.cba.platynereis.platybrowser;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlatyBrowserImageSourcesModelVersion1 implements ImageSourcesModel
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

	public PlatyBrowserImageSourcesModelVersion1(
			String imageDataLocation,
			String tableDataLocation )
	{
		this.imageDataLocation = imageDataLocation;
		this.tableDataLocation = tableDataLocation;

		imageIdToSourceAndMetadata = new HashMap<>();

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
		final String imagesJsonLocation = FileAndUrlUtils.combinePath( imageDataLocation, "/images/images.json" );

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

	@Deprecated
	private void parseJsonFileOldStyle( String imageDataLocation, JsonReader reader ) throws IOException
	{
		reader.beginObject();

		try
		{
			while ( reader.hasNext() )
			{
				final String imageId = reader.nextName();
				final Metadata metadata = new Metadata( imageId );
				metadata.numSpatialDimensions = 3;
				metadata.displayName = imageId;
				setImageModality( imageId, metadata );
				reader.beginObject();
				while ( ! reader.peek().equals( JsonToken.END_OBJECT ) )
					addImageMetadataOldStyle( reader, metadata );
				reader.endObject();

				//TODO: make this h5 for openning from local
				final String imageXmlUrl = FileAndUrlUtils.combinePath( imageDataLocation, "images", "remote",  imageId + ".xml");

				final LazySpimSource lazySpimSource = new LazySpimSource( imageId, imageXmlUrl );
				imageIdToSourceAndMetadata.put(
						imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
				Sources.sourceToMetadata.put( lazySpimSource, metadata );

			}
		} catch ( Exception e )
		{
			final String x = e.toString();
			System.err.println( x );
		}

		reader.endObject();
	}

	private void addSourcesFromJson( String imageDataLocation, JsonReader reader ) throws IOException
	{
		final String storageLocation = imageDataLocation.startsWith( "http" ) ? "remote" : "local";

		GsonBuilder builder = new GsonBuilder();
		LinkedTreeMap imageIdsToMetadata = builder.create().fromJson(reader, Object.class);

		final Set< String > imageIds = imageIdsToMetadata.keySet();
		for ( String imageId : imageIds )
		{
			LinkedTreeMap metadataKeysToValues = ( LinkedTreeMap ) imageIdsToMetadata.get( imageId );

			final LinkedTreeMap storage = (LinkedTreeMap) metadataKeysToValues.get( "Storage" );
			if ( ! storage.keySet().contains( storageLocation ) ) continue;

			final Metadata metadata = new Metadata( imageId );
			metadata.numSpatialDimensions = 3;
			metadata.displayName = imageId;
			presetDefaultMetadata( metadata );
			setImageModality( imageId, metadata );

			final Set< String > metadataKeys = metadataKeysToValues.keySet();

			for ( String key : metadataKeys )
				addImageMetadata( metadata, key, metadataKeysToValues.get( key ), storageLocation, FileAndUrlUtils.combinePath( imageDataLocation, "images" ) );

			final LazySpimSource lazySpimSource = new LazySpimSource( imageId, metadata.xmlLocation );
			imageIdToSourceAndMetadata.put( imageId, new SourceAndMetadata( lazySpimSource, metadata ) );
			Sources.sourceToMetadata.put( lazySpimSource, metadata );
		}

		if ( imageIdToSourceAndMetadata.size() == 0)
		{
			throw new UnsupportedOperationException( "No image data found in: "
					+ FileAndUrlUtils.combinePath( imageDataLocation, "images", storageLocation ) );
		}
	}

	private void presetDefaultMetadata( Metadata metadata )
	{
		metadata.displayRangeMin = 0.0;
		metadata.displayRangeMax = 1000.0;
	}

	public void addImageMetadata( Metadata metadata, String key, Object data, String storageLocation, String imageRootLocation )
	{
		if ( key.equals( "TableFolder" ) )
		{
			metadata.segmentsTablePath = FileAndUrlUtils.combinePath( tableDataLocation, (String) data, "default.csv");
		}
		else if ( key.equals( "Color" ) )
		{
			metadata.displayColor = Utils.getColor( (String) data );
		}
		else if ( key.equals( "MinValue" ) )
		{
			metadata.displayRangeMin = (double) data;
		}
		else if ( key.equals( "MaxValue" ) )
		{
			metadata.displayRangeMax = (double) data;
		}
		else if ( key.equals( "Storage" ) )
		{
			final LinkedTreeMap treeMap = ( LinkedTreeMap ) data;
			metadata.xmlLocation = FileAndUrlUtils.combinePath( imageRootLocation, (String) treeMap.get( storageLocation ) );
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
			metadata.displayColor = Utils.getColor( reader.nextString() );
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
		if ( imageId.contains( SEGMENTATION_ID ) )
		{
			metadata.modality = Metadata.Modality.Segmentation;
		}
		else if ( imageId.contains( EM_ID ) )
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
