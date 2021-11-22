package org.embl.mobie.viewer.segment;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.viewer.MoBIE;
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
	private Supplier< Collection<SegmentationSourceDisplay> > segmentationDisplaySupplier;

	public BdvSegmentSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< SegmentationSourceDisplay > > segmentationDisplaySupplier )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.segmentationDisplaySupplier = segmentationDisplaySupplier;
	}

	public synchronized void clearSelection()
	{
		final Collection< SegmentationSourceDisplay > segmentationDisplays = segmentationDisplaySupplier.get();

		for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplays )
		{
			segmentationDisplay.selectionModel.clearSelection();
		}
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final BdvMousePositionProvider positionProvider = new BdvMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint position = positionProvider.getPosition();

		// now checking in all available sources whether the segment belongs to it.
		// the reason is that the source that is actually shown in the
		// SegmentationSourceDisplay may be a MergedGridSource, which does
		// not "hold" the individual image segments.
		// TODO: This logic should be revised!
		// TODO: only loop through all sources if the source is a MergedGridSource
		// TODO: the mergedGrid source should have a handle on all the sourceAndConverter!


//		for ( SourceAndConverter< ? > sourceAndConverter : sourceNameToSourceAndConverter.values() )
//		{
//
//
//			// TODO: check whether the source is visible!
//			if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
//			{
//				final Source< ? > source = sourceAndConverter.getSpimSource();
//				final double labelIndex = getPixelValue( timePoint, position, source );
//				if ( labelIndex == 0 ) return;
//
//				for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplaySupplier.get() )
//				{
//					try
//					{
//						// TODO: add a "segment exists"?
//						final TableRowImageSegment segment = segmentationDisplay.segmentAdapter.getSegment( labelIndex, timePoint, source.getName() );
//
//						segmentationDisplay.selectionModel.toggle( segment );
//						if ( segmentationDisplay.selectionModel.isSelected( segment ) )
//						{
//							segmentationDisplay.selectionModel.focus( segment );
//						}
//						System.out.println( "\nSelected image segment!\nLabel index: " + labelIndex + "\nTime point: " + timePoint + "\nSource: " + source.getName() );
//					} catch ( Exception e )
//					{
//						// TODO
//						System.out.println( "\nCould NOT select image segment!\nLabel index: " + labelIndex + "\nTime point: " + timePoint + "\nSource: " + source.getName() );
//					}
//				}
//			}
//		}

		// Old code ...
		final Collection< SegmentationSourceDisplay > segmentationDisplays = segmentationDisplaySupplier.get();

		for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplays )
		{
			final Collection< SourceAndConverter< ? > > sourceAndConverters = segmentationDisplay.sourceNameToSourceAndConverter.values();
			for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
			{
				final boolean sourceVisible = bdvHandle.getViewerPanel().state().isSourceVisible( sourceAndConverter );
				if ( ! sourceVisible )
				{
					continue;
				}

				// The source might be a MergedGridSource
				// and thereby represent several sources that
				// need to be inspected for whether they contain selectable
				// image segments.
				final Collection< SourceAndConverter< ? > > containedSourceAndConverters = getContainedSourceAndConverters( sourceAndConverter );

				for ( SourceAndConverter< ? > sac : containedSourceAndConverters )
				{
					if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
					{
						final Source< ? > source = sac.getSpimSource();

						final double labelIndex = getPixelValue( timePoint, position, source );
						if ( labelIndex == 0 ) continue; // background

						final boolean containsSegment = segmentationDisplay.segmentAdapter.containsSegment( labelIndex, timePoint, source.getName() );

						if ( ! containsSegment )
						{
							continue;
						}

						final TableRowImageSegment segment = segmentationDisplay.segmentAdapter.getSegment( labelIndex, timePoint, source.getName() );

						segmentationDisplay.selectionModel.toggle( segment );
						if ( segmentationDisplay.selectionModel.isSelected( segment ) )
						{
							segmentationDisplay.selectionModel.focus( segment );
						}
					}
				}
			}
		}
	}

	private Collection< SourceAndConverter< ? > > getContainedSourceAndConverters( SourceAndConverter< ? > sourceAndConverter )
	{
		final Collection< SourceAndConverter< ? > > containedSourceAndConverters = new HashSet<>();

		if ( sourceAndConverter.getSpimSource() instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource< ? > ) sourceAndConverter.getSpimSource() ).getWrappedSource();
			if ( wrappedSource instanceof MergedGridSource )
			{
				containedSourceAndConverters.addAll( ( ( MergedGridSource ) wrappedSource ).getContainedSourceAndConverters() );
			}
			else
			{
				// Not a MergedGridSource, just return the initial source
				containedSourceAndConverters.add( sourceAndConverter );
			}
		} else
		{
			// Not a MergedGridSource, just return the initial source
			containedSourceAndConverters.add( sourceAndConverter );
		}
		return containedSourceAndConverters;
	}

	private static double getPixelValue( int timePoint, RealPoint position, Source< ? > source )
	{
		final RandomAccess< RealType > randomAccess = ( RandomAccess< RealType > ) source.getSource( timePoint, 0 ).randomAccess();
		final long[] positionInSource = SourceAndConverterHelper.getVoxelPositionInSource( source, position, timePoint, 0 );
		randomAccess.setPosition( positionInSource );
		final double labelIndex = randomAccess.get().getRealDouble();
		return labelIndex;
	}

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}
