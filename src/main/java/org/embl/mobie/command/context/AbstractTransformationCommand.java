package org.embl.mobie.command.context;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.transform.TransformationMode;
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

    @Parameter ( label = "Transformation mode" )
    public TransformationMode mode = TransformationMode.NewImage;

    @Parameter ( label = "Transformation name" )
    public String transformationName = "Some transformation";

    @Parameter ( label = "Transformed image name")
    public String transformedImageName = "Transformed image";

    @Parameter ( label = "Moving Image", choices = {""}, callback = "setMovingImage" )
    public String movingImageName;



    protected List< SourceAndConverter< ? > > sourceAndConverters;
    protected List< String > imageNames;
    protected SourceAndConverter< ? > movingSac;
    protected TransformedSource< ? > movingSource;
    protected AffineTransform3D originalTransform;

    @Override
    public void cancel()
    {
        if ( movingSource != null )
            movingSource.setFixedTransform( originalTransform );
        bdvHandle.getViewerPanel().requestRepaint();
    }

    @Override
    public void initialize()
    {
        sourceAndConverters = MoBIEHelper.getVisibleSacs( bdvHandle );

        if ( sourceAndConverters.size() < 2 )
        {
            IJ.showMessage( "There must be at least two images visible." );
            return;
        }

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

    protected void createTransformedImage( AffineTransform3D affineTransform3D, String description )
    {
        AffineTransformation affineTransformation = new AffineTransformation(
                transformationName,
                affineTransform3D.getRowPackedCopy(),
                Collections.singletonList( movingImageName),
                Collections.singletonList( transformedImageName )
        );

        ViewManager.createTransformedSourceView(
                movingSac,
                transformedImageName,
                affineTransformation,
                description + " transformation of " + movingSac.getSpimSource().getName()
        );
    }

    protected void setMovingImage()
    {
        if ( movingSource != null )
        {
            // reset transform of previously selected image
            movingSource.setFixedTransform( originalTransform );
        }

        // fetch the new moving image
        movingSac = sourceAndConverters.stream()
                .filter( sac -> sac.getSpimSource().getName().equals( movingImageName ) )
                .findFirst().get();
        movingSource = ( TransformedSource< ? > ) movingSac.getSpimSource();
        originalTransform = new AffineTransform3D();
        movingSac.getSpimSource().getSourceTransform( 0, 0, originalTransform );
    }

}
