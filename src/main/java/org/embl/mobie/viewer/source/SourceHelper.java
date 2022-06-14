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
package org.embl.mobie.viewer.source;

import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.color.LazyLabelConverter;
import org.embl.mobie.viewer.transform.RealIntervalProvider;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SourceHelper
{
    // TODO: one could get rid of this if Sources could
    //   return their mask directly with some other method!
	@Deprecated
    public static double[][] getMinMax( Source< ? > source )
    {
        final double[][] minMax = new double[2][];
        final Source< ? > rootSource = fetchRootSource( source );
        if ( rootSource instanceof LazySpimSource )
        {
            minMax[ 0 ] = ( ( LazySpimSource ) rootSource ).getMin();
            minMax[ 1 ] = ( ( LazySpimSource ) rootSource ).getMax();
        }
        else
        {
            final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );
            minMax[ 0 ] = rai.minAsDoubleArray();
            minMax[ 1 ] = rai.maxAsDoubleArray();
        }

        return minMax;
    }

    public static <R extends NumericType<R> & RealType<R>> SourceAndConverter<R> replaceConverter(SourceAndConverter<?> source, Converter<RealType<?>, ARGBType> converter) {
        LabelSource<?> labelVolatileSource = new LabelSource(source.asVolatile().getSpimSource());
        SourceAndConverter<?> volatileSourceAndConverter = new SourceAndConverter(labelVolatileSource, converter);
        LabelSource<?> labelSource = new LabelSource(source.getSpimSource());
        return new SourceAndConverter(labelSource, converter, volatileSourceAndConverter);
    }

    public static <R extends NumericType<R> & RealType<R>> BdvStackSource<?> showAsLabelMask(BdvStackSource<?> bdvStackSource) {
        LazyLabelConverter converter = new LazyLabelConverter();
        SourceAndConverter<R> sac = replaceConverter(bdvStackSource.getSources().get(0), converter);
        BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
        bdvStackSource.removeFromBdv();

        // access by reflection, which feels quite OK as this will be public in future versions of bdv anyway:
        // https://github.com/bigdataviewer/bigdataviewer-vistools/commit/8cad3edac6c563dc2d22abf71345655afa7f49cc
        try {
            Method method = BdvFunctions.class.getDeclaredMethod("addSpimDataSource", BdvHandle.class, SourceAndConverter.class, int.class);
            method.setAccessible(true);
            BdvStackSource<?> newBdvStackSource = (BdvStackSource<?>) method.invoke("addSpimDataSource", bdvHandle, sac, 1);

            Behaviours behaviours = new Behaviours(new InputTriggerConfig());
            behaviours.install(bdvHandle.getTriggerbindings(), "label source " + sac.getSpimSource().getName());
            behaviours.behaviour((ClickBehaviour) (x, y) ->
                            new Thread(() -> {
                                converter.getColoringModel().incRandomSeed();
                                bdvHandle.getViewerPanel().requestRepaint();
                            }).start(),
                    "shuffle random colors " + sac.getSpimSource().getName(),
                    "ctrl L");

            return newBdvStackSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void viewAsHyperstack(BdvStackSource<?> bdvStackSource, int level) {
        RandomAccessibleInterval<?> rai = bdvStackSource.getSources().get(0).getSpimSource().getSource(0, level);
        IntervalView<?> permute = Views.permute(Views.addDimension(rai, 0, 0), 2, 3);
        ImageJFunctions.wrap(Cast.unchecked(permute), "em").show();
    }

    // TODO: implement this recursively
    public static LabelSource< ? > getLabelSource( SourceAndConverter sac )
    {
        if ( sac.getSpimSource() instanceof LabelSource )
        {
            return ( LabelSource< ? > ) sac.getSpimSource();
        }
        else if ( sac.getSpimSource() instanceof TransformedSource )
        {
            if ( ( ( TransformedSource<?> ) sac.getSpimSource() ).getWrappedSource() instanceof LabelSource )
            {
                return ( LabelSource< ? > ) ( ( TransformedSource<?> ) sac.getSpimSource() ).getWrappedSource();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }


    public static LabelSource< ? > isAnnotationSource( SourceAndConverter sac )
    {
        if ( sac.getSpimSource() instanceof LabelSource )
        {
            return ( LabelSource< ? > ) sac.getSpimSource();
        }
        else if ( sac.getSpimSource() instanceof TransformedSource )
        {
            if ( ( ( TransformedSource<?> ) sac.getSpimSource() ).getWrappedSource() instanceof LabelSource )
            {
                return ( LabelSource< ? > ) ( ( TransformedSource<?> ) sac.getSpimSource() ).getWrappedSource();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

	public static int getNumTimepoints( SourceAndConverter< ? > source )
	{
		int numSourceTimepoints = 0;
        final int maxNumTimePoints = 10000; // TODO
        for ( int t = 0; t < maxNumTimePoints; t++ )
		{
			if ( source.getSpimSource().isPresent( t ) )
            {
                numSourceTimepoints++;
            }
            else
            {
                return numSourceTimepoints;
            }
		}

        if ( numSourceTimepoints == maxNumTimePoints )
            System.err.println( source.getSpimSource().getName() + " has more than " + maxNumTimePoints + " time-points. Is this an error?!" );

		return numSourceTimepoints;
	}

	public static RealIntervalProvider getRealIntervalProvider( Source< ? > source )
	{
		if ( source instanceof RealIntervalProvider )
		{
			return ( RealIntervalProvider ) source;
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();
			return getRealIntervalProvider( wrappedSource );
		}
		else if (  source instanceof SourceWrapper )
		{
			final Source< ? > wrappedSource = (( SourceWrapper ) source).getWrappedSource();
			return getRealIntervalProvider( wrappedSource );
		}
		else
		{
			return null;
		}
	}

	/**
	 * Recursively fetch all root sources
	 * @param source
	 * @param rootSources
	 */
	public static void fetchRootSources( Source< ? > source, Set< Source< ? > > rootSources )
	{
		if ( source instanceof SpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof LazySpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof LabelSource )
		{
			final Source< ? > wrappedSource = (( LabelSource ) source).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof MergedGridSource )
		{
			final MergedGridSource< ? > mergedGridSource = ( MergedGridSource ) source;
			final List< ? extends SourceAndConverter< ? > > gridSources = mergedGridSource.getGridSources();
			for ( SourceAndConverter< ? > gridSource : gridSources )
			{
				fetchRootSources( gridSource.getSpimSource(), rootSources );
			}
		}
		else if (  source instanceof ResampledSource )
		{
			final ResampledSource resampledSource = ( ResampledSource ) source;
			final Source< ? > wrappedSource = resampledSource.getOriginalSource();
			fetchRootSources( wrappedSource, rootSources );
		}
		else
		{
			throw new IllegalArgumentException("For sources of type " + source.getClass().getName() + " the root source currently cannot be determined.");
		}
	}

	public static Source< ? > fetchRootSource( Source< ? > source )
	{
		final Set< Source< ? > > rootSources = new HashSet<>();
		fetchRootSources( source, rootSources );
		return rootSources.iterator().next();
	}

	public static RealMaskRealInterval getUnionMask( List< ? extends Source< ? > > sources, int t )
	{
		RealMaskRealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealMaskRealInterval mask = SourceHelper.getMask( source, t );

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.equals( mask, union ) )
				{
					continue;
				}
				else
				{
					union = union.or( mask );
				}
			}
		}

		return union;
	}

	public static RealMaskRealInterval getMask( Source< ? > source, int t )
	{
		// fetch the extent of the source in voxel space
		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];

		final RealIntervalProvider realIntervalProvider = getRealIntervalProvider( source );
		if ( realIntervalProvider != null )
		{
			final FinalRealInterval realInterval = realIntervalProvider.getRealInterval( t );
			realInterval.realMin( min );
			realInterval.realMax( max );
		}
		else
		{
			final RandomAccessibleInterval< ? > rai = source.getSource( t, 0 );
			rai.realMin( min );
			rai.realMax( max );
		}
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( 0, 0, affineTransform3D );

		// expand with voxel dimensions
		final double[] voxelSizes = new double[ 3 ];
		source.getVoxelDimensions().dimensions( voxelSizes );
		for ( int d = 0; d < 3; d++ )
		{
			min[ d ] -= voxelSizes[ d ];
			max[ d ] += voxelSizes[ d ];
		}

		// create mask
		// as compared with estimateBounds this has the
		// advantage that it can represent a rotated box
		final RealMaskRealInterval mask = GeomMasks.closedBox( min, max ).transform( affineTransform3D.inverse() );
		return mask;
	}

	public static SourceAndConverter< ? > getSourceAndConverter( List< SourceAndConverter< ? > > sourceAndConverters, String name )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getSpimSource().getName().equals( name ) )
				return sourceAndConverter;
		}

		return null;
	}
}
