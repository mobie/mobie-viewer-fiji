package de.embl.cba.mobie2.segment;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.RealPoint;

import java.util.Collection;
import java.util.Map;

public class SegmentBdvSelector implements Runnable
{
	private BdvHandle bdvHandle;
	private boolean is2D;
	Collection< SourceAndConverter< ? > > labelSources;
	private Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter;

	public SegmentBdvSelector( BdvHandle bdvHandle, boolean is2D, Collection< SourceAndConverter< ? > > labelSources, Map< SelectionModel< TableRowImageSegment >, SegmentAdapter< TableRowImageSegment > > selectionModelToAdapter )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
		this.labelSources = labelSources;
		this.selectionModelToAdapter = selectionModelToAdapter;
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final SourcesAtMousePositionSupplier sourcesAtMousePositionSupplier = new SourcesAtMousePositionSupplier( bdvHandle, is2D );

		final Collection< SourceAndConverter< ? > > sourceAndConverters = sourcesAtMousePositionSupplier.get();
		final int timePoint = sourcesAtMousePositionSupplier.getTimePoint();
		final RealPoint position = sourcesAtMousePositionSupplier.getPosition();

		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			if ( labelSources.contains( sourceAndConverter ) )
			{
				Source< ? > source = sourceAndConverter.getSpimSource();
				if ( source instanceof LabelSource )
					source = ( ( LabelSource ) source ).getWrappedSource();

				// TODO: the getPixelValue method could probably be simplified now
				final Double labelIndex = BdvUtils.getPixelValue( source, position, timePoint );

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

	@Override
	public void run()
	{
		toggleSelectionAtMousePosition();
	}
}
