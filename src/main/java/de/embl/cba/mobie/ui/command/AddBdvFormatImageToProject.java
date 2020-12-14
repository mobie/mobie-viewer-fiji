package de.embl.cba.mobie.ui.command;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.projects.ProjectsCreatorPanel;
import de.embl.cba.mobie.utils.Utils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import net.imagej.ImageJ;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import bdv.spimdata.XmlIoSpimDataMinimal;

import java.io.File;
import java.io.IOException;

import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Add Bdv Format Image To MoBIE Project..." )
public class AddBdvFormatImageToProject implements Command {

    @Parameter( label = "Select Project Location", style="directory" )
    public File projectLocation;

    @Parameter( label = "Select image xml" )
    public File xmlLocation;

    // TODO - add possiblity to move image too (if don't want to copy it)
    @Parameter (label= "Add method:", choices={"link to current image location", "copy image"}, style="listBox")
    public String addMethod;

    @Parameter( label = "Image Type", choices={"image", "segmentation", "mask"} )
    public String imageType;

    @Parameter (label= "Bdv format:", choices={"n5", "h5"}, style="listBox")
    public String bdvFormat;

    @Parameter (label= "Add to:", choices={"existing dataset", "new dataset"}, style="listBox")
    public String datasetType;

    @Override
    public void run()
    {
        // if ( xmlLocation.exists() ) {
        //     if (FileNameUtils.getExtension(xmlLocation.getAbsolutePath()).equals("xml")) {
        //
        //         ProjectsCreatorPanel projectsCreatorPanel = new ProjectsCreatorPanel(projectLocation);
        //         String chosenDataset;
        //         if (datasetType.equals("new dataset")) {
        //             chosenDataset = projectsCreatorPanel.addDatasetDialog();
        //         } else {
        //             chosenDataset = projectsCreatorPanel.chooseDatasetDialog();
        //         }
        //
        //         if (chosenDataset != null) {
        //             try {
        //                 projectsCreatorPanel.getProjectsCreator().addBdvFormatImage( xmlLocation, chosenDataset, bdvFormat, imageType, addMethod );
        //             } catch (SpimDataException | IOException e) {
        //                 e.printStackTrace();
        //             }
        //         }
        //
        //     } else {
        //         Utils.log("Add image failed - not an xml file");
        //     }
        // } else {
        //     Utils.log("Add image failed - xml file does not exist");
        // }
    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

    }

}
