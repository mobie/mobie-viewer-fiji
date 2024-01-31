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

public abstract class AbstractRegistrationCommand extends AbstractTransformationCommand
{
    @Parameter ( label = "Fixed Image", choices = {""} )
    public String fixedImageName;

    @Override
    public void initialize()
    {
        super.initialize();

        getInfo().getMutableInput( "fixedImageName", String.class )
                .setChoices( imageNames );

        getInfo().getMutableInput( "fixedImageName", String.class )
                .setDefaultValue( imageNames.get( 1 ) );
    }

}
