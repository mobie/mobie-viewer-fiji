package org.embl.mobie.viewer.annotate;

import bdv.util.BdvOverlay;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.color.ListItemsARGBConverter;
import de.embl.cba.tables.color.ColorUtils;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.color.SelectionColoringModel;
import org.embl.mobie.viewer.source.LabelSource;

import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

public class TableRowsIntervalImage< T extends AnnotatedMaskTableRow >
{
	private final List< T > tableRows;
	private final SelectionColoringModel< T > coloringModel;
	private double[] contrastLimits;
	private String name;
	private SourceAndConverter< IntType > sourceAndConverter;
	private RealMaskRealInterval unionMask;
	private RealInterval unionInterval;
	private int size;

	public TableRowsIntervalImage(
			List< T > tableRows,
			SelectionColoringModel< T > coloringModel,
			String name )
	{
		final long currentTimeMillis = System.currentTimeMillis();

		this.tableRows = tableRows;
		this.coloringModel = coloringModel;
		this.name = name;
		this.size = tableRows.size();

		setUnionMask( tableRows );
		createImage();

		final long duration = System.currentTimeMillis() - currentTimeMillis;
		if ( duration > MoBIE.minLogTimeMillis )
			IJ.log("Created annotation image "+name+" in " + duration + " ms." );
	}

	public void setUnionMask( List< T > tableRows )
	{
		size = tableRows.size();

		for ( T tableRow : tableRows )
		{
			final RealMaskRealInterval mask = tableRow.mask();

			if ( unionInterval == null )
			{
				//unionMask = mask;
				unionInterval = mask;
			}
			else
			{

				if ( Intervals.equals(  mask, unionInterval ) )
				{
					continue;
				}
				else
				{
					// TODO: Below hangs
					//unionMask = unionMask.or( mask );
					unionInterval = Intervals.union( unionInterval, mask );
				}
			}
		}

		// TODO: this is a work around
		unionMask = GeomMasks.closedBox( unionInterval.minAsDoubleArray(), unionInterval.maxAsDoubleArray() );
	}

	private void createImage( )
	{
		BiConsumer< RealLocalizable, IntType > biConsumer = ( location, value ) ->
		{
			value.setInteger( ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX );

			for ( int i = 0; i < size; i++ )
			{
				final RealMaskRealInterval mask = tableRows.get( i ).mask();

				if ( mask.test( location ) )
				{
					value.setInteger( i );
					return;
				}
			}
		};

		final FunctionRealRandomAccessible< IntType > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, IntType::new );

		//final IntType intType = randomAccessible.realRandomAccess().get();
		final double realMin = unionMask.realMin( 0 );
		final Interval interval = Intervals.smallestContainingInterval( unionMask );
		final RealRandomAccessibleIntervalSource< IntType > source = new RealRandomAccessibleIntervalSource( randomAccessible, interval, new IntType(), name );

		final ListItemsARGBConverter< T > argbConverter = new ListItemsARGBConverter<>( tableRows, coloringModel );

		sourceAndConverter = new SourceAndConverter( new LabelSource<>( source, ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX, unionMask ), argbConverter );

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
