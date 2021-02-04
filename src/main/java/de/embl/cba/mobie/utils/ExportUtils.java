package de.embl.cba.mobie.utils;

import de.embl.cba.mobie.n5.ExportImagePlusAsN5PlugIn;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;

public class ExportUtils {

    public static boolean isImageSuitable ( ImagePlus imp ) {
        // check the image type
        switch ( imp.getType() )
        {
            case ImagePlus.GRAY8:
            case ImagePlus.GRAY16:
            case ImagePlus.GRAY32:
                break;
            default:
                IJ.showMessage( "Only 8, 16, 32-bit images are supported currently!" );
                return false;
        }

        // check the image dimensionality
        if ( imp.getNDimensions() < 2 )
        {
            IJ.showMessage( "Image must be at least 2-dimensional!" );
            return false;
        }

        return true;
    }

    public static boolean isResolutionsAndSubdivisionsSuitable ( int[][] resolutions, int[][] subdivisions,
                                                                 String resolutionsString, String subdivisionsString ) {
        if ( resolutions.length == 0 )
        {
            IJ.showMessage( "Cannot parse subsampling factors " + resolutionsString );
            return false;
        }
        if ( subdivisions.length == 0 )
        {
            IJ.showMessage( "Cannot parse chunk sizes " + subdivisionsString );
            return false;
        }
        else if ( resolutions.length != subdivisions.length )
        {
            IJ.showMessage( "subsampling factors and chunk sizes must have the same number of elements" );
            return false;
        }

        return true;
    }

    public static FinalVoxelDimensions getVoxelSize (ImagePlus imp ) {
        final double pw = imp.getCalibration().pixelWidth;
        final double ph = imp.getCalibration().pixelHeight;
        final double pd = imp.getCalibration().pixelDepth;
        String punit = imp.getCalibration().getUnit();
        if ( punit == null || punit.isEmpty() )
            punit = "px";
        final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
        return voxelSize;
    }

    public static FinalDimensions getSize (ImagePlus imp ) {
        final int w = imp.getWidth();
        final int h = imp.getHeight();
        final int d = imp.getNSlices();
        final FinalDimensions size = new FinalDimensions( w, h, d );
        return size;
    }

    public static File getSeqFileFromPath ( String seqFilename ) {
        final File seqFile = new File( seqFilename );
        final File parent = seqFile.getParentFile();
        if ( parent == null || !parent.exists() || !parent.isDirectory() )
        {
            IJ.showMessage( "Invalid export filename " + seqFilename );
            return null;
        }
        return seqFile;
    }

    public static AffineTransform3D generateSourceTransform ( FinalVoxelDimensions  voxelSize ) {
        // create SourceTransform from the images calibration
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        sourceTransform.set( voxelSize.dimension(0), 0, 0, 0, 0, voxelSize.dimension(1),
                0, 0, 0, 0, voxelSize.dimension(2), 0 );
        return sourceTransform;
    }

    public static File getN5FileFromXmlPath ( String xmlPath ) {
        final String n5Filename = xmlPath.substring( 0, xmlPath.length() - 4 ) + ".n5";
        return new File( n5Filename );
    }


}
