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
import bdv.viewer.SourceGroup;
import bdv.viewer.SynchronizedViewerState;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
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

    @Parameter ( label = "Moving image(s)", choices = {""}, callback = "setMovingImages" )
    public String selectedSourceName;

    // Too complex to maintain right now
    //@Parameter ( label = "Preview transformation", callback = "previewTransform" )
    //protected Boolean previewTransform = false;

    protected Collection< SourceAndConverter< ? > > visibleSacs;
    protected Collection< SourceAndConverter< ? > > movingSacs;
    protected Collection< Image< ? > > movingImages;
    protected Collection< TransformedSource< ? > > movingSources;
    protected Map< TransformedSource< ? >, AffineTransform3D > movingSourcesToInitialTransform;
    protected List< String > selectableSourceNames; // used by some child classes


    @Override
    public void initialize()
    {
        visibleSacs = MoBIEHelper.getVisibleSacs( bdvHandle );

        selectableSourceNames = visibleSacs.stream()
                .map( sac -> sac.getSpimSource().getName() )
                .collect( Collectors.toList() );

        SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
        List< String > groupNames = state.getGroups().stream()
                .filter( group -> ! state.getSourcesInGroup( group ).isEmpty() )
                .map( state::getGroupName )
                .collect( Collectors.toList() );

        for ( String groupName : groupNames )
        {
            selectableSourceNames.add( GROUP + groupName );
        }

        getInfo().getMutableInput( "selectedSourceName", String.class )
                .setChoices( selectableSourceNames );

        selectedSourceName = selectableSourceNames.get( 0 );

        getInfo().getMutableInput( "selectedSourceName", String.class )
                .setDefaultValue( selectedSourceName );

        setMovingImages();
    }

    protected void applyTransform( AffineTransform3D affineTransform3D, String transformationType )
    {
        if ( mode.equals( TransformationOutput.TransformImage ) )
        {
            throw new RuntimeException( "In place transformation is currently not supported; just give the output image the same name as the input image.");
            //   applyTransformInPlace( affineTransform3D );
        }

        createAffineTransformedImages( movingImages, affineTransform3D, transformationType );

        // FIXME close the Command UI, HOW?
        // https://imagesc.zulipchat.com/#narrow/stream/327238-Fiji/topic/Close.20Scijava.20Command.20UI
    }
    
    protected void createAffineTransformedImages( Collection< Image< ? > > movingImages, AffineTransform3D affineTransform3D, String transformationSuffix )
    {
        String suffix = transformedImageSuffixUI( transformationSuffix );

        for ( Image< ? > movingImage : movingImages )
        {
            // FIXME: Make this more convenient that the user does not have to enter
            //        everything for each image
            String transformedImageName = movingImage.getName() + "-" + suffix;

            AffineTransformation affineTransformation = new AffineTransformation(
                    transformationSuffix,
                    affineTransform3D.getRowPackedCopy(),
                    Collections.singletonList( movingImage.getName() ),
                    Collections.singletonList( transformedImageName )
            );

            ViewManager.createTransformedImageView(
                    movingImage,
                    transformedImageName,
                    affineTransformation,
                    transformationSuffix + " transformation of " + selectedSourceName
            );
        }
    }

    protected String transformedImageSuffixUI( String transformationSuffix )
    {
        final GenericDialog gd = new GenericDialog("Transformed Image(s) Suffix");
        gd.addStringField( "Suffix for the transformed image(s)", transformationSuffix, 40 );
        gd.showDialog();
        if ( gd.wasCanceled() ) return "";
        return gd.getNextString();
    }

    // button callback => rename with care!
    protected void setMovingImages()
    {
        // Reset potential previous transforms
        if ( movingSources != null )
        {
            resetTransforms();
        }

        if ( selectedSourceName.contains( GROUP ) )
        {
            String groupName = selectedSourceName.replace( GROUP, "" );
            SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
            SourceGroup sourceGroup = state.getGroups().stream()
                    .filter( g -> state.getGroupName( g ).equals( groupName ) )
                    .findFirst().get();
            movingSacs = state.getSourcesInGroup( sourceGroup );
            movingImages = movingSacs.stream()
                    .map( sac -> DataStore.sourceToImage().get( sac ) )
                    .collect( Collectors.toSet() );
        }
        else
        {
            Image< ? > image = DataStore.getImage( selectedSourceName );
            if ( image instanceof RegionAnnotationImage )
            {
                movingImages = ( ( RegionAnnotationImage< ? > ) image ).getSelectedImages();
                movingSacs = movingImages.stream()
                        .map( img -> DataStore.sourceToImage().inverse().get( img ) )
                        .collect( Collectors.toSet() );
            }
            else
            {
                movingImages = Collections.singletonList( image );
                movingSacs = Collections.singletonList( DataStore.sourceToImage().inverse().get( image ) );
            }
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
