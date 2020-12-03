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
            String xmlPath = FileAndUrlUtils.combinePath(projectLocation.getAbsolutePath(), "data", chosenDataset, "images", "local", imageName + ".xml");
            if (bdvFormat.equals("n5")) {
                IJ.run("Export Current Image as XML/N5",
                        "  export_path=" + xmlPath);
            } else if ( bdvFormat.equals("h5") ) {
                IJ.run("Export Current Image as XML/HDF5",
                        "  export_path=" + xmlPath );
            }

            // update images.json
            projectsCreatorPanel.getProjectsCreator().addToImagesJson( imageName, imageType, chosenDataset );

            // check it's a label image




            // open xml as rai, at 0 resolution layer
            // make labelRegions
            // get centres and bbs
            // make table, save table
        }


    }

    public static void main(final String... args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String[] columnNames = { "label_id", "anchor_x", "anchor_y",
                "anchor_z", "bb_min_x", "bb_min_y", "bb_min_z", "bb_max_x",
                "bb_max_y", "bb_max_z" };

        final LazySpimSource labelsSource = new LazySpimSource( "labelImage", "C:\\Users\\meechan\\Documents\\temp\\mobie_test\\ruse\\data\\eldeer\\images\\local\\ahoy_label.xml" );
        // has to already be as a labeling type
        // warn needs to be integer, 0 counted as background
        final RandomAccessibleInterval< IntType > rai = labelsSource.getNonVolatileSource( 0, 0);
        ImgLabeling< Integer, IntType > imgLabeling = labelMapAsImgLabeling( rai );

        LabelRegions labelRegions = new LabelRegions( imgLabeling );
        Iterator<LabelRegion> labelRegionIterator = labelRegions.iterator();

        ArrayList<Object[]> rows = new ArrayList<>();
        // ArrayList<Integer> labelIds = new ArrayList<>();
        // ArrayList<double[]> centres = new ArrayList<>();
        // ArrayList<double[]> bbMins = new ArrayList<>();
        // ArrayList<double[]> bbMaxs = new ArrayList<>();
        while ( labelRegionIterator.hasNext() ) {
            Object[] row = new Object[columnNames.length ];
            LabelRegion labelRegion = labelRegionIterator.next();


            double[] centre = new double[rai.numDimensions()];
            labelRegion.getCenterOfMass().localize( centre );
            double[] bbMin = new double[rai.numDimensions()];
            double[] bbMax = new double[rai.numDimensions()];
            labelRegion.realMin( bbMin );
            labelRegion.realMax( bbMax );

            row[0] =  labelRegion.getLabel();
            row[1] = centre[0];
            row[2] = centre[1];
            row[3] = centre[2];
            row[4] = bbMin[0];
            row[5] = bbMin[1];
            row[6] = bbMin[2];
            row[7] = bbMax[0];
            row[8] = bbMax[1];
            row[9] = bbMax[2];

            rows.add( row );

        }

        Object[][] rowArray = new Object[ rows.size() ] [ columnNames.length ];
        rowArray = rows.toArray( rowArray );
        // compensate for spacing

        // make a Jtable
        JTable table = new JTable( rowArray, columnNames);
        Tables.saveTable( table, new File ("C:\\Users\\meechan\\Documents\\temp\\mobie_test\\ruse\\data\\eldeer\\images\\local\\test_output.csv"));

    }

}
