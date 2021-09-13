package org.embl.mobie.viewer.color;

import bdv.viewer.TimePointListener;
import de.embl.cba.tables.color.ColoringModel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import java.util.HashMap;
import java.util.List;

public class ListItemsARGBConverter< T > implements Converter< RealType, ARGBType >, OpacityAdjuster, TimePointListener
{
	public static final int OUT_OF_BOUNDS_ROW_INDEX = -1;
	private final ColoringModel< T > coloringModel;
	private final List< T > list;
	private int backgroundARGBIndex; // default, background color
	private final HashMap< Integer, Integer > indexToColor;
	private double opacity = 0.5;

	public ListItemsARGBConverter(
			List< T > list,
			ColoringModel< T > coloringModel )
	{
		this.list = list;
		this.coloringModel = coloringModel;
		backgroundARGBIndex = ARGBType.rgba( 0,0,0,255 );
		indexToColor = new HashMap<>();
	}

	@Override
	public void convert( RealType rowIndex, ARGBType color )
	{
		if ( rowIndex instanceof Volatile )
		{
			if ( ! ( ( Volatile ) rowIndex ).isValid() )
			{
				color.set( backgroundARGBIndex );
				return;
			}
		}

		final int index = ( int ) rowIndex.getRealDouble();

		if ( index == OUT_OF_BOUNDS_ROW_INDEX )
		{
			color.set( backgroundARGBIndex );
			return;
		}

		final T item = list.get( index );

		if ( item == null )
		{
			throw new UnsupportedOperationException( "Item not found " + item );
		}
		else
		{
			coloringModel.convert( item, color );
			final int alpha = ARGBType.alpha( color.get() );
			color.mul( alpha / 255.0 );
		}

		color.mul( opacity );
	}

	@Override
	public void setOpacity( double opacity )
	{
		this.opacity = opacity;
	}

	@Override
	public double getOpacity()
	{
		return opacity;
	}

	@Override
	public void timePointChanged( int timePointIndex )
	{
		// TODO: why would we need this?
	}
}
