package de.embl.cba.mobie2.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SourcesAtMousePositionSupplier implements Supplier< Collection< SourceAndConverter< ? > > >
{
	BdvHandle bdvHandle;
	boolean is2D;
	private RealPoint position;
	private int timePoint;

	public SourcesAtMousePositionSupplier( BdvHandle bdvHandle, boolean is2D )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
	}

	@Override
	public Collection< SourceAndConverter< ? > > get()
	{
		// Gets mouse location in space (global 3D coordinates) and time
		position = new RealPoint( 3 );
		bdvHandle.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( position );
		int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		final List< SourceAndConverter< ? > > sourceAndConverters = SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf( bdvHandle )
				.stream()
				.filter( sac -> SourceAndConverterHelper.isPositionWithinSourceInterval( sac, position, timePoint, is2D ) )
				.filter( sac -> SourceAndConverterServices.getSourceAndConverterDisplayService().isVisible( sac, bdvHandle ) )
				.collect( Collectors.toList() );

		return sourceAndConverters;
	}

	public RealPoint getPosition()
	{
		return position;
	}

	public int getTimePoint()
	{
		return timePoint;
	}
}
