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
package org.embl.mobie.command.context;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.bdv.GlobalMousePositionProvider;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.image.StitchedImage;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.source.SourceToImagePlusConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Show " + ShowRasterImagesCommand.RAW + " Image(s)" )
public class ShowRasterImagesCommand< T extends NumericType< T > > implements BdvPlaygroundActionCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String RAW = "Raw";

	@Parameter( label = "BdvHandle" )
	public BdvHandle bdvHandle;

	@Parameter( label = "Source(s)" )
	public SourceAndConverter[] sourceAndConverterArray;

	@Override
	public void run()
	{
		final List< SourceAndConverter< T > > sourceAndConverters = getSacs();

		for ( SourceAndConverter< T > sourceAndConverter : sourceAndConverters )
		{
			Image< ? > image = (Image< ? >) SourceAndConverterServices.getSourceAndConverterService().getMetadata( sourceAndConverter, Image.class.getName() );

			if ( image instanceof RegionAnnotationImage  )
			{
				continue;
			}

			if ( image instanceof StitchedImage )
			{
				final RealPoint position = new GlobalMousePositionProvider( bdvHandle ).getPositionAsRealPoint();

				// traverse through the potentially several
				// layers of stitching
				while ( image instanceof StitchedImage )
				{
					final Optional< ? extends Image< ? > > optionalTileImage = (( StitchedImage< ?, ? > ) image).getTileImageAtGlobalPosition( position );

					if ( optionalTileImage.isPresent() )
					{
						image = optionalTileImage.get();
					}
					else
					{
						image = null;
						break;
					}
				}

				if ( image != null )
					export( ( Source< T > ) image.getSourcePair().getSource() );
			}
			else
			{
				final Source< T > source = getRootSource( sourceAndConverter.getSpimSource() );
				export( source );
			}
		}
	}

	private void export( Source< T > source )
	{
		final int numMipmapLevels = source.getNumMipmapLevels();

		if ( numMipmapLevels > 1 )
		{
			final String[] choices = new String[ numMipmapLevels ];
			for ( int level = 0; level < numMipmapLevels; level++ )
			{
				long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();
				choices[ level ] = Arrays.toString( dimensions );
			}

			final GenericDialog dialog = new GenericDialog( source.getName() );
			dialog.addChoice( "Resolution level", choices, choices[ 0 ] );
			dialog.showDialog();

			if ( dialog.wasCanceled() ) return;

			final int level = dialog.getNextChoiceIndex();
			showImagePlus( source, level );
		}
		else
		{
			showImagePlus( source, 0 );
		}
	}

	private void showImagePlus( Source< T > source, int level )
	{
		IJ.log(source.getName() + ": " + RAW + " data = " + source.getName() );

		long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();

		IJ.log( source.getName() + ": Exporting at resolution level = " + level );
		IJ.log( source.getName() + ": [nx, yz, nz] = " + Arrays.toString( dimensions ) );

		final ImagePlus imagePlus = convertSourceToImagePlus( source, level );

		imagePlus.show();

		IJ.log(source.getName() + ": Export done!" );
	}

	public ImagePlus convertSourceToImagePlus( Source< T > source, int level )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, level, sourceTransform );

		double[] sourceScale = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			sourceScale[ d ] = Affine3DHelpers.extractScale( sourceTransform, d );
		}

		IJ.log( source.getName() + ": Scale = " + Arrays.toString( sourceScale ) );
		IJ.log( source.getName() + ": Transform = " + sourceTransform );

		final ImagePlus imagePlus = new SourceToImagePlusConverter<>( source ).getImagePlus( level );
		imagePlus.getCalibration().setUnit( source.getVoxelDimensions().unit() );
		imagePlus.getCalibration().pixelWidth = sourceScale[ 0 ];
		imagePlus.getCalibration().pixelHeight = sourceScale[ 1 ];
		imagePlus.getCalibration().pixelDepth = sourceScale[ 2 ];
		return imagePlus;
	}

	private Source< T > getRootSource( Source< T > source )
	{
		Set< Source< ? > > rootSources = new HashSet<>();
		SourceHelper.fetchRootSources( source, rootSources );

		if ( rootSources.size() == 1 )
		{
			return ( Source< T > ) rootSources.iterator().next();
		}

		final List< Source< ? > > atCurrentMousePosition = SourceHelper.filterAtCurrentMousePosition( rootSources, bdvHandle );
		return ( Source< T > ) atCurrentMousePosition.iterator().next();
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
}
