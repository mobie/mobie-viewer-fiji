package de.embl.cba.platynereis;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.modelview.images.ImageSourcesModel;
import de.embl.cba.tables.modelview.images.SourceAndMetadata;
import de.embl.cba.tables.modelview.images.SourceMetadata;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import java.io.File;
import java.util.*;

public class PlatynereisImageSourcesModel implements ImageSourcesModel
{
	public static final String DEFAULT_EM_RAW_FILE_ID = "em-raw-full-res";
	public static final String DEFAULT_LABELS_FILE_ID = "em-segmented-cells-labels" ;
	public static final String LABELS_FILE_ID = "-labels" ;

	public static final String BDV_XML_SUFFIX = ".xml";
	public static final String EM_RAW_FILE_ID = "em-raw-";
	public static final String EM_FILE_ID = "em-";
	public static final String NEW_PROSPR = "-new";
	public static final String AVG_PROSPR = "-avg";

	public static final String MEDS = "-MEDs" ;
	public static final String SPMS = "-SPMs";
	public static final String OLD = "-OLD";

	private final Map< String, SourceAndMetadata > nameToSourceAndMetadata;

	public PlatynereisImageSourcesModel( File directory )
	{
		nameToSourceAndMetadata = new HashMap<>();

		List< File > imageFiles = de.embl.cba.platynereis.utils.FileUtils.getFiles( directory, ".*.xml" );

		for ( File imageFile : imageFiles )
		{
			addSource( imageFile );
		}

	}

	@Override
	public Map< String, SourceAndMetadata > sources()
	{
		return nameToSourceAndMetadata;
	}

	@Override
	public boolean is2D()
	{
		return false;
	}

	private static SourceMetadata metadataFromSpimData( File file )
	{
		final SourceMetadata metadata = new SourceMetadata( sourceName( file ) );

		metadata.displayRangeMin = 0.0D;
		metadata.displayRangeMax = 1000.0D;

		if ( file.toString().contains( LABELS_FILE_ID ) )
		{
			metadata.flavour = SourceMetadata.Flavour.LabelSource;
		}
		else
		{
			metadata.flavour = SourceMetadata.Flavour.IntensitySource;
		}

		if ( file.toString().contains( DEFAULT_EM_RAW_FILE_ID ) )
		{
			metadata.showInitially = true;
		}

		if ( file.toString().contains( DEFAULT_LABELS_FILE_ID ) )
		{
			metadata.showInitially = true;
		}

		if ( file.toString().contains( EM_RAW_FILE_ID ) )
		{
			metadata.displayRangeMin = 0.0D;
			metadata.displayRangeMax = 255.0D;
		}

		metadata.numSpatialDimensions = 3;
		metadata.displayName = sourceName( file );

		return metadata;
	}


	private static String sourceName( File file )
	{
		String dataSourceName = file.getName().replaceAll( BDV_XML_SUFFIX, "" );

		dataSourceName = getProSPrName( dataSourceName );

		return dataSourceName;
	}

	private static String getProSPrName( String dataSourceName )
	{
		if ( dataSourceName.contains( NEW_PROSPR ) )
		{
			dataSourceName = dataSourceName.replace( NEW_PROSPR, MEDS );
		}
		else if ( dataSourceName.contains( AVG_PROSPR ) )
		{
			dataSourceName = dataSourceName.replace( AVG_PROSPR, SPMS );
		}
		else if ( ! dataSourceName.contains( EM_FILE_ID ) )
		{
			dataSourceName = dataSourceName + OLD;
		}
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

	class LazySpimSource< T extends NumericType< T > > implements Source< T >
	{
		private final String name;
		private final File file;
		private Source< T > source;

		public LazySpimSource( String name, File file )
		{
			this.name = name;
			this.file = file;
		}

		private Source< T > wrappedSource()
		{
			if ( source == null )
			{
				final SpimData spimData = openSpimData( file );
				final List< ConverterSetup > converterSetups = new ArrayList<>();
				final List< SourceAndConverter< ? > > sources = new ArrayList<>();
				BigDataViewer.initSetups( spimData, converterSetups, sources );

				source = ( Source< T > ) sources.get( 0 ).asVolatile().getSpimSource();
			}

			return source;
		}

		@Override
		public boolean isPresent( int t )
		{
			if ( t == 0 ) return true;
			return false;
		}

		@Override
		public RandomAccessibleInterval< T > getSource( int t, int level )
		{
			return wrappedSource().getSource( t, level );
		}

		@Override
		public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
		{
			return wrappedSource().getInterpolatedSource( t, level, method );
		}

		@Override
		public void getSourceTransform( int t, int level, AffineTransform3D transform )
		{
			wrappedSource().getSourceTransform( t, level, transform  );
		}

		@Override
		public T getType()
		{
			return wrappedSource().getType();
		}

		@Override
		public String getName()
		{
			return wrappedSource().getName();
		}

		@Override
		public VoxelDimensions getVoxelDimensions()
		{
			return wrappedSource().getVoxelDimensions();
		}

		@Override
		public int getNumMipmapLevels()
		{
			return wrappedSource().getNumMipmapLevels();
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
