package de.embl.cba.mobie2.grid;

import bdv.util.BdvOverlay;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringModel;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
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
	private Interval plateInterval;
	private RandomAccessibleInterval< IntType > rai;
	private double[] contrastLimits;
	private HashMap< String, T > nameToTableRow;
	private HashMap< String, Integer > nameToTableRowIndex;
	private ARGBConvertedRealSource argbSource;
	private String name;
	private SourceAndConverter< IntType > sourceAndConverter;

	public TableRowsIntervalImage(
			List< T > tableRows,
			ColoringModel< T > coloringModel,
			String name )
	{
		this.tableRows = tableRows;
		this.coloringModel = coloringModel;
		this.name = name;

		createSiteNameToTableRowMap( tableRows );

		contrastLimits = new double[ 2 ];

		createImage();
	}

	public void createSiteNameToTableRowMap( List< T > tableRows )
	{
		nameToTableRow = new HashMap<>();
		nameToTableRowIndex = new HashMap();

		int rowIndex = 0;
		for ( T tableRow : tableRows )
		{
			nameToTableRow.put( tableRow.getName(), tableRow );
			nameToTableRowIndex.put( tableRow.getName(), rowIndex++ );
		}
	}

	private void createImage( )
	{
		BiConsumer< Localizable, IntType > biConsumer = ( l, t ) ->
		{
			t.setInteger( ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX );

			for ( T annotatedIntervalTableRow : tableRows )
			{
				final Interval interval = annotatedIntervalTableRow.getInterval();
				if ( interval == null ) return;

				if ( Intervals.contains( interval, l ) )
				{
					t.setInteger( annotatedIntervalTableRow.rowIndex() );
				}
			}
		};

		final FunctionRandomAccessible< IntType > randomAccessible =
				new FunctionRandomAccessible( 2, biConsumer, IntType::new );

		rai = Views.interval( randomAccessible, plateInterval );

		rai = Views.addDimension( rai, 0, 0 );

		final RandomAccessibleIntervalSource< IntType > tableRowIndexSource
				= new RandomAccessibleIntervalSource<>( rai, Util.getTypeFromInterval( rai ), "table row index" );

		final ListItemsARGBConverter< T > argbConverter =
				new ListItemsARGBConverter<>( tableRows, coloringModel );

		sourceAndConverter = new SourceAndConverter( tableRowIndexSource, argbConverter );

		contrastLimits[ 0 ] = 0;
		contrastLimits[ 1 ] = 255;
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
