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

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.io.ImageDataInfo;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.transform.TransformHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(type = BdvPlaygroundActionCommand.class,
        menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Log Images Info")
public class SourcesInfoCommand implements BdvPlaygroundActionCommand
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    @Parameter
    public BdvHandle bdvHandle;

    //@Parameter (label = "Show transformation history")
    public Boolean showTransformationHistory = true;

    @Override
    public void run()
    {
        int t = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
        List< SourceAndConverter< ? > > visibleSacs = MoBIEHelper.getVisibleSacs( bdvHandle );
        visibleSacs.forEach( sac ->
        {
            Image< ? > image = DataStore.sourceToImage().get( sac );
            if ( image instanceof RegionAnnotationImage )
                return;

            ImageDataInfo imageDataInfo = MoBIEHelper.fetchImageDataInfo( image );

            Source< ? > source = sac.getSpimSource();
            AffineTransform3D sourceTransform = new AffineTransform3D();
            sac.getSpimSource().getSourceTransform( t, 0, sourceTransform );

            IJ.log( "" );
            IJ.log( "# " + source.getName() );
            IJ.log( "Source URI: " + imageDataInfo.uri );
            IJ.log( "Dataset index within URI: " + imageDataInfo.datasetId );
            IJ.log( "Data type: " + source.getType().getClass().getSimpleName() );
            IJ.log( "Shape: " + Arrays.toString( source.getSource( t,0 ).dimensionsAsLongArray() ) );
            IJ.log( "Number of resolution levels: " + source.getNumMipmapLevels() );
            IJ.log( "Voxel size: " + Arrays.toString( source.getVoxelDimensions().dimensionsAsDoubleArray() ) );

            ArrayList< Transformation > transformations = TransformHelper.fetchAllImageTransformations( image );
            Transformation imageTransformation = transformations.get( 0 );
            if ( imageTransformation instanceof AffineTransformation )
            {
                AffineTransform3D initialTransform = ( ( AffineTransformation ) imageTransformation ).getAffineTransform3D();
                IJ.log( "Original image transformation: " + MoBIEHelper.print( initialTransform.getRowPackedCopy(), 3 ) );
                AffineTransform3D additionalTransform = sourceTransform.copy().concatenate( initialTransform.inverse() );
                IJ.log( "Combined additional transformation: " + MoBIEHelper.print( additionalTransform.getRowPackedCopy(), 3 ) );
                IJ.log( "Total transformation: " +  MoBIEHelper.print( sourceTransform.getRowPackedCopy(), 3 )  );
            }

            if ( showTransformationHistory )
            {
                IJ.log( "## Transformation history" );
                transformations.forEach( transformation ->
                {
                    IJ.log( transformation.toString() );
                } );
            }
        });
    }
}
