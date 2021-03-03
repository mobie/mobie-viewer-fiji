package de.embl.cba.mobie.image;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bookmark.Layout;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.image.SourceAndMetadata;
import net.imglib2.*;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.util.*;
import java.util.function.BiConsumer;

public class SourceGroupLabelSourceCreator
{
	public static final String SOURCE_GROUP_LABEL_IMAGE_METADATA = "SourceGroupLabelImageMetadata";
	private final String name;
	private final ArrayList< String > sourceNames;
	private final HashMap< String, SourceAndMetadata > sourcesAndMetadata;
	private Map< String, RealInterval > sourceNameToInterval;
	private Map< String, Integer > sourceNameToLabelIndex;
	private RandomAccessibleIntervalSource< IntType > labelSource;
	private Metadata metadata;

	public SourceGroupLabelSourceCreator( HashMap< String, SourceAndMetadata > sourcesAndMetadata, String name, ArrayList< String > sourceNames )
	{
		this.sourcesAndMetadata = sourcesAndMetadata;
		this.name = name;
		this.sourceNames = sourceNames;
	}

	private void prepare()
	{
		sourceNameToInterval = createIntervalMap( sourcesAndMetadata, sourceNames );

		sourceNameToLabelIndex = new HashMap<>();

		int labelIndex = 1;
		for ( String sourceName : sourceNameToInterval.keySet() )
		{
			sourceNameToLabelIndex.put( sourceName, labelIndex++ );
		}

		final RealInterval union = createUnion( sourceNameToInterval );

		BiConsumer< RealLocalizable, IntType > biConsumer = ( l, t ) ->
		{
			t.setInteger( 0 );

			for ( Map.Entry< String, RealInterval > entry : sourceNameToInterval.entrySet() )
			{
				if ( Intervals.contains( entry.getValue(), l ) )
				{
					t.setInteger( sourceNameToLabelIndex.get( entry.getKey() ) );
					return;
				}
			}
		};

		final FunctionRandomAccessible< IntType > randomAccessible =
				new FunctionRandomAccessible( 3, biConsumer, IntType::new );

		RandomAccessibleInterval< IntType > rai = Views.interval( randomAccessible, Intervals.smallestContainingInterval( union ) );

		labelSource = new RandomAccessibleIntervalSource<>( rai, Util.getTypeFromInterval( rai ), name );

		metadata = new Metadata( name );

		final SourceGroupLabelImageMetadata labelImageMetadata = new SourceGroupLabelImageMetadata();
		labelImageMetadata.sourceNameToInterval = sourceNameToInterval;
		labelImageMetadata.sourceNameToLabelIndex = sourceNameToLabelIndex;
		metadata.misc.put( SOURCE_GROUP_LABEL_IMAGE_METADATA, labelImageMetadata );
	}

	private RealInterval createUnion( Map< String, RealInterval > sourceNameToInterval )
	{
		RealInterval union = null;
		for ( String sourceName : sourceNameToInterval.keySet() )
		{
			final RealInterval interval = sourceNameToInterval.get( sourceName );
			if ( union == null )
				union = interval;
			else
				union = Intervals.union( interval, union );
		}

		return union;
	}

	public SourceAndMetadata< IntType > create()
	{
		prepare();

		final SourceAndMetadata< IntType > sourceAndMetadata = new SourceAndMetadata<>( labelSource, metadata );

		return sourceAndMetadata;
	}

	private static HashMap< String, RealInterval > createIntervalMap( HashMap< String, SourceAndMetadata > sourcesAndMetadata, ArrayList< String > sourceNames )
	{
		final HashMap< String, RealInterval > sourceNameToInterval = new HashMap<>();
		for ( String sourceName : sourceNames )
		{
			final SourceAndMetadata< ? > sourceAndMetadata = sourcesAndMetadata.get( sourceName );
			final FinalRealInterval sourceInterval = Utils.estimateBounds( sourceAndMetadata.source() );
			final AffineTransform3D transform3D = new AffineTransform3D();
			transform3D.set( sourceAndMetadata.metadata().addedTransform );

			final FinalRealInterval translatedSourceInterval = transform3D.estimateBounds( sourceInterval );
			sourceNameToInterval.put( sourceName, translatedSourceInterval );
		}
		return sourceNameToInterval;
	}
}
