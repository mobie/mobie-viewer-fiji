package org.embl.mobie.command.context;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.lib.MoBIEHelper;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTransformationCommand extends DynamicCommand implements BdvPlaygroundActionCommand, Interactive, Initializable
{
    @Parameter
    public BdvHandle bdvHandle;

    @Parameter ( label = "Transformation name" )
    public String name = "Some transformation";

    @Parameter ( label = "Moving Image", choices = {""} )
    public String movingImageName;

    protected List< SourceAndConverter< ? > > sourceAndConverters;
    protected List< String > imageNames;

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

        getInfo().getMutableInput( "movingImageName", String.class )
                .setDefaultValue( imageNames.get( 0 ) );

    }

    @Override
    public void run()
    {
        super.run();
    }


}
