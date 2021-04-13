package de.embl.cba.mobie2.segment;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.bdv.BdvMousePositionProvider;
import de.embl.cba.mobie2.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class SegmentBdvSelector implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	Supplier< Collection< SourceAndConverter< ? > > > labelSourceSupplier;
	private Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter;

	public SegmentBdvSelector( BdvHandle bdvHandle, boolean is2D, Supplier< Collection< SourceAndConverter< ? > > > labelSourceSupplier, Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.labelSourceSupplier = labelSourceSupplier;
		this.selectionModelToAdapter = selectionModelToAdapter;
	}


	private synchronized void toggleSelectionAtMousePosition()
	{
		final BdvMousePositionProvider positionProvider = new BdvMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint position = positionProvider.getPosition();

		final Collection< SourceAndConverter< ? > > labelSources = labelSourceSupplier.get();

		for ( SourceAndConverter< ? > sourceAndConverter : labelSources )
		{
			if ( SourceAndConverterHelper.isPositionWithinSourceInterval( sourceAndConverter, position, timePoint, is2D ) )
			{
				Source< ? > source = sourceAndConverter.getSpimSource();
				if ( source instanceof LabelSource )
					source = ( ( LabelSource ) source ).getWrappedSource();

				final double labelIndex = getPixelValue( timePoint, position, source );

				if ( labelIndex == 0 ) return;

				// The image viewer can show several sources that
				// are associated with selection models. In fact already
				// one selection model can be associated to several sources
				// that are shown in parallel, and which share the same
				// feature table. We thus check in all models whether the
				// selected segment is a part of that model.
				for ( SelectionModel< TableRowImageSegment > selectionModel : selectionModelToAdapter.keySet() )
				{
					final TableRowImageSegment segment = selectionModelToAdapter.get( selectionModel ).getSegment( labelIndex, timePoint, source.getName() );

					if ( segment != null)
					{
						selectionModel.toggle( segment );
						if ( selectionModel.isSelected( segment ) )
						{
							selectionModel.focus( segment );
						}
					}
				}
			}
		}
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
