package de.embl.cba.mobie.projectcreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import de.embl.cba.mobie.n5.N5FSImageLoader;
import de.embl.cba.mobie.n5.N5S3ImageLoader;
import de.embl.cba.mobie.Dataset;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.mobie.view.View;
import de.embl.cba.mobie.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.mobie.ui.UserInterfaceHelper.tidyString;

public class ProjectCreatorHelper {
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

    public static boolean isValidAffine(String affine) {
        if (!affine.matches("^[0-9. ]+$")) {
            IJ.log("Invalid affine transform - must contain only numbers and spaces");
            return false;
        }

        String[] splitAffine = affine.split(" ");
        if (splitAffine.length != 12) {
            IJ.log("Invalid affine transform - must be of length 12");
            return false;
        }

        return true;
    }

    public static AffineTransform3D parseAffineString(String affine) {
        if (isValidAffine(affine)) {
            AffineTransform3D sourceTransform = new AffineTransform3D();
            String[] splitAffineTransform = affine.split(" ");
            double[] doubleAffineTransform = new double[splitAffineTransform.length];
            for (int i = 0; i < splitAffineTransform.length; i++) {
                doubleAffineTransform[i] = Double.parseDouble(splitAffineTransform[i]);
            }
            sourceTransform.set(doubleAffineTransform);
            return sourceTransform;
        } else {
            return null;
        }
    }

    public static String generateDefaultAffine(ImagePlus imp) {
        final double pixelWidth = imp.getCalibration().pixelWidth;
        final double pixelHeight = imp.getCalibration().pixelHeight;
        final double pixelDepth = imp.getCalibration().pixelDepth;

        String defaultAffine = pixelWidth + " 0.0 0.0 0.0 0.0 " + pixelHeight + " 0.0 0.0 0.0 0.0 " + pixelDepth + " 0.0";
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

    public static ImageDataFormat getImageFormatFromSpimDataMinimal( SpimDataMinimal spimDataMinimal ) {
        ImageDataFormat imageFormat = null;
        BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();
        if ( imgLoader instanceof N5FSImageLoader | imgLoader instanceof N5ImageLoader ) {
            imageFormat = ImageDataFormat.BdvN5;
        } else if ( imgLoader instanceof N5S3ImageLoader ) {
            imageFormat = ImageDataFormat.BdvN5S3;
        }

        return imageFormat;
    }

    public static File getImageLocationFromSpimDataMinimal(SpimDataMinimal spimDataMinimal, ImageDataFormat imageFormat ) {
        File imageLocation = null;

        switch ( imageFormat ) {
            case BdvN5:
                // get image loader to find absolute image location
                BasicImgLoader imgLoader = spimDataMinimal.getSequenceDescription().getImgLoader();
                if (imgLoader instanceof N5ImageLoader) {
                    N5ImageLoader n5ImageLoader = (N5ImageLoader) imgLoader;
                    imageLocation = n5ImageLoader.getN5File();
                } else if (imgLoader instanceof N5FSImageLoader) {
                    N5FSImageLoader n5ImageLoader = (N5FSImageLoader) imgLoader;
                    imageLocation = n5ImageLoader.getN5File();
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

    public static boolean isSpimData2D( SpimDataMinimal spimDataMinimal ) {
        BasicViewSetup firstSetup = spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().get(0);
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

    public static int getNTimepointsFromSpimData( SpimDataMinimal spimDataMinimal ) {
        return spimDataMinimal.getSequenceDescription().getTimePoints().size();
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
}
