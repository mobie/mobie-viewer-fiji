package de.embl.cba.mobie.ui.command;

import bdv.ij.ExportImagePlusAsN5PlugIn;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.projects.ProjectsCreatorPanel;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;
import static de.embl.cba.morphometry.Utils.labelMapAsImgLabelingRobert;
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

    // @Parameter( label = "Current Image")
    // public ImagePlus currentImage;

    @Parameter (label= "Add to:", choices={"existing dataset", "new dataset"}, style="listBox")
    public String datasetType;

    // TODO - add a way to add existing mobie files also e.g. link to current location, copy to project, move to project

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
