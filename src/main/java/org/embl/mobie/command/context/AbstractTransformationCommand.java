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

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.widget.SelectableImages;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.transform.TransformationOutput;
import org.embl.mobie.lib.view.ViewManager;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractTransformationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Interactive, Initializable
{
    public static final String GROUP = "group: ";
    @Parameter
    public BdvHandle bdvHandle;

    // Too complex to maintain right now
    //@Parameter ( label = "Transformation target" )
    public TransformationOutput mode = TransformationOutput.CreateNewImage;

//    @Parameter ( label = "Moving image(s)", choices = {""},
//            description = "Select the image that you want to transform.\n" +
//                    "For transforming multiple images at once, configure a group within the\n" +
//                    "BigDataViewer side panel [ P ] and restart this command; \n" +
//                    "the group will appear as a selection choice.",
//            callback = "setMovingImages",
//            style = "radioButtonHorizontal")
//    public String selectedSourceName;

    // Note that this is populated by org.embl.mobie.command.widget.SwingSelectableImagesWidget
    @Parameter ( label = "Moving image(s)" ) // , callback = "setMovingImages"
    public SelectableImages selectedImages = new SelectableImages();

    @Parameter ( label = "Transformed image(s) suffix",
            description = "Upon transformation this suffix will be appended to the moving image name.\n" +
                    "Carefully choose a meaningful suffix here that will create a unique new image name.\n" +
                    "If you leave this empty the input image view will be overwritten.")
    public String suffix = "transformed";

    // Too complex to maintain right now
    //@Parameter ( label = "Preview transformation", callback = "previewTransform" )
    //protected Boolean previewTransform = false;

    protected Collection< SourceAndConverter< ? > > sacs;
    protected Collection< SourceAndConverter< ? > > movingSacs;
    protected Collection< Image< ? > > movingImages;
    protected Collection< TransformedSource< ? > > movingSources;
    protected Map< TransformedSource< ? >, AffineTransform3D > movingSourcesToInitialTransform;
    protected List< String > selectableSourceNames; // used by some child classes


    @Override
    public void initialize()
    {
        sacs = MoBIEHelper.getVisibleSacs( bdvHandle );

//        selectableSourceNames = sacs.stream()
//                .map( sac -> sac.getSpimSource().getName() )
//                .collect( Collectors.toList() );
//
//        SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
//        List< String > groupNames = state.getGroups().stream()
//                .filter( group -> ! state.getSourcesInGroup( group ).isEmpty() )
//                .map( state::getGroupName )
//                .collect( Collectors.toList() );
//
//        for ( String groupName : groupNames )
//        {
//            selectableSourceNames.add( GROUP + groupName );
//        }

//        selectedSourceName = selectableSourceNames.get( 0 );
//
//        getInfo().getMutableInput( "selectedSourceName", String.class )
//                .setDefaultValue( selectedSourceName );

        // setMovingImages();
    }

    protected void applyTransform( AffineTransform3D affineTransform3D )
    {
        if ( mode.equals( TransformationOutput.TransformImage ) )
        {
            throw new RuntimeException( "In place transformation is currently not supported; just give the output image the same name as the input image.");
            //   applyTransformInPlace( affineTransform3D );
        }

        createSaveAndViewAffineTransformedImages( movingImages, affineTransform3D, suffix );

        // Remove the moving image displays
        // because we are now showing the transformed once
        List< String > movingImageNames = selectedImages.getNames();
        ViewManager viewManager = MoBIE.getInstance().getViewManager();
        List< Display > displays = viewManager.getCurrentSourceDisplays();
        List< Display > displaysToRemove = displays.stream()
                .filter( display -> display.getSources().size() == 1 )
                .filter( display -> display.getSources().stream().anyMatch( movingImageNames::contains ) )
                .collect( Collectors.toList() );

        for ( Display display : displaysToRemove )
            viewManager.removeDisplay( display, false );

        // FIXME close the Command UI, HOW?
        //    Maybe we use the hack that finds the awt Window based on its name?
        //    https://imagesc.zulipchat.com/#narrow/stream/327238-Fiji/topic/Close.20Scijava.20Command.20UI
    }
    
    protected static void createSaveAndViewAffineTransformedImages(
            Collection< Image< ? > > movingImages,
            AffineTransform3D affineTransform3D,
            String suffix )
    {
        for ( Image< ? > movingImage : movingImages )
        {
            String transformedImageName = movingImage.getName();
            if ( ! suffix.isEmpty() )
                transformedImageName += "-" + suffix;

            AffineTransformation affineTransformation = new AffineTransformation(
                    suffix,
                    affineTransform3D.getRowPackedCopy(),
                    Collections.singletonList( movingImage.getName() ),
                    Collections.singletonList( transformedImageName )
            );

            View view = ViewManager.createTransformedImageView(
                    movingImage,
                    transformedImageName,
                    affineTransformation,
                    movingImage.getName() + ", " + suffix );

            if ( MoBIE.getInstance().getViewManager().getViewsSaver().saveViewDialog( view ) )
            {
                // Show the transformed images
                // TODO: it would be nice to remove the non-transformed images from the current view
                MoBIE.getInstance().getViewManager().show( view );
            }
        }
    }

    protected void setMovingImages()
    {

        // Reset potential previous transforms
        if ( movingSources != null )
        {
            resetTransforms();
        }

//        if ( selectedSourceName.contains( GROUP ) )
//        {
//            String groupName = selectedSourceName.replace( GROUP, "" );
//            SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
//            SourceGroup sourceGroup = state.getGroups().stream()
//                    .filter( g -> state.getGroupName( g ).equals( groupName ) )
//                    .findFirst().get();
//            movingSacs = state.getSourcesInGroup( sourceGroup );
//            movingImages = movingSacs.stream()
//                    .map( sac -> DataStore.sourceToImage().get( sac ) )
//                    .collect( Collectors.toSet() );
//        }
//        else
//        {

        movingImages = selectedImages.getNames().stream()
                .map( name -> DataStore.getImage( name ) )
                .collect( Collectors.toList() );

        Image< ? > firstImage = movingImages.iterator().next();
        if ( firstImage instanceof RegionAnnotationImage )
        {
            assert movingImages.size() == 1;

            movingImages = ( ( RegionAnnotationImage< ? > ) firstImage ).getSelectedImages();
            movingSacs = movingImages.stream()
                    .map( img -> DataStore.sourceToImage().inverse().get( img ) )
                    .collect( Collectors.toSet() );
        }
        else
        {
            movingSacs = movingImages.stream()
                    .map( image -> DataStore.sourceToImage().inverse().get( image ) )
                    .collect( Collectors.toList() );
        }

        movingSources = movingSacs.stream()
                .map( sac -> ( TransformedSource< ? > ) sac.getSpimSource() )
                .collect( Collectors.toSet() );

        movingSourcesToInitialTransform = new HashMap<>();
        for ( TransformedSource< ? > movingSource : movingSources )
        {
            AffineTransform3D initialTransform = new AffineTransform3D();
            movingSource.getFixedTransform( initialTransform );
            movingSourcesToInitialTransform.put( movingSource, initialTransform );
        }
    }

    protected void resetTransforms()
    {
        movingSources.forEach( source -> source.setFixedTransform( movingSourcesToInitialTransform.get( source ) ) );
    }

//    protected void applyTransformInPlace( AffineTransform3D affineTransform )
//    {
//        final AffineTransform3D newFixedTransform = movingSourcesToInitialTransform.copy();
//        newFixedTransform.preConcatenate( affineTransform.copy() );
//        movingSources.setFixedTransform( newFixedTransform );
//    }

    // Should be overwritten by child classes!
//    protected void previewTransform()
//    {
//        previewTransform( new AffineTransform3D() );
//    }

//    protected void previewTransform( AffineTransform3D affineTransform3D, boolean preview )
//    {
//        getInfo().getMutableInput("previewTransformation", Boolean.class).setValue( this, preview );
//        previewTransform( affineTransform3D );
//    }


//    protected void previewTransform( AffineTransform3D affineTransform3D )
//    {
//        if ( previewTransform )
//        {
//            // add alignmentTransform
//            applyTransformInPlace( affineTransform3D );
//        }
//        else
//        {
//            // reset original transform
//            applyTransformInPlace( new AffineTransform3D() );
//        }
//
//        bdvHandle.getViewerPanel().requestRepaint();
//    }
}
