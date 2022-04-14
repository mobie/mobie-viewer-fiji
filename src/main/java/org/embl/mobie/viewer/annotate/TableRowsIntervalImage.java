package org.embl.mobie.viewer.annotate;

import bdv.util.BdvOverlay;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.color.ListItemsARGBConverter;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringModel;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.source.LabelSource;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class TableRowsIntervalImage< T extends AnnotatedIntervalTableRow >
{
	private final List< T > tableRows;
	private final ColoringModel< T > coloringModel;
	private double[] contrastLimits;
	private HashMap< String, T > nameToTableRow;
	private HashMap< String, Integer > nameToTableRowIndex;
	private String name;
	private SourceAndConverter< IntType > sourceAndConverter;
	private RealInterval union;
	private int size;

	public TableRowsIntervalImage(
			List< T > tableRows,
			ColoringModel< T > coloringModel,
			String name )
	{
		this.tableRows = tableRows;
		this.coloringModel = coloringModel;
		this.name = name;

		init( tableRows );
		createImage();
	}

	public void init( List< T > tableRows )
	{
		size = tableRows.size();
		nameToTableRow = new HashMap<>();
		nameToTableRowIndex = new HashMap();

		int rowIndex = 0;
		for ( T tableRow : tableRows )
		{
			nameToTableRow.put( tableRow.getName(), tableRow );
			nameToTableRowIndex.put( tableRow.getName(), rowIndex++ );
			if ( union == null )
				union = tableRow.getInterval();
			else
				union = Intervals.union( tableRow.getInterval(), union );
		}
	}

	private void createImage( )
	{
		BiConsumer< RealLocalizable, IntType > biConsumer = ( l, t ) ->
		{
			t.setInteger( ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX );

			for ( int i = 0; i < size; i++ )
			{
				final RealInterval interval = tableRows.get( i ).getInterval();

				if ( Intervals.contains( interval, l ) )
				{
					t.setInteger( i );
					return;
				}
			}
		};

		final FunctionRealRandomAccessible< IntType > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, IntType::new );

		final IntType intType = randomAccessible.realRandomAccess().get();
		final RealRandomAccessibleIntervalSource< IntType > source = new RealRandomAccessibleIntervalSource( randomAccessible, Intervals.smallestContainingInterval( union ), intType, name );

		final ListItemsARGBConverter< T > argbConverter = new ListItemsARGBConverter<>( tableRows, coloringModel );

		sourceAndConverter = new SourceAndConverter( new LabelSource<>( source, ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX ), argbConverter );

		contrastLimits = new double[]{ 0, 255 };
	}

	public String getName()
	{
		return name;
	}

	public ARGBType getColor()
	{
		return ColorUtils.getARGBType( Color.GRAY );
	}

	public double[] getContrastLimits()
	{
		return contrastLimits;
	}

	public SourceAndConverter< IntType > getSourceAndConverter()
	{
		return sourceAndConverter;
	}

	public BdvOverlay getOverlay()
	{
		return null;
	}

	public boolean isInitiallyVisible()
	{
		return true;
	}
}
