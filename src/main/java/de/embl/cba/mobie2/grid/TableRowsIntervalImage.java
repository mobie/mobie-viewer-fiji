package de.embl.cba.mobie2.grid;

import bdv.util.BdvOverlay;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.plateviewer.image.channel.AbstractBdvViewable;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.view.TableRowsTableView;
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

public class TableRowsIntervalImage< T extends AnnotatedIntervalTableRow > extends AbstractBdvViewable
{
	private final List< T > tableRows;
	private final SelectionColoringModel< T > coloringModel;
	private Interval plateInterval;
	private RandomAccessibleInterval< IntType > rai;
	private double[] contrastLimits;
	private final TableRowsTableView< T > tableView;
	private HashMap< String, T > nameToTableRow;
	private HashMap< String, Integer > nameToTableRowIndex;
	private ARGBConvertedRealSource argbSource;
	private String name;

	public TableRowsIntervalImage(
			List< T > tableRows,
			SelectionColoringModel< T > coloringModel,
			TableRowsTableView< T > tableView,
			Interval plateInterval,
			String name )
	{
		this.tableRows = tableRows;
		this.coloringModel = coloringModel;

		this.plateInterval = plateInterval;
		this.tableView = tableView;
		this.name = name;

		createSiteNameToTableRowMap( tableRows );

		contrastLimits = new double[ 2 ];

		createImage();
	}

	public TableRowsTableView< T > getTableView()
	{
		return tableView;
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

		argbSource = new ARGBConvertedRealSource( tableRowIndexSource , argbConverter );

		contrastLimits[ 0 ] = 0;
		contrastLimits[ 1 ] = 255;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public ARGBType getColor()
	{
		return ColorUtils.getARGBType( Color.GRAY );
	}

	@Override
	public double[] getContrastLimits()
	{
		return contrastLimits;
	}

	@Override
	public RandomAccessibleInterval< ? > getRAI()
	{
		return rai;
	}

	@Override
	public Source< ? > getSource()
	{
		return argbSource;
	}

	@Override
	public BdvOverlay getOverlay()
	{
		return null;
	}

	@Override
	public boolean isInitiallyVisible()
	{
		return true;
	}

	@Override
	public Metadata.Type getType()
	{
		return Metadata.Type.Image;
	}
}
