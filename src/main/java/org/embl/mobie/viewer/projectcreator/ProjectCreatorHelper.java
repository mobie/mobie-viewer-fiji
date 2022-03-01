package org.embl.mobie.viewer.projectcreator;

import bdv.img.n5.N5ImageLoader;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.n5.loaders.N5FSImageLoader;
import org.embl.mobie.io.n5.loaders.N5S3ImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.N5S3OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.readers.N5OmeZarrReader;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViews;
import org.embl.mobie.io.util.FileAndUrlUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.viewer.ui.UserInterfaceHelper.tidyString;

public class ProjectCreatorHelper {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static boolean isImageSuitable(ImagePlus imp) {
        // check the image type
        switch (imp.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.GRAY16:
            case ImagePlus.GRAY32:
                break;
            default:
                IJ.showMessage("Only 8, 16, 32-bit images are supported currently!");
                return false;
        }

        // check the image dimensionality
        if (imp.getNDimensions() < 2) {
            IJ.showMessage("Image must be at least 2-dimensional!");
            return false;
        }

        return true;
    }

    public static AffineTransform3D generateDefaultAffine(ImagePlus imp) {
        final double pixelWidth = imp.getCalibration().pixelWidth;
        final double pixelHeight = imp.getCalibration().pixelHeight;
        final double pixelDepth = imp.getCalibration().pixelDepth;

        AffineTransform3D defaultAffine = new AffineTransform3D();
        defaultAffine.scale( pixelWidth, pixelHeight, pixelDepth );

        return defaultAffine;
    }

    public static FinalVoxelDimensions getVoxelSize(ImagePlus imp) {
        final double pw = imp.getCalibration().pixelWidth;
        final double ph = imp.getCalibration().pixelHeight;
        final double pd = imp.getCalibration().pixelDepth;
        String punit = imp.getCalibration().getUnit();
        if (punit == null || punit.isEmpty())
            punit = "px";
        final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions(punit, pw, ph, pd);
        return voxelSize;
    }

    public static FinalDimensions getSize(ImagePlus imp) {
        final int w = imp.getWidth();
        final int h = imp.getHeight();
        final int d = imp.getNSlices();
        final FinalDimensions size = new FinalDimensions(w, h, d);
        return size;
    }

    public static File getSeqFileFromPath(String seqFilename) {
        final File seqFile = new File(seqFilename);
        final File parent = seqFile.getParentFile();
        if (parent == null || !parent.exists() || !parent.isDirectory()) {
            IJ.showMessage("Invalid export filename " + seqFilename);
            return null;
        }
        return seqFile;
    }

    public static AffineTransform3D generateSourceTransform(FinalVoxelDimensions voxelSize) {
        // create SourceTransform from the images calibration
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        sourceTransform.set(voxelSize.dimension(0), 0, 0, 0, 0, voxelSize.dimension(1),
                0, 0, 0, 0, voxelSize.dimension(2), 0);
        return sourceTransform;
    }

    public static File getN5FileFromXmlPath(String xmlPath) {
        final String n5Filename = xmlPath.substring(0, xmlPath.length() - 4) + ".n5";
        return new File(n5Filename);
    }

    public static File getOmeZarrFileFromXmlPath(String xmlPath) {
        final String omeZarrFileName = xmlPath.substring(0, xmlPath.length() - 4) + ".ome.zarr";
        return new File(omeZarrFileName);
    }

    public static ImageDataFormat getImageFormatFromSpimDataMinimal( SpimData spimData ) {
        ImageDataFormat imageFormat = null;
        BasicImgLoader imgLoader = spimData.getSequenceDescription().getImgLoader();
        if ( imgLoader instanceof N5FSImageLoader | imgLoader instanceof N5ImageLoader ) {
            imageFormat = ImageDataFormat.BdvN5;
        } else if ( imgLoader instanceof N5S3ImageLoader ) {
            imageFormat = ImageDataFormat.BdvN5S3;
        } else if ( imgLoader instanceof N5OMEZarrImageLoader ) {
            imageFormat = ImageDataFormat.OmeZarr;
        } else if ( imgLoader instanceof N5S3OMEZarrImageLoader ) {
            imageFormat = ImageDataFormat.OmeZarrS3;
        }

        return imageFormat;
    }

    public static File getImageLocationFromSequenceDescription( AbstractSequenceDescription seq, ImageDataFormat imageFormat ) {
        File imageLocation = null;

        // get image loader to find absolute image location
        BasicImgLoader imgLoader = seq.getImgLoader();
        switch ( imageFormat ) {
            case BdvN5:
                if (imgLoader instanceof N5ImageLoader) {
                    N5ImageLoader n5ImageLoader = (N5ImageLoader) imgLoader;
                    imageLocation = n5ImageLoader.getN5File();
                } else if (imgLoader instanceof N5FSImageLoader) {
                    N5FSImageLoader n5ImageLoader = (N5FSImageLoader) imgLoader;
                    imageLocation = n5ImageLoader.getN5File();
                }
                break;

            case BdvOmeZarr:

            case OmeZarr:
                if (imgLoader instanceof N5OMEZarrImageLoader) {
                    N5OMEZarrImageLoader zarrLoader = (N5OMEZarrImageLoader) imgLoader;
                    imageLocation = new File( ((N5OmeZarrReader) zarrLoader.n5).getBasePath() );
                }
                break;
        }

        return imageLocation;
    }

    public static File getDataLocation( File projectLocation ) {
        String dataDirectoryPath = FileAndUrlUtils.combinePath(projectLocation.getAbsolutePath(), "data");
        File dataDirectory = new File( dataDirectoryPath );
        if( !dataDirectory.exists() ) {
            return projectLocation;
        } else {
            return dataDirectory;
        }
    }

    public static boolean isSpimData2D( SpimData spimData ) {
        BasicViewSetup firstSetup = spimData.getSequenceDescription().getViewSetupsOrdered().get(0);
        long[] dimensions = firstSetup.getSize().dimensionsAsLongArray();
        if ( dimensions.length < 3 || dimensions[2] == 1 ) {
            return true;
        } else {
            return false;
        }
    }

    public static Map<String, ArrayList<String>> getGroupToViewsMap( Dataset dataset ) {
        Map<String, ArrayList<String>> groupToViewsMap = new HashMap<>();
        for ( String viewName: dataset.views.keySet() ) {
            View view = dataset.views.get( viewName );
            String group = view.getUiSelectionGroup();
            if ( !groupToViewsMap.containsKey( group ) ) {
                ArrayList<String> views = new ArrayList<>();
                views.add( viewName );
                groupToViewsMap.put( group, views );
            } else {
                groupToViewsMap.get( group ).add( viewName );
            }
        }

        return groupToViewsMap;
    }

    public static Map<String, ArrayList<String>> getGroupToViewsMap( AdditionalViews additionalViews ) {
        Map<String, ArrayList<String>> groupToViewsMap = new HashMap<>();
        for ( String viewName: additionalViews.views.keySet() ) {
            View view = additionalViews.views.get( viewName );
            String group = view.getUiSelectionGroup();
            if ( !groupToViewsMap.containsKey( group ) ) {
                ArrayList<String> views = new ArrayList<>();
                views.add( viewName );
                groupToViewsMap.put( group, views );
            } else {
                groupToViewsMap.get( group ).add( viewName );
            }
        }

        return groupToViewsMap;
    }

    public static int getNTimepointsFromSpimData( SpimData spimData ) {
        return spimData.getSequenceDescription().getTimePoints().size();
    }

    public static String makeNewUiSelectionGroup( String[] currentUiSelectionGroups ) {
        String newUiSelectionGroup = chooseNewSelectionGroupNameDialog();

        // get rid of any spaces, warn for unusual characters
        if ( newUiSelectionGroup != null ) {
            newUiSelectionGroup = tidyString(newUiSelectionGroup);
        }

        if ( newUiSelectionGroup != null ) {
            boolean alreadyExists = Arrays.asList(currentUiSelectionGroups).contains( newUiSelectionGroup );
            if ( alreadyExists ) {
                newUiSelectionGroup = null;
                IJ.log("Saving view aborted - new ui selection group already exists");
            }
        }

        return newUiSelectionGroup;
    }

    public static String chooseNewSelectionGroupNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose ui selection group Name:");

        gd.addStringField("New ui selection group name:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return gd.getNextString();
        } else {
            return null;
        }
    }

    public static String imageFormatToFolderName( ImageDataFormat imageFormat ) {
        return imageFormat.toString().replaceAll("\\.", "-");
    }

    public static String getVoxelSizeString( ImagePlus imp ) {
        DecimalFormat df = new DecimalFormat("#.###");
        String voxelString =  "Voxel size: " + df.format( imp.getCalibration().pixelWidth ) + ", " +
                df.format( imp.getCalibration().pixelHeight ) + ", " + df.format( imp.getCalibration().pixelDepth ) +
                " " + imp.getCalibration().getUnit();

        return voxelString;
    }
}
