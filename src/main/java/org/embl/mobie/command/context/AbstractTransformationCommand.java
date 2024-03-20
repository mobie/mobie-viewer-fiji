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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTransformationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Interactive, Initializable
{
    @Parameter
    public BdvHandle bdvHandle;

    //@Parameter ( label = "Transformation target" )
    public TransformationOutput mode = TransformationOutput.CreateNewImage;

    @Parameter ( label = "Moving image", choices = {""}, callback = "setMovingImage" )
    public String movingImageName;

    protected List< SourceAndConverter< ? > > sourceAndConverters;
    protected List< String > imageNames;
    protected SourceAndConverter< ? > movingSac;
    protected TransformedSource< ? > movingSource;
    protected AffineTransform3D previousFixedTransform;

    @Override
    public void cancel()
    {
        if ( movingSource != null )
            movingSource.setFixedTransform( previousFixedTransform );
        bdvHandle.getViewerPanel().requestRepaint();
    }

    @Override
    public void initialize()
    {
        sourceAndConverters = MoBIEHelper.getVisibleSacs( bdvHandle );

        imageNames = sourceAndConverters.stream()
                .map( sac -> sac.getSpimSource().getName() )
                .collect( Collectors.toList() );

        getInfo().getMutableInput( "movingImageName", String.class )
                .setChoices( imageNames );

        movingImageName = imageNames.get( 0 );

        getInfo().getMutableInput( "movingImageName", String.class )
                .setDefaultValue( movingImageName );

        setMovingImage();
    }

    protected void applyAffineTransform3D( AffineTransform3D affineTransform3D, String transformationType )
    {
        Image< ? > movingImage = DataStore.sourceToImage().get( movingSac );

        if ( movingImage instanceof RegionAnnotationImage )
        {
            // TODO: handle multiple selections instead of just taking the first one?!
            movingImage = ( ( RegionAnnotationImage< ? > ) movingImage ).getSelectedImages().get( 0 );
            movingSac = DataStore.sourceToImage().inverse().get( movingImage );
        }

        if ( mode.equals( TransformationOutput.CreateNewImage ) )
        {
            createAffineTransformedImage( affineTransform3D, transformationType );
        }
        else if ( mode.equals( TransformationOutput.TransformMovingImage ))
        {
            applyTransformInPlace( affineTransform3D );
        }
    }
    
    protected void createAffineTransformedImage( AffineTransform3D affineTransform3D, String transformationType )
    {
        String transformedImageName = askForTransformedImageName( transformationType );
        if ( transformedImageName == null ) return;

        AffineTransformation affineTransformation = new AffineTransformation(
                transformationType,
                affineTransform3D.getRowPackedCopy(),
                Collections.singletonList( movingImageName),
                Collections.singletonList( transformedImageName )
        );

        // FIXME: Change this to createTransformedImageView
        ViewManager.createTransformedSourceView(
                movingSac,
                transformedImageName,
                affineTransformation,
                transformationType + " transformation of " + movingImageName
        );
    }

    protected String askForTransformedImageName( String transformationType )
    {
        String transformedImageName = movingImageName + "-" + transformationType;

        final GenericDialog gd = new GenericDialog("Transformed Image Name");
        gd.addStringField( "Transformed Image Name", transformedImageName, 40 );
        gd.showDialog();
        if ( gd.wasCanceled() ) return null;
        return gd.getNextString();
    }

    protected void setMovingImage()
    {
        if ( movingSource != null )
        {
            // reset transform of previously selected image
            movingSource.setFixedTransform( previousFixedTransform );
        }

        // fetch the new moving image
        movingSac = sourceAndConverters.stream()
                .filter( sac -> sac.getSpimSource().getName().equals( movingImageName ) )
                .findFirst().get();
        movingSource = ( TransformedSource< ? > ) movingSac.getSpimSource();
        previousFixedTransform = new AffineTransform3D();
        movingSource.getFixedTransform( previousFixedTransform );
    }

    protected void applyTransformInPlace( AffineTransform3D affineTransform )
    {
        final AffineTransform3D newFixedTransform = previousFixedTransform.copy();
        newFixedTransform.preConcatenate( affineTransform.copy().inverse() );
        movingSource.setFixedTransform( newFixedTransform );

        // TODO what would be the pro and con of transforming the image instead as below?
        // DataStore.sourceToImage().get( movingSac ).transform( alignmentTransform );
    }

}
