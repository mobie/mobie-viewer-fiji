package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.projects.projectsCreator.ProjectsCreator;
import de.embl.cba.mobie.projects.projectsCreator.RemoteMetadataCreator;
import de.embl.cba.mobie.projects.projectsCreator.ui.ProjectsCreatorPanel;
import de.embl.cba.mobie.utils.Utils;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;

import static de.embl.cba.mobie.utils.ui.SwingUtils.resetSwingLookAndFeel;
import static org.scijava.ItemVisibility.MESSAGE;

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

    @Parameter( label= "Authentication", choices={"Anonymous", "Protected"}, style="listBox")
    public String authentication = "Anonymous";

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
        resetSwingLookAndFeel();

        File dataLocation = new File( projectLocation, "data" );

        if ( !projectLocation.exists() ) {
            Utils.log( "Add Remote failed - MoBIE project does not exist!" );
        } else if ( !dataLocation.exists() ) {
            Utils.log( "Add remote failed - this folder does not contain a valid MoBIE project structure. \n " +
                    "Please choose a MoBIE project folder (this contains a 'data' folder at the top level)" );
        } else {

            if ( continueDialog() ) {
                ProjectsCreator projectsCreator = new ProjectsCreator(projectLocation);
                RemoteMetadataCreator remoteMetadataCreator = projectsCreator.getRemoteMetadataCreator();

                if (authentication.equals("Anonymous")) {
                    remoteMetadataCreator.createRemoteMetadata( signingRegion, serviceEndpoint, bucketName,
                            ProjectsCreator.Authentication.Anonymous );
                } else {
                    remoteMetadataCreator.createRemoteMetadata( signingRegion, serviceEndpoint, bucketName,
                            ProjectsCreator.Authentication.Protected );
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
