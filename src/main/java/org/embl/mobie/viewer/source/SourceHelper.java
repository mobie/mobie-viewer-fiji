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

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.color.LazyLabelConverter;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.lang.reflect.Method;

public abstract class SourceHelper
{

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
}
