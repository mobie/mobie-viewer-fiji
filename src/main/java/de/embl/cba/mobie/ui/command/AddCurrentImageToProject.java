package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.projects.ProjectsCreatorPanel;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.io.File;
import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Add Current Image To MoBIE Project..." )
public class AddCurrentImageToProject implements Command {

    @Parameter (visibility=MESSAGE, required=false)
    public String message = "Make sure your pixel size, and unit, are set properly under Image > Properties...";

    @Parameter( label = "Select Project Location", style="directory" )
    public File projectLocation;

    @Parameter( label = "Image Name" )
    public String imageName;

    @Parameter( label = "Image Type", choices={"image", "segmentation", "mask"} )
    public String imageType;

    @Parameter (label= "Bdv format:", choices={"n5", "h5"}, style="listBox")
    public String bdvFormat;

    @Parameter (label= "Add to:", choices={"existing dataset", "new dataset"}, style="listBox")
    public String datasetType;

    // TODO - add a way to add existing bdv xml files also e.g. link to current location, copy to project, move to project

    @Override
    public void run()
    {
        ProjectsCreatorPanel projectsCreatorPanel = new ProjectsCreatorPanel( projectLocation );

        String chosenDataset;
        if (datasetType.equals("new dataset")) {
            chosenDataset = projectsCreatorPanel.addDatasetDialog();
        } else {
            chosenDataset = projectsCreatorPanel.chooseDatasetDialog();
        }

        if ( chosenDataset != null ) {
            projectsCreatorPanel.getProjectsCreator().addImage( imageName, chosenDataset, bdvFormat, imageType );
        }
    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

    }

}
