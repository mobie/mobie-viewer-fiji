/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package mobie3.viewer.projectcreator;

import bdv.img.n5.N5ImageLoader;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mobie3.viewer.serialize.Dataset;
import mobie3.viewer.view.AdditionalViews;
import mobie3.viewer.view.View;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.n5.loaders.N5FSImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.N5OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.readers.N5OmeZarrReader;
import org.embl.mobie.io.util.IOHelper;
import ucar.units.PrefixDBException;
import ucar.units.SpecificationException;
import ucar.units.Unit;
import ucar.units.UnitDBException;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;
import ucar.units.UnitSystemException;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.viewer.ui.UserInterfaceHelper.tidyString;

public class ProjectCreatorHelper {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

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

    public static AffineTransform3D generateDefaultAffine(ImagePlus imp) {
        final double pixelWidth = imp.getCalibration().pixelWidth;
        final double pixelHeight = imp.getCalibration().pixelHeight;
        final double pixelDepth = imp.getCalibration().pixelDepth;

        AffineTransform3D defaultAffine = new AffineTransform3D();
        defaultAffine.scale( pixelWidth, pixelHeight, pixelDepth );

        return defaultAffine;
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
        String dataDirectoryPath = IOHelper.combinePath(projectLocation.getAbsolutePath(), "data");
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
        final GenericDialog gd = new GenericDialog("Choose ui selection group name:");

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

    private static String replaceMu( String string ) {
        // Convert the mu symbol into "u" - as udunits can't handle this
        return  string.replace("\u00B5", "u");
    }

    public static boolean unitsEqual( String unit1, String unit2 ) throws PrefixDBException, UnitSystemException, SpecificationException, UnitDBException {
        // check with udunits that units are equivalent
        UnitFormat unitFormatter = UnitFormatManager.instance();
        Unit parsedUnit1 = unitFormatter.parse(replaceMu(unit1));
        Unit parsedUnit2 = unitFormatter.parse(replaceMu(unit2));

        return parsedUnit1.getCanonicalString().equals(parsedUnit2.getCanonicalString());
    }

    public static boolean isImageValid( int nChannels, String imageUnit, String projectUnit, boolean bdvFormat ) {
        // reject multi-channel images
        if ( nChannels > 1 ) {
            // for bdv format images, don't print the fiji shortcuts
            String channelMessage = "Multi-channel images are not supported. \n Please split the channels";
            if ( !bdvFormat ) {
                IJ.log( channelMessage + " [ Image > Color > Split Channels], and add each separately.");
            } else {
                IJ.log(channelMessage + ", and add each separately.");
            }
            return false;
        }

        // reject images with a different unit to the rest of the project
        try {
            if ( projectUnit != null && !unitsEqual( imageUnit, projectUnit) ) {
                String unitMessage = "Image has a different unit (" + imageUnit + ") to the rest of the project (" +
                        projectUnit + "). \n Please set your image unit";
                if (! bdvFormat ) {
                    IJ.log( unitMessage + " under [ Image > Properties... ] to match the project.");
                } else {
                    IJ.log( unitMessage + " to match the project." );
                }
                return false;
            }
        } catch (PrefixDBException | UnitSystemException | SpecificationException | UnitDBException e) {
            IJ.log("Couldn't parse unit.");
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
