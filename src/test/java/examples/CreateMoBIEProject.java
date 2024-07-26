package examples;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.open.project.OpenMoBIEProjectCommand;
import org.embl.mobie.lib.create.ProjectCreator;

import java.io.File;
import java.io.IOException;

public class CreateMoBIEProject
{
    public static void main( String[] args ) throws IOException
    {
        // Init the project
        // Please CHANGE the below path to point to location on your computer
        String projectPath = "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/test-project";

        ProjectCreator creator = new ProjectCreator( new File( projectPath ) );
        creator.getDatasetsCreator().addDataset(
                "dataset",
                false // say whether the data is only 2D (false = 3D)
        );

        // Add one image
        ImagePlus imagePlus = IJ.openImage( "http://imagej.net/images/mri-stack.zip" );
        creator.getImagesCreator().addImage(
                imagePlus,
                "MRI",
                "dataset",
                ProjectCreator.ImageType.Image,
                new AffineTransform3D(),
                "views",
                false,  // view is exclusive
                true // overwrite
        );

        // Open the project to check whether it worked
        new ImageJ().ui().showUI();
        OpenMoBIEProjectCommand openMoBIE = new OpenMoBIEProjectCommand();
        openMoBIE.uri = creator.getProjectLocation().getAbsolutePath();
        openMoBIE.run();
    }
}
