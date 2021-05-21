package de.embl.cba.mobie2.command;

import de.embl.cba.mobie2.projectcreator.ProjectCreator;
import de.embl.cba.mobie2.projectcreator.RemoteMetadataCreator;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static de.embl.cba.mobie2.projectcreator.ProjectCreatorHelper.getDataLocation;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Add/Update MoBIE Project remote..." )
public class AddRemoteToMoBIEProjectCommand implements Command {

    @Parameter ( label="MoBIE project folder:", style="directory" )
    public File projectLocation;

    @Parameter( label = "Signing Region" )
    public String signingRegion = "us-west-2";

    @Parameter( label = "Service Endpoint" )
    public String serviceEndpoint = "https://...";

    @Parameter( label = "Bucket Name" )
    public String bucketName;

    private boolean continueDialog() {
        int result = JOptionPane.showConfirmDialog(null,
                "This will overwrite any existing remote metadata - continue?", "Overwrite remote metadata?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run()
    {

        // using File script parameter changes the look and feel of swing, reset it to default here
        setMoBIESwingLookAndFeel();

        if ( !projectLocation.exists() ) {
            IJ.log( "Add Remote failed - MoBIE project does not exist!" );
        } else {
            if ( continueDialog() ) {
                try {
                    ProjectCreator projectsCreator = new ProjectCreator( getDataLocation( projectLocation ) );
                    RemoteMetadataCreator remoteMetadataCreator = projectsCreator.getRemoteMetadataCreator();
                    remoteMetadataCreator.createRemoteMetadata( signingRegion, serviceEndpoint, bucketName );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

}
