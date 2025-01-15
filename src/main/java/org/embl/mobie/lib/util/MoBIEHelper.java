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
package org.embl.mobie.lib.util;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.github.GitHubUtils;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.image.AnnotationLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.ImageDataImage;
import org.embl.mobie.lib.image.TransformedImage;
import org.embl.mobie.lib.io.ImageDataInfo;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.embl.mobie.io.util.IOHelper.combinePath;
import static org.embl.mobie.io.util.IOHelper.getPaths;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.isSourceIntersectingCurrentView;


public abstract class MoBIEHelper
{

	public static RealPoint getGlobalMouseCoordinates( Bdv bdv )
	{
		final RealPoint posInBdvInMicrometer = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( posInBdvInMicrometer );
		return posInBdvInMicrometer;
	}

	public static ImgLabeling< Integer, IntType > labelMapAsImgLabeling( RandomAccessibleInterval< IntType > labelMap )
	{
		final ImgLabeling< Integer, IntType > imgLabeling = new ImgLabeling<>( labelMap );

		final double maximumLabel = getMaximumValue( labelMap );

		final ArrayList< Set< Integer > > labelSets = new ArrayList< >();

		labelSets.add( new HashSet<>() ); // empty 0 label
		for ( int label = 1; label <= maximumLabel; ++label )
		{
			final HashSet< Integer > set = new HashSet< >();
			set.add( label );
			labelSets.add( set );
		}

		new LabelingMapping.SerialisationAccess< Integer >( imgLabeling.getMapping() )
		{
			{
				super.setLabelSets( labelSets );
			}
		};

		return imgLabeling;
	}

	public static < T extends RealType< T > >
	Double getMaximumValue( RandomAccessibleInterval< T > rai )
	{
		Cursor< T > cursor = Views.iterable( rai ).cursor();

		double maxValue = Double.MIN_VALUE;

		double value;
		while ( cursor.hasNext() )
		{
			value = cursor.next().getRealDouble();
			if ( value > maxValue )
				maxValue = value;
		}

		return maxValue;
	}


	public static Double parseDouble( String cell )
	{
		if ( cell.equalsIgnoreCase( "nan" )
				|| cell.equalsIgnoreCase( "na" )
				|| cell.equals( "" ) )
			return Double.NaN;
		else if ( cell.equalsIgnoreCase( "inf" ) )
			return Double.POSITIVE_INFINITY;
		else if ( cell.equalsIgnoreCase( "-inf" ) )
			return Double.NEGATIVE_INFINITY;
		else
			return Double.parseDouble( cell );
	}

	public static String removeExtension( String uri )
	{
		uri = FilenameUtils.removeExtension( uri );

		// some have two extensions, e.g. .ome.zarr
		uri = FilenameUtils.removeExtension( uri );

		return uri;
	}

	public static ImageDataInfo fetchImageDataInfo( Image< ? > image )
	{
		if ( image instanceof ImageDataImage )
		{
			ImageDataInfo imageDataInfo = new ImageDataInfo();
			imageDataInfo.uri = ( ( ImageDataImage ) image ).getUri();
			imageDataInfo.datasetId = ( ( ImageDataImage ) image ).getSetupId();
			return imageDataInfo;
		}
		else if ( image instanceof TransformedImage )
		{
			TransformedImage transformedImage = ( TransformedImage ) image;
			Image< ? > wrappedImage = transformedImage.getWrappedImage();
			return fetchImageDataInfo( wrappedImage );
		}
		else if ( image instanceof AnnotationLabelImage )
		{
			Image< ? extends IntegerType< ? > > labelImage = ( ( AnnotationLabelImage< ? > ) image ).getLabelImage();
			return fetchImageDataInfo( labelImage );
		}
		else
		{
			ImageDataInfo imageDataInfo = new ImageDataInfo();
			imageDataInfo.uri = "Could not determine URI of " + image.getClass().getSimpleName();
			return imageDataInfo;
		}
	}

	public static final String GRID_TYPE_HELP = "If the images are different and not too many, use Transformed for more flexible visualisation.\n" +
			"If all images are identical use Stitched for better performance.";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static String print(double[] array, int numSignificantDigits) {
		StringBuilder pattern = new StringBuilder("#");
		if (numSignificantDigits > 0) pattern.append(".");
		for (int i = 0; i < numSignificantDigits; i++) pattern.append("#");
		DecimalFormat formatter = new DecimalFormat(pattern.toString());

		StringBuilder result = new StringBuilder();
		result.append( "(" );
		for (int i = 0; i < array.length; i++) {
			if (Math.abs(array[i]) < 1e-10) {
				array[i] = 0.0;  // Explicitly set to zero to remove negative sign
			}
			result.append(formatter.format(array[i]));
			if (i < array.length - 1) {
				result.append(", ");
			}
		}
		result.append( ")" );
		return result.toString();
	}

	public static String print(double value, int numSignificantDigits) {
		StringBuilder pattern = new StringBuilder("#");
		if (numSignificantDigits > 0) pattern.append(".");
		for (int i = 0; i < numSignificantDigits; i++) pattern.append("#");
		DecimalFormat formatter = new DecimalFormat(pattern.toString());
		return formatter.format( value );
	}

	public static <E extends Enum<E>> String[] enumAsStringArray(Class<E> enumClass) {
		return Arrays.stream(enumClass.getEnumConstants())
				.map(Enum::name)
				.toArray(String[]::new);
	}

	public static int[] asInts( long[] longs) {
		int[] ints = new int[longs.length];

		for(int i = 0; i < longs.length; ++i)
			ints[i] = (int)longs[i];

		return ints;
	}

	public static long[] asLongs( int[] ints) {
		long[] longs = new long[ints.length];

		for(int i = 0; i < longs.length; ++i)
			longs[i] = ints[i];

		return longs;
	}

	// from https://www.geeksforgeeks.org/print-longest-common-substring/
	public static String longestCommonSubstring( String s1, String s2 )
	{
		int m = s1.length();
		int n = s2.length();

		// Create a table to store lengths of longest common
		// suffixes of substrings.   Note that LCSuff[i][j]
		// contains length of longest common suffix of X[0..i-1]
		// and Y[0..j-1]. The first row and first column entries
		// have no logical meaning, they are used only for
		// simplicity of program
		int[][] LCSuff = new int[m + 1][n + 1];

		// To store length of the longest common substring
		int len = 0;

		// To store the index of the cell which contains the
		// maximum value. This cell's index helps in building
		// up the longest common substring from right to left.
		int row = 0, col = 0;

	   /* Following steps build LCSuff[m+1][n+1] in bottom
	   up fashion. */
		for (int i = 0; i <= m; i++) {
			for (int j = 0; j <= n; j++) {
				if (i == 0 || j == 0)
					LCSuff[i][j] = 0;

				else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					LCSuff[i][j] = LCSuff[i - 1][j - 1] + 1;
					if (len < LCSuff[i][j]) {
						len = LCSuff[i][j];
						row = i;
						col = j;
					}
				}
				else
					LCSuff[i][j] = 0;
			}
		}

		// if true, then no common substring exists
		if (len == 0) {
			System.out.println("No Common Substring");
			return s1;
		}

		// allocate space for the longest common substring
		String resultStr = "";

		// traverse up diagonally form the (row, col) cell
		// until LCSuff[row][col] != 0
		while (LCSuff[row][col] != 0) {
			resultStr = s1.charAt(row - 1) + resultStr; // or Y[col-1]
			--len;

			// move diagonally up to previous cell
			row--;
			col--;
		}

		//  longest common substring
		return resultStr;
	}



	public static boolean is2D( ImageData< ? > imageData, int datasetIndex )
	{
		// TODO: this could be fetched from the metadata if
		//   we decide to implement this
		long[] dimensions = imageData.getSourcePair( datasetIndex ).getB().getSource( 0, 0 ).dimensionsAsLongArray();
		if ( dimensions.length == 2 || dimensions[ 2 ] == 1)
			return true;
		else
			return false;
	}

	public static List< String > getNamedGroups( String regex )
	{
		List< String > namedGroups = new ArrayList<>();

		Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher( regex );

		while ( m.find() ) {
			namedGroups.add(m.group(1));
		}

		return namedGroups;
	}

	public static VoxelDimensions fetchVoxelDimensions( String uri )
	{
		VoxelDimensions voxelDimensions = ImageDataOpener
				.open( uri,
						ImageDataFormat.fromPath( uri ),
						ThreadHelper.sharedQueue )
				.getSourcePair( 0 )
				.getB()
				.getVoxelDimensions();

		return voxelDimensions;
	}

	public static String createAbsolutePath( String rootFolder, String fileName, String folderName )
	{
		// The "root" is AutoMic table specific
		Path relativePath = Paths.get( folderName, fileName );
		Path tempPath = Paths.get( "root" ).relativize( relativePath );
		Path resolve = Paths.get( rootFolder ).resolve( tempPath );
		return resolve.toString();
	}

	public static String createPath( String rootLocation, String githubBranch, String... files )
	{
		if ( rootLocation.contains( "github.com" ) )
		{
			rootLocation = GitHubUtils.createRawUrl( rootLocation, githubBranch );
		}

		final ArrayList< String > strings = new ArrayList<>();
		strings.add( rootLocation );
		Collections.addAll( strings, files );
		final String path = combinePath( strings.toArray( new String[0] ) );

		return path;
	}

	public static String getLog( AtomicInteger dataSetIndex, int numTotal, AtomicInteger dataSetLoggingInterval, AtomicLong lastLogMillis )
	{
		final int currentDatasetIndex = dataSetIndex.incrementAndGet();

		if ( currentDatasetIndex % dataSetLoggingInterval.get() == 0  )
		{
			// Update logging frequency
			// such that a message appears
			// approximately every 5000 ms
			final long currentTimeMillis = System.currentTimeMillis();
			if ( currentTimeMillis - lastLogMillis.get() < 4000 )
				dataSetLoggingInterval.set( Math.max( 1, dataSetLoggingInterval.get() * 2 ) );
			else if ( currentTimeMillis - lastLogMillis.get() > 6000 )
				dataSetLoggingInterval.set( Math.max( 1, dataSetLoggingInterval.get() / 2  ) );
			lastLogMillis.set( currentTimeMillis );

			// Return log message
			return "Initialising (" + currentDatasetIndex + "/" + numTotal + "): ";
		}
		else
		{
			return null;
		}
	}

    public static List< SourceAndConverter< ? > > getVisibleSacs( BdvHandle bdv )
    {
        final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getBdvDisplayService();

        final List< SourceAndConverter< ? > > sacs = displayService.getSourceAndConverterOf( bdv );
        List< SourceAndConverter< ? > > visibleSacs = new ArrayList<>(  );
        for ( SourceAndConverter< ? > sac : sacs )
        {
            // TODO: this does not evaluate to true for all visible sources
            if ( displayService.isVisible( sac, bdv ) )
            {
                if ( sac.getSpimSource().getSource(0,0) != null ) // TODO improve this hack that allows to discard overlays source from screenshot
                    visibleSacs.add( sac );
            }
        }

        return visibleSacs;
    }

	public static List< SourceAndConverter< ? > > getSacs( BdvHandle bdv )
	{
		final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getBdvDisplayService();
		return displayService.getSourceAndConverterOf( bdv );
	}

    public static VoxelDimensions getPixelDimensions()
    {
        return new FinalVoxelDimensions( "pixel", 1.0, 1.0, 1.0 );
    }

	public static List< String > getFullPaths( String regex, String root )
	{
		if ( root != null )
			regex = new File( root, regex ).getAbsolutePath();

		try
		{
			// TODO: what is the correct path depth here?
			//   Note that is can become very slow for
			//   OME-Zarr with its many sub-folders if the depth is too deep
            return getPaths( regex, 2 );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static String toURI( File file )
	{
		String string = file.toString();

		if (string.startsWith("https:/") || string.startsWith("http:/"))
		{
			string = string
					.replaceFirst("https:/", "https://")
					.replaceFirst("http:/", "http://");
		}

		return string;
	}

	public static List< SourceAndConverter< ? > > getVisibleSacsInCurrentView( final BdvHandle bdvHandle )
	{
		final List< SourceAndConverter <?> > visibleSacs = getVisibleSacs( bdvHandle );

		List< SourceAndConverter< ? > > sacs = new ArrayList<>();
		for ( SourceAndConverter< ?  > sac : visibleSacs )
		{
			// TODO: can we determine from BDV whether a source is intersecting viewer plane?
			//       why do we need is2D=false ?
			if ( ! isSourceIntersectingCurrentView( bdvHandle, sac.getSpimSource(), false ) )
				continue;
			sacs.add( sac );
		}
		return sacs;
	}

	public static < R extends RealType< R > & NativeType< R > >
	RandomAccessibleInterval< R > copyVolumeRaiMultiThreaded( RandomAccessibleInterval< R > volume,
															  int numThreads )
	{
		final int dimensionX = ( int ) volume.dimension( 0 );
		final int dimensionY = ( int ) volume.dimension( 1 );
		final int dimensionZ = ( int ) volume.dimension( 2 );

		final long numElements =
				AbstractImg.numElements( Intervals.dimensionsAsLongArray( volume ) );

		RandomAccessibleInterval< R > copy;

		if ( numElements < Integer.MAX_VALUE - 1 )
		{
			copy = new ArrayImgFactory( Util.getTypeFromInterval( volume ) ).create( volume );
		}
		else
		{
			int cellSizeZ = (int) ( ( Integer.MAX_VALUE - 1 )
					/ ( volume.dimension( 0  ) * volume.dimension( 1 ) ) );

			final int[] cellSize = {
					dimensionX,
					dimensionY,
					cellSizeZ };

			copy = new CellImgFactory( Util.getTypeFromInterval( volume ), cellSize ).create( volume );
		}

		final int[] blockSize = {
				dimensionX,
				dimensionY,
				( int ) Math.ceil( 1.0 * dimensionZ / numThreads ) };

		Grids.collectAllContainedIntervals(
						Intervals.dimensionsAsLongArray( volume ) , blockSize )
				.parallelStream().forEach(
						interval -> copy( volume, Views.interval( copy, interval )));

		return copy;
	}

	private static < T extends Type< T > > void copy( final RandomAccessible< T > source,
													  final IterableInterval< T > target )
	{
		// create a cursor that automatically localizes itself on every move
		Cursor< T > targetCursor = target.localizingCursor();
		RandomAccess< T > sourceRandomAccess = source.randomAccess();

		// iterate over the input cursor
		while ( targetCursor.hasNext() )
		{
			// move input cursor forward
			targetCursor.fwd();

			// set the output cursor to the position of the input cursor
			sourceRandomAccess.setPosition( targetCursor );

			// set the value of this pixel of the output image, every Type supports T.set( T type )
			targetCursor.get().set( sourceRandomAccess.get() );
		}
	}

	public static double[] getCalibration( Source source, int level )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();

		source.getSourceTransform( 0, level, sourceTransform );

		final double[] calibration = getScale( sourceTransform );

		return calibration;
	}


	public static double[] getCurrentViewNormalVector( Bdv bdv )
	{
		AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdv.getBdvHandle().getViewerPanel().state().getViewerTransform( currentViewerTransform );

		final double[] viewerC = new double[]{ 0, 0, 0 };
		final double[] viewerX = new double[]{ 1, 0, 0 };
		final double[] viewerY = new double[]{ 0, 1, 0 };

		final double[] dataC = new double[ 3 ];
		final double[] dataX = new double[ 3 ];
		final double[] dataY = new double[ 3 ];

		final double[] dataV1 = new double[ 3 ];
		final double[] dataV2 = new double[ 3 ];
		final double[] currentNormalVector = new double[ 3 ];

		currentViewerTransform.inverse().apply( viewerC, dataC );
		currentViewerTransform.inverse().apply( viewerX, dataX );
		currentViewerTransform.inverse().apply( viewerY, dataY );

		LinAlgHelpers.subtract( dataX, dataC, dataV1 );
		LinAlgHelpers.subtract( dataY, dataC, dataV2 );

		LinAlgHelpers.cross( dataV1, dataV2, currentNormalVector );

		LinAlgHelpers.normalize( currentNormalVector );

		return currentNormalVector;
	}

	public static AffineTransform3D quaternionToAffineTransform3D( double[] rotationQuaternion )
	{
		double[][] rotationMatrix = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( rotationQuaternion, rotationMatrix );
		return matrixAsAffineTransform3D( rotationMatrix );
	}

	public static AffineTransform3D matrixAsAffineTransform3D( double[][] rotationMatrix )
	{
		final AffineTransform3D rotation = new AffineTransform3D();
		for ( int row = 0; row < 3; ++row )
			for ( int col = 0; col < 3; ++ col)
				rotation.set( rotationMatrix[ row ][ col ], row, col);
		return rotation;
	}

	public static double[] getBdvWindowCenter( Bdv bdv )
	{
		final double[] centre = new double[ 3 ];

		centre[ 0 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		centre[ 1 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		return centre;
	}

	public static double[] getScale( AffineTransform3D sourceTransform )
	{
		// https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati

		final double[] calibration = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			final double[] vector = new double[ 3 ];
			for ( int i = 0; i < 3 ; i++ )
			{
				vector[ i ] = sourceTransform.get( d, i );
			}

			calibration[ d ] = LinAlgHelpers.length( vector );
		}
		return calibration;
	}

	public static ArrayList< double[] > getVoxelSpacings( Source< ? > source )
	{
		final ArrayList< double[] > voxelSpacings = new ArrayList<>();
		final int numMipmapLevels = source.getNumMipmapLevels();
		for ( int level = 0; level < numMipmapLevels; ++level )
			voxelSpacings.add( getCalibration( source, level ) );

		return voxelSpacings;
	}

	public static boolean notNullOrEmpty( final String string )
	{
		return string != null && !string.isEmpty();
	}
}
