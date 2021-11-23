package org.embl.mobie.viewer.segment;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.io.n5.source.LabelSource;
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

		final Collection< SegmentationSourceDisplay > segmentationDisplays = segmentationDisplaySupplier.get();

		for ( SegmentationSourceDisplay segmentationDisplay : segmentationDisplays )
		{
			final Collection< SourceAndConverter< ? > > displayedSourceAndConverters = segmentationDisplay.sourceNameToSourceAndConverter.values();
			for ( SourceAndConverter< ? > displayedSourceAndConverter : displayedSourceAndConverters )
			{
				final boolean sourceVisible = bdvHandle.getViewerPanel().state().isSourceVisible( displayedSourceAndConverter );
				if ( ! sourceVisible )
				{
					continue;
				}

				// The source might be a MergedGridSource and thereby represent several sources that
				// need to be inspected for whether they contain selectable image segments.
				// TODO: Probably more efficient and consistent to simplify this,
				//   using the image name encoding instead.
				final Collection< SourceAndConverter< ? > > sourceAndConverters = getContainedSourceAndConverters( displayedSourceAndConverter );

				for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
				{
					if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
					{
						final Source< ? > source = sourceAndConverter.getSpimSource();

						final double labelIndex = getPixelValue( timePoint, position, source );
						if ( labelIndex == 0 ) continue; // background

						final String sourceName = source.getName();

						final boolean containsSegment = segmentationDisplay.segmentAdapter.containsSegment( labelIndex, timePoint, sourceName );

						if ( ! containsSegment )
						{
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
		final double labelIndex = randomAccess.get().getRealDouble();
		return labelIndex;
	}

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}
