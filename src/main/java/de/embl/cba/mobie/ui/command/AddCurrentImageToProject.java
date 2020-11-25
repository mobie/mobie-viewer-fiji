package de.embl.cba.mobie.ui.command;

import bdv.ij.ExportImagePlusAsN5PlugIn;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
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

    @Parameter (label= "Bdv format:", choices={"n5", "h5"}, style="listBox")
    public String bdvFormat;

    // @Parameter( label = "Current Image")
    // public ImagePlus currentImage;

    @Parameter (label= "Add to:", choices={"existing dataset", "new dataset"}, style="listBox")
    public String datasetType;

    @Override
    public void run()
    {
        // dialog to name dataset, and create it
        // dropdown of existing datasets
        IJ.run("Export Current Image as XML/HDF5",
                "  export_path=C:/Users/meechan/Documents/testooo.xml");
        IJ.run("Export Current Image as XML/N5",
                "  export_path=C:/Users/meechan/Documents/testry.xml");
        // // Dialog for new name of dataset, or dropdownof existing
        // String dataset_name = "fluffy";
        // File dataDirectory = new File( projectLocation, "data");
        // if ( dataDirectory.exists() ) {
        //     IJ.run("Export Current Image as XML/N5",
        //             "  subsampling_factors=[{ {1,1,1} }] n5_chunk_sizes=[{ {64,64,64} }] compression=[raw (no compression)] default export_path=C:/Users/meechan/Documents/testry.xml");
        // }
        // IJ.log(projectLocation.toString());
    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

    }

}
