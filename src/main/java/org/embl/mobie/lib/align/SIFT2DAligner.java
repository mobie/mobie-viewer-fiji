package org.embl.mobie.lib.align;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.*;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.command.context.AutomaticRegistrationCommand;

/**
 * Extract landmark correspondences in two images as PointRoi.
 *
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} with respect to a transformation model to identify
 * landmark correspondences.
 *
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.4b
 *
 * Modified by Christian Tischer
 */
public class SIFT2DAligner
{
    final static private DecimalFormat decimalFormat = new DecimalFormat();
    final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    private final ImagePlus impA;
    private final ImagePlus impB;
    final private List< Feature > fs1 = new ArrayList< Feature >();
    final private List< Feature > fs2 = new ArrayList< Feature >();;
    private AffineTransform3D siftTransform;

    static private class Param
    {
        final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();

        public Double pixelSize = null;

        /**
         * Closest/next closest neighbour distance ratio
         */
        public float rod = 0.92f;

        public boolean useGeometricConsensusFilter = true;

        /**
         * Maximal allowed alignment error in px
         */
        public float maxEpsilon = 25.0f;

        /**
         * Inlier/candidates ratio
         */
        public float minInlierRatio = 0.05f;

        /**
         * Minimal absolute number of inliers
         */
        public int minNumInliers = 7;

        /**
         * Implemeted transformation models for choice
         */
        public String transformationType;
    }

    final static private Param p = new Param();

    public SIFT2DAligner( ImagePlus impA, ImagePlus impB, String transformationType )
    {
        this.impA = impA;
        this.impB = impB;

        decimalFormatSymbols.setGroupingSeparator( ',' );
        decimalFormatSymbols.setDecimalSeparator( '.' );
        decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
        decimalFormat.setMaximumFractionDigits( 3 );
        decimalFormat.setMinimumFractionDigits( 3 );

        p.transformationType = transformationType;
    }

    // TODO: make it an extra optional step to adapt the parameters
    public boolean run( Boolean showIntermediates )
    {
        final GenericDialog gd = new GenericDialog( "SIFT 2D Aligner" );

        gd.addNumericField( "initial_gaussian_blur :", p.sift.initialSigma, 2, 6, "px" );
        gd.addNumericField( "steps_per_scale_octave :", p.sift.steps, 0 );
        gd.addNumericField( "minimum_image_size :", p.sift.minOctaveSize, 0, 6, "px" );
        gd.addNumericField( "maximum_image_size :", p.sift.maxOctaveSize, 0, 6, "px" );

        gd.addNumericField( "feature_descriptor_size :", p.sift.fdSize, 0 );
        gd.addNumericField( "feature_descriptor_orientation_bins :", p.sift.fdBins, 0 );
        gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );

        gd.addCheckbox( "filter matches by geometric consensus", p.useGeometricConsensusFilter );
        gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
        gd.addNumericField( "minimal_inlier_ratio :", p.minInlierRatio, 2 );
        gd.addNumericField( "minimal_number_of_inliers :", p.minNumInliers, 0 );

        gd.showDialog();

        if (gd.wasCanceled()) return false;

        p.sift.initialSigma = ( float )gd.getNextNumber();
        p.sift.steps = ( int )gd.getNextNumber();
        p.sift.minOctaveSize = ( int )gd.getNextNumber();
        p.sift.maxOctaveSize = ( int )gd.getNextNumber();

        p.sift.fdSize = ( int )gd.getNextNumber();
        p.sift.fdBins = ( int )gd.getNextNumber();
        p.rod = ( float )gd.getNextNumber();

        p.useGeometricConsensusFilter = gd.getNextBoolean();
        p.maxEpsilon = ( float )gd.getNextNumber();
        p.minInlierRatio = ( float )gd.getNextNumber();
        p.minNumInliers = ( int )gd.getNextNumber();
        
        return run( impA, impB, showIntermediates );
    }
    
    /**
     * Execute with current parameters
     *
     * @return
     *        boolean whether a model was found
     */
    private boolean run( final ImagePlus imp1, final ImagePlus imp2, Boolean showIntermediates ) {

        // cleanup
        fs1.clear();
        fs2.clear();

        final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
        final SIFT ijSIFT = new SIFT( sift );

        long start_time = System.currentTimeMillis();
        IJ.log( "Processing SIFT ..." );
        ijSIFT.extractFeatures( imp1.getProcessor(), fs1 );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
        IJ.log( fs1.size() + " features extracted." );

        start_time = System.currentTimeMillis();
        IJ.log( "Processing SIFT ..." );
        ijSIFT.extractFeatures( imp2.getProcessor(), fs2 );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
        IJ.log( fs2.size() + " features extracted." );

        start_time = System.currentTimeMillis();
        IJ.log( "Identifying correspondence candidates using brute force ..." );
        final List< PointMatch > candidates = new ArrayList< PointMatch >();
        FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );

        final ArrayList< Point > p1 = new ArrayList< Point >();
        final ArrayList< Point > p2 = new ArrayList< Point >();
        final List< PointMatch > inliers;

        boolean modelFound = false;

        if ( p.useGeometricConsensusFilter )
        {
            IJ.log( candidates.size() + " potentially corresponding features identified." );

            start_time = System.currentTimeMillis();
            IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
            inliers = new ArrayList< PointMatch >();

            AbstractModel< ? > model;
            switch ( p.transformationType )
            {
                case AutomaticRegistrationCommand.TRANSLATION:
                    model = new TranslationModel2D();
                    break;
                case AutomaticRegistrationCommand.RIGID:
                    model = new RigidModel2D();
                    break;
                case AutomaticRegistrationCommand.SIMILARITY:
                    model = new SimilarityModel2D();
                    break;
                case AutomaticRegistrationCommand.AFFINE:
                    model = new AffineModel2D();
                    break;
//                case 4:
//                    // TODO: What is this?
//                    model = new HomographyModel2D();
//                    break;
                default:
                    return modelFound;
            }


            try
            {
                modelFound = model.filterRansac(
                        candidates,
                        inliers,
                        1000,
                        p.maxEpsilon,
                        p.minInlierRatio,
                        p.minNumInliers );

                if ( model instanceof AbstractAffineModel2D )
                {
                    final double[] a = new double[6];
                    ( ( AbstractAffineModel2D< ? > ) model ).toArray( a );
                    siftTransform = new AffineTransform3D();
                    siftTransform.set(
                            a[0], a[2], 0, a[4],
                            a[1], a[3], 0, a[5],
                            0, 0, 1, 0);
                    siftTransform = siftTransform.inverse();
                }
                else
                    IJ.showMessage( "Cannot apply " + model );

            }
            catch ( final NotEnoughDataPointsException e )
            {
                modelFound = false;
            }

            IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );

            if ( modelFound )
            {
                PointMatch.apply( inliers, model );

                IJ.log( inliers.size() + " corresponding features with an average displacement of " + decimalFormat.format( PointMatch.meanDistance( inliers ) ) + "px identified." );
                IJ.log( "Estimated transformation model: " + model );
            }
            else
                IJ.log( "No correspondences found." );
        }
        else
        {
            inliers = candidates;
            IJ.log( candidates.size() + " corresponding features identified." );
        }

        if ( ! inliers.isEmpty() && showIntermediates  )
        {
            imp1.show();
            imp2.show();
            PointMatch.sourcePoints( inliers, p1 );
            PointMatch.targetPoints( inliers, p2 );
            imp1.setRoi( Util.pointsToPointRoi( p1 ) );
            imp2.setRoi( Util.pointsToPointRoi( p2 ) );
        }

        return modelFound;
    }

    public AffineTransform3D getAlignmentTransform()
    {
        return siftTransform;
    }
}