package de.embl.cba.platynereis.platybrowser;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.platynereis.utils.FileUtils;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
		final String imagesJsonLocation = FileUtils.combinePath( imageDataLocation, "/images/images.json" );

		try
		{
			InputStream is = FileUtils.getInputStream( imagesJsonLocation );

			final JsonReader reader = new JsonReader( new InputStreamReader( is, "UTF-8" ) );
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
						addImageMetadata( reader, metadata );
					reader.endObject();

					//TODO: make this h5 for openning from local
					final String imageXmlUrl = FileUtils.combinePath( imageDataLocation, "images", "remote",  imageId + ".xml");

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
			reader.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new UnsupportedOperationException();
		}

	}

	public void addImageMetadata( JsonReader reader, Metadata metadata ) throws IOException
	{
		final String nextName = reader.nextName();
		if ( nextName.equals( "TableFolder" ) )
		{
			metadata.segmentsTablePath = FileUtils.combinePath( tableDataLocation, reader.nextString(), "default.csv");
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
