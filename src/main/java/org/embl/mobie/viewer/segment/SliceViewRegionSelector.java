package org.embl.mobie.viewer.segment;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRow;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.annotate.AnnotatedMaskAdapter;
import org.embl.mobie.viewer.bdv.GlobalMousePositionProvider;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.MergedGridSource;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;

public class SliceViewRegionSelector implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	private Supplier< Collection< AnnotationDisplay > > annotatedRegionDisplaySupplier;

	public SliceViewRegionSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< AnnotationDisplay > > annotatedRegionDisplaySupplier )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.annotatedRegionDisplaySupplier = annotatedRegionDisplaySupplier;
	}

	public synchronized void clearSelection()
	{
		final Collection< AnnotationDisplay > regionDisplays = getCurrent();

		for ( AnnotationDisplay regionDisplay : regionDisplays )
		{
			regionDisplay.selectionModel.clearSelection();
		}
	}

	private Collection< AnnotationDisplay > getCurrent()
	{
		return annotatedRegionDisplaySupplier.get();
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final GlobalMousePositionProvider positionProvider = new GlobalMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint position = positionProvider.getPositionAsRealPoint();

		final Collection< AnnotationDisplay > regionDisplays = annotatedRegionDisplaySupplier.get();

		for ( AnnotationDisplay regionDisplay : regionDisplays )
		{
			final Collection< SourceAndConverter< ? > > sourceAndConverters = regionDisplay.sourceNameToSourceAndConverter.values();

			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				if ( ! bdvHandle.getViewerPanel().state().isSourceVisible( sourceAndConverter ) )
				{
					continue;
				}

				if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
				{
					final Source< ? > source = sourceAndConverter.getSpimSource();

					final double pixelValue = getPixelValue( timePoint, position, source );
					final String sourceName = getSourceName( source, pixelValue );
					double labelIndex = getLabelIndex( source, pixelValue );

					TableRow tableRow = getTableRow( timePoint, regionDisplay, sourceName, labelIndex );

					if ( tableRow != null )
					{
						regionDisplay.selectionModel.toggle( tableRow );

						if ( regionDisplay.selectionModel.isSelected( tableRow ) )
						{
							//regionDisplay.
							regionDisplay.selectionModel.focus( tableRow, this );
						}
					}
				}
			}
		}
	}

	private TableRow getTableRow( int timePoint, AnnotationDisplay regionDisplay, String sourceName, double labelIndex )
	{
		if ( regionDisplay instanceof SegmentationDisplay )
		{
			final boolean containsSegment = (( SegmentationDisplay ) regionDisplay ).segmentAdapter.containsSegment( labelIndex, timePoint, sourceName );

			if ( ! containsSegment )
			{
				// This happens when there is a segmentation without
				// a segment table, or when the background label has been selected
				return null;
			}
			else
			{
				return ( ( SegmentationDisplay ) regionDisplay ).segmentAdapter.getSegment( labelIndex, timePoint, sourceName );
			}
		}
		else if ( regionDisplay instanceof RegionDisplay )
		{
			final RegionDisplay annotatedSourceDisplay = ( RegionDisplay ) regionDisplay;
			final AnnotatedMaskAdapter adapter = annotatedSourceDisplay.annotatedMaskAdapter;
			return adapter.getAnnotatedMask( timePoint, labelIndex );
		}
		else
		{
			throw new UnsupportedOperationException( "Region display of type " + regionDisplay.getClass().getName() + " is not supported for selection.");
		}
	}

	private static double getLabelIndex( Source< ? > source, double pixelValue )
	{
		if ( MergedGridSource.instanceOf( source ) )
		{
			return SourceNameEncoder.getValue( Double.valueOf( pixelValue ).longValue() );
		}
		else
		{
			return pixelValue;
		}
	}

	private static String getSourceName( Source< ? > source, double labelIndex )
	{
		if ( MergedGridSource.instanceOf( source ) )
		{
			return SourceNameEncoder.getName( Double.valueOf( labelIndex ).longValue() );
		}
		else
		{
			final String sourceName = source.getName();
			return sourceName;
		}
	}

	@Deprecated
	private Collection< SourceAndConverter< ? > > getContainedSourceAndConverters( SourceAndConverter< ? > sourceAndConverter )
	{
		final Collection< SourceAndConverter< ? > > containedSourceAndConverters = new HashSet<>();

		Source< ? > source = sourceAndConverter.getSpimSource();

		if ( source instanceof LabelSource )
		{
			source = ( ( LabelSource ) source ).getWrappedSource();
		}

		if ( source instanceof TransformedSource )
		{
			source = ( ( TransformedSource< ? > ) source ).getWrappedSource();
		}

		if ( source instanceof MergedGridSource )
		{
			containedSourceAndConverters.addAll( ( ( MergedGridSource ) source ).getContainedSourceAndConverters() );
		}
		else
		{
			// not a MergedGridSource, just return the initial source
			containedSourceAndConverters.add( sourceAndConverter );
		}

		return containedSourceAndConverters;
	}

	private static double getPixelValue( int timePoint, RealPoint position, Source< ? > source )
	{
		final RandomAccess< RealType > randomAccess = ( RandomAccess< RealType > ) source.getSource( timePoint, 0 ).randomAccess();
		final long[] positionInSource = SourceAndConverterHelper.getVoxelPositionInSource( source, position, timePoint, 0 );
		randomAccess.setPosition( positionInSource );
		return randomAccess.get().getRealDouble();
	}

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}
