package de.embl.cba.mobie.projects.projectsCreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.BdvFunctions;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.bookmark.Bookmark;
import de.embl.cba.mobie.bookmark.write.BookmarkFileWriter;
import de.embl.cba.mobie.bookmark.BookmarkReader;
import de.embl.cba.mobie.dataset.Datasets;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.*;
import de.embl.cba.mobie.n5.DownsampleBlock;
import de.embl.cba.mobie.n5.WriteImgPlusToN5;
import de.embl.cba.mobie.projects.projectsCreator.ui.ManualN5ExportPanel;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.color.ColoringLuts;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.GzipCompression;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.embl.cba.morphometry.Utils.labelMapAsImgLabeling;

public class ProjectsCreator {
    private final Project project;
    private final DatasetsCreator datasetsCreator;
    private final ImagesCreator imagesCreator;

    public enum BdvFormat {
        // TODO - add OME.ZARR
        n5
    }

    public enum ImageType {
        image,
        segmentation,
        mask
    }

    public enum AddMethod {
        link,
        copy,
        move
    }

    public ProjectsCreator ( File projectLocation ) {
        this.project = new Project( projectLocation );
        this.datasetsCreator = new DatasetsCreator( project );
        this.imagesCreator = new ImagesCreator( project );
    }

    public void addImage( String imageName, String datasetName, BdvFormat bdvFormat, ImageType imageType,
                    AffineTransform3D sourceTransform, boolean useDefaultSettings ) {
        imagesCreator.addImage( imageName, datasetName, bdvFormat, imageType, sourceTransform, useDefaultSettings );
    }

    public void addBdvFormatImage( File xmlLocation, String datasetName, ImageType imageType, AddMethod addMethod ) {
        try {
            imagesCreator.addBdvFormatImage( xmlLocation, datasetName, imageType, addMethod ) ;
        } catch (SpimDataException | IOException e ) {
            e.printStackTrace();
        }
    }

    public void addDataset( String datasetName ) {
        datasetsCreator.addDataset( datasetName );
    }

    public void renameDataset( String oldName, String newName ) {
        datasetsCreator.renameDataset( oldName, newName );
    }

    public void makeDefaultDataset( String datasetName ) {
        datasetsCreator.makeDefaultDataset( datasetName );
    }

    public boolean isDefaultDataset( String datasetName ) {
        return project.isDefaultDataset( datasetName );
    }

    public String[] getDatasetNames() {
        project.getDatasetNames();
    }

    public String[] getImageNames( String datasetName ) {
        return project.getDataset( datasetName ).getCurrentImageNames();
    }


}
