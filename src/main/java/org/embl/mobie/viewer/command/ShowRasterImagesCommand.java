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
package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.source.SourceHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Show " + ShowRasterImagesCommand.RAW + " Image(s)" )
public class ShowRasterImagesCommand< T extends NumericType< T > > implements BdvPlaygroundActionCommand
{
	public static final String RAW = "Raw";

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Override
	public void run()
	{
		final List< SourceAndConverter< T > > sourceAndConverters = getSacs();

		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			export( sourceAndConverter );
		}
	}

	private void export( SourceAndConverter< T > sourceAndConverter )
	{
		final Source< T > source = getRootSource( sourceAndConverter.getSpimSource() );
		final int levels = source.getNumMipmapLevels();
		final String[] choices = new String[ levels ];
		for ( int level = 0; level < levels; level++ )
		{
			long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();
			choices[ level ] = Arrays.toString( dimensions );
		}

		final GenericDialog dialog = new GenericDialog( source.getName() );
		dialog.addChoice( "Resolution level", choices, choices[ 0 ] );
		dialog.showDialog();
		if ( dialog.wasCanceled() ) return;
		final int level = dialog.getNextChoiceIndex();

		showImagePlus( sourceAndConverter, level );
	}

	private void showImagePlus( SourceAndConverter< T > sourceAndConverter, int level )
	{
		final Source< T > source = sourceAndConverter.getSpimSource();
		final Source< T > rootSource = getRootSource( source );

		if ( rootSource == null )
		{
			IJ.log( source.getName() + ": Consists of multiple sources and export to ImagePlus is not yet supported.");
			return;
		}

		IJ.log(source.getName() + ": " + RAW + " data = " + rootSource.getName() );

		long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();

		IJ.log( source.getName() + ": Exporting at resolution level = " + level );
		IJ.log( source.getName() + ": [nx, yz, nz] = " + Arrays.toString( dimensions ) );

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, level, sourceTransform );

		final AffineTransform3D rootSourceTransform = new AffineTransform3D();
		rootSource.getSourceTransform( 0, level, rootSourceTransform );

		double[] sourceScale = new double[ 3 ];
		double[] rootSourceScale = new double[ 3 ];

		for ( int d = 0; d < 3; d++ )
		{
			sourceScale[ d ] = Affine3DHelpers.extractScale( sourceTransform, d );
			rootSourceScale[ d ] = Affine3DHelpers.extractScale( rootSourceTransform, d );
		}

		IJ.log( source.getName() + ": Scale = " + Arrays.toString( sourceScale ) );
		IJ.log( source.getName() + ": Transform = " + sourceTransform );
		IJ.log( source.getName() + ": " + RAW + " data scale = " + Arrays.toString( rootSourceScale ) );
		IJ.log( source.getName() + ": " + RAW + " data transform = " + rootSourceTransform );

		final ImagePlus imagePlus = getImagePlus( rootSource, level );
		imagePlus.getCalibration().setUnit( rootSource.getVoxelDimensions().unit() );
		imagePlus.getCalibration().pixelWidth = rootSourceScale[ 0 ];
		imagePlus.getCalibration().pixelHeight = rootSourceScale[ 1 ];
		imagePlus.getCalibration().pixelDepth = rootSourceScale[ 2 ];

		imagePlus.show();

		IJ.log(source.getName() + ": Export done!" );
	}

	private int getExportLevel( Source< T > rootSource, long maxNumPixels )
	{
		final int numMipmapLevels = rootSource.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] dimensions = rootSource.getSource( 0, level ).dimensionsAsLongArray();

			final boolean javaIndexingOK = dimensions[ 0 ] * dimensions[ 1 ] < Integer.MAX_VALUE - 1;

			final boolean sizeOK = dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] < maxNumPixels;

			if( javaIndexingOK && sizeOK )
				return level;
		}
		return -1;
	}

	private Source< T > getRootSource( Source< T > source )
	{
		final Set< Source< ? > > rootSources = new HashSet<>();
		SourceHelper.fetchRootSources( source, rootSources );
		if ( rootSources.size() > 1 )
			throw new UnsupportedOperationException("Cannot show raw image(s) of a source that is composed of multiple sources.");
		final Source< T > rootSource = ( Source< T > ) rootSources.iterator().next();
		return rootSource;
	}

	private ImagePlus getImagePlus( Source< T > source, int level )
	{
		final RandomAccessibleInterval< T > raiXYZT = getRAIXYZT( source, level );
		final IntervalView< T > raiXYZTC = Views.addDimension( raiXYZT, 0, 0 );
		final IntervalView< T > raiXYCZT = Views.permute( Views.permute( raiXYZTC, 4, 3 ), 3, 2);
		final ImagePlus imagePlus = ImageJFunctions.wrap( raiXYCZT, source.getName() );
		return imagePlus;
	}

	private List< SourceAndConverter< T > > getSacs()
	{
		final List< SourceAndConverter< T > > sourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverterArray )
		{
			sourceAndConverters.add( ( SourceAndConverter< T > ) sourceAndConverter );
		}
		return sourceAndConverters;
	}

	private RandomAccessibleInterval< T > getRAIXYZT( Source< T > source, int level )
	{
		int numTimepoints = 0;
		while ( source.isPresent( numTimepoints ) )
		{
			numTimepoints++;
			if ( numTimepoints >= Integer.MAX_VALUE )
			{
				throw new RuntimeException("The source " + source.getName() + " appears to contain more than " + Integer.MAX_VALUE + " time points; maybe something is wrong?");
			}
		}

		return getRAIXYZT( source, level, numTimepoints );
	}

	private RandomAccessibleInterval< T > getRAIXYZT( Source< T > source, int level, int numTimepoints )
	{
		final ArrayList< RandomAccessibleInterval< T > > rais = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; t++ )
		{
			rais.add( source.getSource( t, level ) );
		}
		return Views.stack( rais );
	}

	private HashMap< SourceAndConverter< T >, AffineTransform3D > fetchTransforms( List< SourceAndConverter< T > > movingSacs )
	{
		final HashMap< SourceAndConverter< T >, AffineTransform3D > sacToTransform = new HashMap<>();
		for ( SourceAndConverter movingSac : movingSacs )
		{
			final AffineTransform3D fixedTransform = new AffineTransform3D();
			( ( TransformedSource ) movingSac.getSpimSource()).getFixedTransform( fixedTransform );
			sacToTransform.put( movingSac, fixedTransform );
		}
		return sacToTransform;
	}
}
