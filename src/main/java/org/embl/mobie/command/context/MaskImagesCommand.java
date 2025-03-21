/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.widget.SelectableImages;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.MaskedImage;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.view.ViewManager;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Mask Images")
public class MaskImagesCommand extends BoxSelectionCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter ( label = "Mask Images" )
    public SelectableImages selectedImages;

    @Override
    public void run()
    {
        super.run();

        if ( ! transformedBox.isValid() ) return;

        IJ.log( "# Mask images" );
        RealMaskRealInterval mask = transformedBox.asMask();
        List< ? extends Image< ? > > images = selectedImages.getNames().stream()
                .map( DataStore::getImage )
                .collect( Collectors.toList() );

        for ( Image< ? > image : images )
        {
            MaskedImage< ? > maskedImage = new MaskedImage<>( ( Image ) image, image.getName() + "_masked", mask );

            DataStore.addImage( maskedImage );
            // TODO: probably the masking should happen within the view
            //       then we would need a new "MaskingTransformation"
            View view = ViewManager.createImageView( maskedImage, maskedImage.getName(), null, "" );

            MoBIE.getInstance().getViewManager().show( view );

            // log the masked region
            AffineTransform3D sourceTransform = new AffineTransform3D();
            maskedImage.getSourcePair().getSource().getSourceTransform( 0,0 , sourceTransform);
            sourceTransform = sourceTransform.inverse();
            double[] sourceCenter = center.clone();
            double[] sourceSize = size.clone();
            sourceTransform.apply( center, sourceCenter );
            for ( int d = 0; d < 3; d++ )
                sourceSize[ d ] = Affine3DHelpers.extractScale( sourceTransform, d ) * size[ d ];
            IJ.log( "  Image: " + image.getName() );
            IJ.log( "    Center in image voxel coordinate system: " + Arrays.toString( sourceCenter ) );
            IJ.log( "    Size in image voxel coordinate system: " + Arrays.toString( sourceSize ) );
            // IJ.log( "Interval in source coordinate system: " + Arrays.toString( interval.minAsDoubleArray() ) + ", " + Arrays.toString( interval.maxAsDoubleArray() ) );
            // IJ.log( "Interval transform in global coordinate system: " + Arrays.toString( transform.getRowPackedCopy() ) );
        }
    }
}
