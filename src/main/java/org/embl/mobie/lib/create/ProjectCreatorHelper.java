/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.create;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimData;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.util.ThreadHelper;
import org.embl.mobie.lib.view.AdditionalViews;
import ucar.units.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

import static org.embl.mobie.ui.UserInterfaceHelper.tidyString;

/**
 * Utility class for helper functions used during project creation
 */
public class ProjectCreatorHelper {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    /**
     * Check if entered affine string is valid - i.e. does it only contain valid characters, and have a length of 12
     * @param affine affine string
     * @return whether the affine string is valid or not
     */
    public static boolean isValidAffine(String affine) {
        if (!affine.matches("^[-0-9., ]+$")) {
            IJ.log("Invalid affine transform - must contain only numbers, commas and spaces");
            return false;
        }

        String[] splitAffine = affine.split(",");
        if (splitAffine.length != 12) {
            IJ.log("Invalid affine transform - must be of length 12");
            return false;
        }

        return true;
    }

    /**
     * Parses an affine string to a valid AffineTransform3D
     * @param affine an affine string
     * @return a valid AffineTransform3D
     */
    public static AffineTransform3D parseAffineString(String affine) {
        if (isValidAffine(affine)) {
            AffineTransform3D sourceTransform = new AffineTransform3D();
            // remove spaces
            affine = affine.replaceAll("\\s","");
            String[] splitAffineTransform = affine.split(",");
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

    /**
     * Generates a default affine transform for a given ImagePlus image - this only sets the scaling to match the
     * voxel dimensions of the image.
     * @param imp ImagePlus image
     * @return An AffineTransform 3D with scaling set to match the image
     */
    public static AffineTransform3D generateDefaultAffine(ImagePlus imp) {
        final double pixelWidth = imp.getCalibration().pixelWidth;
        final double pixelHeight = imp.getCalibration().pixelHeight;
        final double pixelDepth = imp.getCalibration().pixelDepth;

        AffineTransform3D defaultAffine = new AffineTransform3D();
        defaultAffine.scale( pixelWidth, pixelHeight, pixelDepth );

        return defaultAffine;
    }

    /**
     * Finds filepath of directory containing project data i.e. checks if a 'data' folder is present
     * @param projectLocation project location directory
     * @return directory containing project data
     */
    public static File getDataLocation( File projectLocation ) {
        String dataDirectoryPath = IOHelper.combinePath(projectLocation.getAbsolutePath(), "data");
        File dataDirectory = new File( dataDirectoryPath );
        if( !dataDirectory.exists() ) {
            return projectLocation;
        } else {
            return dataDirectory;
        }
    }

    public static boolean is2D( String uri )
    {
        return is2D( ImageDataOpener.open( uri, ImageDataFormat.fromPath( uri ), ThreadHelper.sharedQueue ) );
    }

    public static boolean is2D( ImageData< ? > imageData ) {
        long[] dimensions = imageData.getSourcePair( 0 ).getB().getSource( 0,0  ).dimensionsAsLongArray();;
        if ( dimensions.length < 3 || dimensions[2] == 1 ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get mapping of ui selection groups (i.e. MoBIE dropdown menu names) to views for a given dataset
     * @param dataset dataset
     * @return Map of ui selection group names to array of view names
     */
    public static Map<String, ArrayList<String>> getGroupToViewsMap( Dataset dataset ) {
        return getGroupToViewsMap( dataset.views() );
    }

    /**
     * Get mapping of ui selection groups (i.e. MoBIE dropdown menu names) to views for given additionalViews (i.e.
     * views saved in a separate views json file).
     * @param additionalViews additional views - from a separate views json file
     * @return Map of ui selection group names to array of view names
     */
    public static Map<String, ArrayList<String>> getGroupToViewsMap( AdditionalViews additionalViews ) {
        return getGroupToViewsMap(additionalViews.views);
    }

    /**
     * Get mapping of ui selection groups (i.e. MoBIE dropdown menu names) to views for given views
     * @param viewNameToView map of view names to view
     * @return Map of ui selection group names to array of view names
     */
    public static Map<String, ArrayList<String>> getGroupToViewsMap( Map<String, View> viewNameToView ) {
        Map<String, ArrayList<String>> groupToViewsMap = new HashMap<>();
        for ( String viewName: viewNameToView.keySet() ) {
            View view = viewNameToView.get( viewName );
            Set< String > selectionGroups = view.getUiSelectionGroups();
            for ( String selectionGroup : selectionGroups )
            {
                if ( ! groupToViewsMap.containsKey( selectionGroup ) ) {
                    ArrayList<String> views = new ArrayList<>();
                    views.add( viewName );
                    groupToViewsMap.put( selectionGroup, views );
                } else {
                    groupToViewsMap.get( selectionGroup ).add( viewName );
                }
            }
        }

        return groupToViewsMap;
    }

    /**
     * Get number of timepoints present in spimData
     * @param spimData spimData object
     * @return number of timepoints
     */
    public static int getNumTimePointsFromSpimData( SpimData spimData ) {
        return spimData.getSequenceDescription().getTimePoints().size();
    }

    /**
     * Open dialog to choose new ui selection group name
     * @param currentUiSelectionGroups array of current ui selection group names
     * @return name of new ui selection group, or null if cancelled
     */
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

    /**
     * Dialog to enter new ui selection group name
     * @return name of new ui selection group, or null if cancelled
     */
    public static String chooseNewSelectionGroupNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose ui selection group name:");

        gd.addStringField("New ui selection group name:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return gd.getNextString();
        } else {
            return null;
        }
    }


    /**
     * Get string stating the voxel dimensions in x/y/z for the given ImagePlus image
     * @param imp ImagePlus image
     * @return String describing the voxel dimensions
     */
    public static String getVoxelSizeString( ImagePlus imp ) {
        DecimalFormat df = new DecimalFormat("#.###");
        String voxelString =  "Voxel size: " + df.format( imp.getCalibration().pixelWidth ) + ", " +
                df.format( imp.getCalibration().pixelHeight ) + ", " + df.format( imp.getCalibration().pixelDepth ) +
                " " + imp.getCalibration().getUnit();

        return voxelString;
    }

    /**
     * Convert the mu symbol into "u" - as udunits can't handle this
     * @param string string containing mu symbol
     * @return string containing 'u'
     */
    private static String replaceMu( String string ) {
        return  string.replace("\u00B5", "u");
    }

    /**
     * Check with udunits that given unit strings are equivalent
     * @param unit1 name of unit
     * @param unit2 name of unit
     * @return whether units are equivalent or not
     * @throws PrefixDBException
     * @throws UnitSystemException
     * @throws SpecificationException
     * @throws UnitDBException
     */
    public static boolean unitsEqual( String unit1, String unit2 ) throws PrefixDBException, UnitSystemException, SpecificationException, UnitDBException {
        // check with udunits that units are equivalent
        UnitFormat unitFormatter = UnitFormatManager.instance();
        Unit parsedUnit1 = unitFormatter.parse(replaceMu(unit1));
        Unit parsedUnit2 = unitFormatter.parse(replaceMu(unit2));

        return parsedUnit1.getCanonicalString().equals(parsedUnit2.getCanonicalString());
    }

    // TODO: it would be nice to support non-matching voxel units
    //       see also: https://imagesc.zulipchat.com/#narrow/stream/327326-BigDataViewer/topic/N5.20viewer.20support.20for.20different.20voxel.20units.3F
    public static boolean isImageValid( String uri, String projectVoxelUnit )
    {
        return isImageValid( ImageDataOpener.open( uri, ImageDataFormat.fromPath( uri ), ThreadHelper.sharedQueue ), projectVoxelUnit );
    }

    public static boolean isImageValid(  ImageData< ? > imageData, String projectVoxelUnit )
    {
        int numSetups = imageData.getNumDatasets();
        String calibrationUnit = imageData.getSourcePair( 0 ).getB().getVoxelDimensions().unit();
        return isImageValid( numSetups, calibrationUnit, projectVoxelUnit, true );
    }

    /**
     * Checks if image with given properties is valid for MoBIE project
     * @param nChannels number of channels
     * @param imageUnit voxel unit of image
     * @param projectUnit voxel unit of project
     * @param isFile whether image is in a BigDataViewer (bdv) compatible format e.g. N5/ome-zarr
     * @return whether image is valid for MoBIE project or not
     */
    // FIXME: For supporting linking to multi-dataset OME-Zarr we have to pull
    //   apart the voxel dimension check and the multi-channel check
    //   Even for adding an ImagePlus, we could support selecting the channel
    public static boolean isImageValid( int nChannels, String imageUnit, String projectUnit, boolean isFile ) {
        // reject multi-channel images
        if ( nChannels > 1 ) {
            if ( isFile )
            {
                // TODO: Ask the user which channel or dataset to add in a UI!
                IJ.log( "Linking multi-channel or multi-data images is not yet supported.\n" +
                        "Please write an issue or forum post if you need this functionality.\n" +
                        "https://github.com/mobie/mobie-viewer-fiji/issues\n" +
                        "https://forum.image.sc/" );
            }
            else
            {
                IJ.log(  "Multi-channel images are not supported. Please use [ Image > Color > Split Channels], and add each separately.");
            }
            return false;
        }

        // reject images with a different unit to the rest of the project
        try {
            if ( projectUnit != null && !unitsEqual( imageUnit, projectUnit) ) {
                String unitMessage = "Image has a different unit (" + imageUnit + ") to the rest of the project (" +
                        projectUnit + "). \n Please set your image unit";
                if (! isFile ) {
                    IJ.log( unitMessage + " under [ Image > Properties... ] to match the project.");
                } else {
                    IJ.log( unitMessage + " to match the project." );
                }
                return false;
            }
        }
        catch (PrefixDBException | UnitSystemException | SpecificationException | UnitDBException e) {
            IJ.log("Couldn't parse unit.");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean uriIsInsideDir( String uri, File dir) {
        if (IOHelper.getType( uri ) != IOHelper.ResourceType.FILE) {
            return false;
        }

        Path filePath = Paths.get( uri ).normalize();
        Path dirPath = Paths.get( dir.getAbsolutePath() ).normalize();

        return filePath.startsWith(dirPath);
    }

}
