/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.viewer.annotate;

import bdv.tools.transformation.TransformedSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class TableRowsIntervalImage< T extends RegionTableRow >
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

		final ArrayList< Integer > timePoints = configureTimePoints();


		final FunctionRealRandomAccessible< IntType > randomAccessible = new FunctionRealRandomAccessible( 3, biConsumer, IntType::new );

		final Interval interval = Intervals.smallestContainingInterval( unionMask );

		final RealRandomAccessibleIntervalSource< IntType > source = new RealRandomAccessibleIntervalSource( randomAccessible, interval, new IntType(), name );

		final ListItemsARGBConverter< T > argbConverter = new ListItemsARGBConverter<>( tableRows, coloringModel );

		final LabelSource< IntType > labelSource = new LabelSource<>( source, ListItemsARGBConverter.OUT_OF_BOUNDS_ROW_INDEX, unionMask, timePoints );

		final TransformedSource< IntType > transformedSource = new TransformedSource<>( labelSource );

		sourceAndConverter = new SourceAndConverter( transformedSource, argbConverter );

		contrastLimits = new double[]{ 0, 255 };
	}

	private ArrayList< Integer > configureTimePoints()
	{
		// TODO: make this configurable in constructor or base it on the tableRows which have: tableRows.get( 0 ).timePoint()
		final ArrayList< Integer > timepoints = new ArrayList<>();
		timepoints.add( 0 );
		return timepoints;
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
