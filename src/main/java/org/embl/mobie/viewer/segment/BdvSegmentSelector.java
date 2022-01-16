package org.embl.mobie.viewer.segment;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.type.volatiles.VolatileUnsignedIntType;
import org.embl.mobie.io.n5.source.LabelSource;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.SourceNameEncoder;
import org.embl.mobie.viewer.bdv.BdvMousePositionProvider;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.transform.MergedGridSource;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public class BdvSegmentSelector implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	private Supplier< Collection< SegmentationSourceDisplay > > segmentationDisplaySupplier;

	// A segmentationDisplaySupplier is used such that the segmentation images can
	// change during runtime.
	public BdvSegmentSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< SegmentationSourceDisplay > > segmentationDisplaySupplier )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.segmentationDisplaySupplier = segmentationDisplaySupplier;
	}

	public synchronized void clearSelection()
	{
		final Collection< SegmentationSourceDisplay > segmentationDisplays = getCurrent();

		for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplays )
		{
			segmentationDisplay.selectionModel.clearSelection();
		}
	}

	private Collection< SegmentationSourceDisplay > getCurrent()
	{
		return segmentationDisplaySupplier.get();
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final BdvMousePositionProvider positionProvider = new BdvMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint position = positionProvider.getPosition();

		final Collection< SegmentationSourceDisplay > segmentationDisplays = segmentationDisplaySupplier.get();

		for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplays )
		{
			final Collection< SourceAndConverter< ? > > segmentationSourceAndConverters = segmentationDisplay.sourceNameToSourceAndConverter.values();

			for ( SourceAndConverter< ? > sourceAndConverter : segmentationSourceAndConverters )
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

					if ( labelIndex == 0 ) continue; // image background

					final boolean containsSegment = segmentationDisplay.segmentAdapter.containsSegment( labelIndex, timePoint, sourceName );

					if ( ! containsSegment )
					{
						// This happens when there is a segmentation without
						// a segment table
						continue;
					}

					final TableRowImageSegment segment = segmentationDisplay.segmentAdapter.getSegment( labelIndex, timePoint, sourceName );

					segmentationDisplay.selectionModel.toggle( segment );

					if ( segmentationDisplay.selectionModel.isSelected( segment ) )
					{
						segmentationDisplay.selectionModel.focus( segment );
					}
				}
			}
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
		final String sourceName = source.getName();
		return sourceName;
	}

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
