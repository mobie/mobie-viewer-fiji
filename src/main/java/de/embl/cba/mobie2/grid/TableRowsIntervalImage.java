package de.embl.cba.mobie2.grid;

import bdv.util.BdvOverlay;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.RandomAccessibleIntervalSource4D;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringModel;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class TableRowsIntervalImage< T extends AnnotatedIntervalTableRow >
{
	private final List< T > tableRows;
	private final ColoringModel< T > coloringModel;
	private RandomAccessibleInterval< IntType > rai;
	private double[] contrastLimits;
	private HashMap< String, T > nameToTableRow;
	private HashMap< String, Integer > nameToTableRowIndex;
	private ARGBConvertedRealSource argbSource;
	private String name;
	private SourceAndConverter< IntType > sourceAndConverter;
	private RealInterval union;

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
		BiConsumer< Localizable, IntType > biConsumer = ( l, t ) ->
		{
			t.setInteger( ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX );

			for ( T annotatedIntervalTableRow : tableRows )
			{
				final RealInterval interval = annotatedIntervalTableRow.getInterval();

				if ( Intervals.contains( interval, l ) )
				{
					t.setInteger( annotatedIntervalTableRow.rowIndex() );
				}
			}
		};

		final FunctionRandomAccessible< IntType > randomAccessible = new FunctionRandomAccessible( 3, biConsumer, IntType::new );

		rai = Views.interval( randomAccessible, Intervals.smallestContainingInterval( union ) );

		// add time dimension
		rai = Views.addDimension( rai, 0, 0 );

		final RandomAccessibleIntervalSource4D< IntType > tableRowIndexSource
				= new RandomAccessibleIntervalSource4D<>( rai, Util.getTypeFromInterval( rai ), "table row index" );

		final ListItemsARGBConverter< T > argbConverter =
				new ListItemsARGBConverter<>( tableRows, coloringModel );

		sourceAndConverter = new SourceAndConverter( tableRowIndexSource, argbConverter );

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

	public RandomAccessibleInterval< ? > getRAI()
	{
		return rai;
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
