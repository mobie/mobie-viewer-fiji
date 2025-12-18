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

import bdv.AbstractSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import ij.IJ;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.realtransform.*;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
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
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.io.ImageDataInfo;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.InterpolatedAffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.source.Masked;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.transform.InterpolatedAffineRealTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
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
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static org.embl.mobie.io.util.IOHelper.combinePath;
import static org.embl.mobie.io.util.IOHelper.getPaths;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.isSourceIntersectingCurrentView;


public abstract class MoBIEHelper
{
	public static <T> Set<T> findDuplicates(Collection<T> collection) {
		Set<T> seen = new HashSet<>();
		Set<T> duplicates = new HashSet<>();

		for (T element : collection) {
			if ( ! seen.add(element) )
			{
				duplicates.add(element);
			}
		}

		return duplicates;
	}

	public static FinalRealInterval expand( final RealInterval interval, final double border )
	{
		final int n = interval.numDimensions();
		final double[] min = new double[ n ];
		final double[] max = new double[ n ];
		interval.realMin( min );
		interval.realMax( max );
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] -= border;
			max[ d ] += border;
		}
		return new FinalRealInterval( min, max );
	}

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

		// The above will only remove the ".zarr"
		if ( uri.endsWith( ".ome" ) )
			uri = uri.replace( ".ome", "" );

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
		else if ( image instanceof AnnotatedLabelImage )
		{
			Image< ? extends IntegerType< ? > > labelImage = ( ( AnnotatedLabelImage< ? > ) image ).getLabelImage();
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

	public static List< SourceAndConverter< ? > > getVisibleImageSacs( BdvHandle bdv )
	{
		List< SourceAndConverter< ? > > visibleSacs = getVisibleSacs( bdv );

		// Remove RegionImages
		visibleSacs = visibleSacs.stream()
				.filter( sac -> ! ( DataStore.sourceToImage().get( sac ) instanceof RegionAnnotationImage ) )
				.collect( Collectors.toList() );

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

	public static double[] getBdvWindowCenter( Bdv bdv )
	{
		final double[] centre = new double[ 3 ];

		centre[ 0 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getWidth() / 2.0;
		centre[ 1 ] = bdv.getBdvHandle().getViewerPanel().getDisplay().getHeight() / 2.0;

		return centre;
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
		return string != null && ! string.isEmpty();
	}

	public static ArrayList< double[] > getVoxelSpacings( Source<?> source, int t )
	{
		ArrayList<double[]> voxelSpacings = new ArrayList<>();
		int numMipmapLevels = source.getNumMipmapLevels();

		for(int level = 0; level < numMipmapLevels; ++level)
			voxelSpacings.add( getVoxelSpacing(source, t, level));

		return voxelSpacings;
	}

	public static double[] getVoxelSpacing( Source< ? > source, int t, int level )
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, level, sourceTransform);
		return getScale( sourceTransform );
	}

	public static double[] getScale( AffineTransform3D affineTransform3D ) {

		double[] scales = new double[3];
		for(int d = 0; d < 3; ++d)
			scales[d] = Affine3DHelpers.extractScale( affineTransform3D, d );

		return scales;
	}

	public static AffineTransform3D getViewerTransformWithNewCenter( BdvHandle bdvHandle, double[] position )
	{
		if ( position.length == 2 )
		{
			position = new double[]{
					position[ 0 ],
					position[ 1 ],
					0
			};
		}

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

		// ViewerTransform notes:
		// - applyInverse: coordinates in viewer => coordinates in image
		// - apply: coordinates in image => coordinates in viewer

		final double[] targetPositionInViewerInPixels = new double[ 3 ];
		currentViewerTransform.apply( position, targetPositionInViewerInPixels );

		for ( int d = 0; d < 3; d++ )
		{
			targetPositionInViewerInPixels[ d ] *= -1;
		}

		adaptedViewerTransform.translate( targetPositionInViewerInPixels );

		final double[] windowCentreInViewerInPixels = getWindowCentreInPixelUnits( bdvHandle.getViewerPanel() );

		adaptedViewerTransform.translate( windowCentreInViewerInPixels );

		return adaptedViewerTransform;
	}

	public static double[] getWindowCentreInPixelUnits( ViewerPanel viewerPanel )
	{
		final double[] windowCentreInPixelUnits = new double[ 3 ];
		windowCentreInPixelUnits[ 0 ] = viewerPanel.getDisplay().getWidth() / 2.0;
		windowCentreInPixelUnits[ 1 ] = viewerPanel.getDisplay().getHeight() / 2.0;
		return windowCentreInPixelUnits;
	}

	public static double[] getWindowCentreInCalibratedUnits( BdvHandle bdvHandle )
	{
		final double[] centreInPixelUnits = getWindowCentreInPixelUnits( bdvHandle.getViewerPanel() );
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( affineTransform3D );
		final double[] centreInCalibratedUnits = new double[ 3 ];
		affineTransform3D.inverse().apply( centreInPixelUnits, centreInCalibratedUnits );
		return centreInCalibratedUnits;
	}

	public static int getLevel( Source< ? > source, int currentTimePoint, long maxNumVoxels )
	{
		final ArrayList< double[] > voxelSpacings = getVoxelSpacings( source, currentTimePoint );

		final int numLevels = voxelSpacings.size();

		int level;

		for ( level = 0; level < numLevels; level++ )
		{
			final long numElements = Intervals.numElements( source.getSource( 0, level ) );

			if ( numElements <= maxNumVoxels )
				break;
		}

		if ( level == numLevels ) level = numLevels - 1;

		return level;
	}

	public static int getLevel( Source< ? > source, int currentTimePoint, double[] requestedVoxelSpacing )
	{
		ArrayList< double[] > voxelSpacings = getVoxelSpacings( source, currentTimePoint );
		return getLevel( voxelSpacings, requestedVoxelSpacing );
	}

	public static int getLevel( ArrayList< double[] > sourceVoxelSpacings, double[] requestedVoxelSpacing )
	{
		int level;
		int numLevels = sourceVoxelSpacings.size();
		final int numDimensions = sourceVoxelSpacings.get( 0 ).length;

		for ( level = 0; level < numLevels; level++ )
		{
			boolean allLargerOrEqual = true;

			for ( int d = 0; d < numDimensions; d++ )
			{
                if ( sourceVoxelSpacings.get( level )[ d ] < requestedVoxelSpacing[ d ] )
                {
                    allLargerOrEqual = false;
                    break;
                }
			}

			if ( allLargerOrEqual ) break;
		}

		if ( level == numLevels ) level = numLevels - 1;

		return level;
	}

	public static RealInterval createMask( List< ? extends Source< ? > > sources, int t )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealInterval bounds = SourceHelper.getMask( source, t );

			if ( union == null )
				union = bounds;
			else
				union = Intervals.union( bounds, union );
		}

		return union;
	}

	public static double[] getCenter( Image< ? > image, int t )
	{
		final RealInterval bounds = image.getMask();
        return getCenter( bounds );
	}

	public static double[] getCenter( RealInterval interval )
	{
		final double[] center = interval.minAsDoubleArray();
		final double[] max = interval.maxAsDoubleArray();
		for ( int d = 0; d < max.length; d++ )
		{
			center[ d ] = ( center[ d ] + max[ d ] ) / 2;
		}
		return center;
	}

	public static double[] getCenter( SourceAndConverter< ? > sourceAndConverter )
	{
		// TODO: Add documentation for why the mask is used and
		final RealInterval bounds = SourceHelper.getMask( sourceAndConverter.getSpimSource(), 0 );
		final double[] center = getCenter( bounds );
		return center;
	}

	public static AffineTransform3D createTranslationTransform(
			final Image< ? > image,
			final boolean centerAtOrigin,
			final double[] translation )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getCenter( image, 0 );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		// System.out.println( "Image: " + image.getName() );
		// System.out.println( "Translation: " + translationX + ", " + translationY );
		translationTransform.translate( translation );
		return translationTransform;
	}

	public static AffineTransform3D createTranslationTransform( double translationX, double translationY, SourceAndConverter< ? > sourceAndConverter, boolean centerAtOrigin )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getCenter( sourceAndConverter );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		translationTransform.translate( translationX, translationY, 0 );
		return translationTransform;
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel )
	{
		return createNormalisedViewerTransform( viewerPanel, getWindowCentreInPixelUnits( viewerPanel ) );
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel, double[] position )
	{
		final AffineTransform3D view = new AffineTransform3D();
		viewerPanel.state().getViewerTransform( view );

		// translate position to upper left corner of the Window (0,0)
		final AffineTransform3D translate = new AffineTransform3D();
		translate.translate( position );
		view.preConcatenate( translate.inverse() );

		// divide by window width
		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();;
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		view.preConcatenate( scale );

		return view;
	}

	public static AffineTransform3D createUnnormalizedViewerTransform( AffineTransform3D normalisedTransform, ViewerPanel viewerPanel )
	{
		final AffineTransform3D transform = normalisedTransform.copy();

		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		transform.preConcatenate( scale.inverse() );

		AffineTransform3D translate = new AffineTransform3D();
		translate.translate( getWindowCentreInPixelUnits( viewerPanel ) );

		transform.preConcatenate( translate );

		return transform;
	}

	public static AffineTransform3D getScatterPlotViewerTransform( BdvHandle bdv, double[] min, double[] max, double aspectRatio, boolean invertY, double zoom )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
		{
			final double center = ( min[ d ] + max[ d ] ) / 2.0;
			centerPosition[ d ] = - center;
		}
		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = getWindowDimensions( bdv );

		final int windowMinSize = Math.min( bdvWindowDimensions[ 0 ], bdvWindowDimensions[ 1 ] );
		final double[] scales = new double[ 2 ];
		scales[ 0 ] = zoom * windowMinSize / (max[ 0 ] - min[ 0 ]);
		scales[ 1 ] = scales[ 0 ] / aspectRatio;

		scales[ 1 ] = invertY ? -scales[ 1 ] : scales[ 1 ];
		affineTransform3D.scale( scales[ 0 ], scales[ 1 ], 1.0 );

		double[] shiftToBdvWindowCenter = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	public static AffineTransform3D getIntervalViewerTransform( BdvHandle bdv, RealInterval interval  )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}
		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = getWindowDimensions( bdv );

		double scale = Double.MAX_VALUE;
		for ( int d = 0; d < 2; d++ )
		{
			final double size = interval.realMax( d ) - interval.realMin( d );
			scale = Math.min( scale, 1.0 * ( bdvWindowDimensions[ d ] - 40 ) / size );
		}
		affineTransform3D.scale( scale );

		double[] shiftToBdvWindowCenter = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}
		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	private static int[] getWindowDimensions( BdvHandle bdv )
	{
		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();
		return bdvWindowDimensions;
	}

	public static AffineTransform3D asAffineTransform3D( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	public static String createNormalisedViewerTransformString( BdvHandle bdv, double[] position )
	{
		final AffineTransform3D view = createNormalisedViewerTransform( bdv.getViewerPanel(), position );
		final String replace = view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
		final String collect = Arrays.stream( replace.split( "," ) ).map( x -> "n" + x.trim() ).collect( Collectors.joining( "," ) );
		return collect;
	}

	// The evaluation of the resulting masks is slower than in
	// create createUnionBox, but it takes rotations into account.
	// FIXME: This currently does not really work, because in {@code TableSawAnnotatedRegion}
	//   the dilation of the mask will create a rectangular shape
	//   see "if ( relativeDilation > 0 )"
	public static RealMaskRealInterval union( Collection< ? extends Masked > maskedCollection )
	{
		if ( maskedCollection.isEmpty() )
			throw new RuntimeException("Cannot create union of empty list of masks.");

		RealMaskRealInterval union = null;

		for ( Masked masked : maskedCollection )
		{
			final RealMaskRealInterval mask = masked.getMask();

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.contains( union, mask ) )
					continue;

				union = union.or( mask );
			}
		}

		return union;
	}

	public static RealMaskRealInterval unionBox( Collection< ? extends Masked > maskedCollection )
	{
		if ( maskedCollection.isEmpty() )
			throw new RuntimeException("Cannot create union of empty list of masks.");

		RealInterval union = null;

		for ( Masked masked : maskedCollection )
		{
			final RealMaskRealInterval mask = masked.getMask();

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.contains( union, mask ) )
					continue;

				union = Intervals.union( mask, union );
			}
		}

		// convert to a box
		final double[] min = union.minAsDoubleArray();
		final double[] max = union.maxAsDoubleArray();
		return GeomMasks.closedBox( min, max );
	}

	@Nullable
	public static double[] getRealDimensions( RealMaskRealInterval unionMask )
	{
		final int numDimensions = unionMask.numDimensions();
		final double[] realDimensions = new double[ numDimensions ];
		final double[] min = unionMask.minAsDoubleArray();
		final double[] max = unionMask.maxAsDoubleArray();
		for ( int d = 0; d < numDimensions; d++ )
			realDimensions[ d ] = max[ d ] - min [ d ];
		return realDimensions;
	}

	public static String maskToString( RealMaskRealInterval mask )
	{
		return Arrays.toString( mask.minAsDoubleArray() ) + " - " + Arrays.toString( mask.maxAsDoubleArray() );
	}

	@NotNull
	public static AffineGet getEnlargementTransform( RealMaskRealInterval realMaskRealInterval, double scale )
	{
		int numDimensions = realMaskRealInterval.numDimensions();

		if ( numDimensions == 2 )
		{
			AffineTransform2D transform2D = new AffineTransform2D();
			double[] center = getCenter( realMaskRealInterval );
			transform2D.translate( Arrays.stream( center ).map( x -> -x ).toArray() );
			transform2D.scale( 1.0 / scale );
			transform2D.translate( center );
			return transform2D;
		}
		else if ( numDimensions == 3 )
		{
			AffineTransform3D transform3D = new AffineTransform3D();
			double[] center = getCenter( realMaskRealInterval );
			transform3D.translate( Arrays.stream( center ).map( x -> -x ).toArray() );
			transform3D.scale( 1.0 / scale );
			transform3D.translate( center );
			return transform3D;
		}
		else
		{
			throw new RuntimeException( "Unsupported number of dimensions " + numDimensions + ".");
		}
	}

	public static ArrayList< Transformation > fetchAllImageTransformations( Image< ? > image )
	{
		ArrayList< Transformation > transformations = new ArrayList<>();
		collectTransformations( image, transformations );
		Collections.reverse( transformations ); // first transformation first
		return transformations;
	}

	private static void collectTransformations( Image< ? > image, Collection< Transformation > transformations )
	{
		if ( image instanceof TransformedImage )
		{
			TransformedImage transformedImage = ( TransformedImage ) image;
			transformations.add( transformedImage.getTransformation() );
			collectTransformations( transformedImage.getWrappedImage(), transformations );
		}
		else if ( image instanceof AnnotatedLabelImage )
		{
			Image< ? extends IntegerType< ? > > labelImage = ( ( AnnotatedLabelImage< ? > ) image ).getLabelImage();
			collectTransformations( labelImage, transformations );
		}
		else
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			image.getSourcePair().getSource().getSourceTransform( 0, 0, affineTransform3D  );
			AffineTransformation affineTransformation = new AffineTransformation(
					"original image transform",
					affineTransform3D,
					Collections.singletonList( image.getName() ) );
			transformations.add( affineTransformation );
		}
	}

	private static void collectTransformations( Source< ? > source, Collection< Transformation > transformations )
	{
		if ( source instanceof AbstractSpimSource )
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			source.getSourceTransform( 0, 0, affineTransform3D );
			AffineTransformation affineTransformation = new AffineTransformation(
					"Input transformation", // FIXME: Those are not the names in the JSON
					affineTransform3D,
					Collections.singletonList( source.getName() ) );
			transformations.add( affineTransformation );
		}
		else if ( source instanceof TransformedSource )
		{
			TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) source;
			final Source< ? > wrappedSource = transformedSource.getWrappedSource();
			AffineTransform3D fixedTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( fixedTransform );
			// FIXME How to get the names?
			//  We could extend TransformedSource and add a field for the name of the transformation
			if ( ! fixedTransform.isIdentity() )
			{
				AffineTransformation affineTransformation = new AffineTransformation(
						"Additional transformation", // FIXME: Those are not the names in the JSON
						fixedTransform,
						Collections.singletonList( wrappedSource.getName() ) );
				transformations.add( affineTransformation );
			}
			collectTransformations( wrappedSource, transformations );
		}
		else if ( source instanceof RealTransformedSource )
		{
			RealTransformedSource< ? > realTransformedSource = ( RealTransformedSource< ? > ) source;
			RealTransform realTransform = realTransformedSource.getRealTransform();
			if ( realTransform instanceof InterpolatedAffineRealTransform )
			{
				Source< ? > wrappedSource = realTransformedSource.getWrappedSource();
				InterpolatedAffineRealTransform interpolatedAffineRealTransform = ( InterpolatedAffineRealTransform ) realTransform;
				InterpolatedAffineTransformation interpolatedAffineTransformation =
						new InterpolatedAffineTransformation(
								interpolatedAffineRealTransform.getName(),
								interpolatedAffineRealTransform.getTransforms(),
								wrappedSource.getName(),
								source.getName()
						);
				transformations.add( interpolatedAffineTransformation );
				collectTransformations( wrappedSource, transformations );
			}
			else
			{
				IJ.log( "Fetching transformations from " + source.getClass().getName() + " is not implemented." );
			}
		}
		else
		{
			IJ.log("Fetching transformations from " + source.getClass().getName() + " is not implemented.");
		}
	}

	public static ArrayList< Transformation > fetchAddedImageTransformations( Image< ? > image )
	{
		ArrayList< Transformation > allTransformations = fetchAllImageTransformations( image );
		allTransformations.remove( 0 ); // in MoBIE this is part of the raw image itself
		return allTransformations;
	}

	// Wrap the a sourcePair into new TransformedSources,
	// because otherwise, if the incremental transformations of the input TransformedSources
	// are changed, e.g. by the current logic of how the ManualTransformEditor works,
	// this creates a mess.
	public static < T > SourcePair< T > wrapTransformSourceAroundSourcePair( SourcePair< T > sourcePair )
	{
		TransformedSource< T > inputTransformedSource = ( TransformedSource< T > ) sourcePair.getSource();
		Source< T > inputSource = inputTransformedSource.getWrappedSource();
		TransformedSource< T > wrappedTransformedSource = new TransformedSource<>( inputSource, inputSource.getName() );
		AffineTransform3D transform3D = new AffineTransform3D();
		inputTransformedSource.getFixedTransform( transform3D );
		wrappedTransformedSource.setFixedTransform( transform3D );

		if ( sourcePair.getVolatileSource() == null )
		{
			return new DefaultSourcePair<>( wrappedTransformedSource, null );
		}

		Source< ? extends Volatile< T > > inputVolatileSource = ( ( TransformedSource< ? extends Volatile< T > > ) sourcePair.getVolatileSource() ).getWrappedSource();
		TransformedSource wrappedTransformedVolatileSource = new TransformedSource<>( inputVolatileSource, wrappedTransformedSource );
		return new DefaultSourcePair<>( wrappedTransformedSource, wrappedTransformedVolatileSource );
	}

	public static AffineTransform3D rotateAroundGlobalBdvWindowCenter( AffineTransform3D rotation, final BdvHandle bdv )
	{
		double[] centre = BdvHandleHelper.getWindowCentreInCalibratedUnits( bdv );
		final AffineTransform3D translateCenterToOrigin = new AffineTransform3D();
		translateCenterToOrigin.translate( DoubleStream.of( centre ).map( x -> -x ).toArray() );

		final AffineTransform3D translateOriginToCenter = new AffineTransform3D();
		translateOriginToCenter.translate( centre );

		return translateCenterToOrigin.copy()
				.preConcatenate( rotation )
				.preConcatenate( translateOriginToCenter );
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

	@NotNull
	public static AffineTransform3D getCurrentViewerRotation( final BdvHandle bdvHandle )
	{
		AffineTransform3D viewerTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
		double[] qCurrentRotation = new double[ 4 ];
		Affine3DHelpers.extractRotation( viewerTransform, qCurrentRotation );
		final AffineTransform3D currentRotation = quaternionToAffineTransform3D( qCurrentRotation );
		return currentRotation;
	}

	public static Corners getBdvWindowGlobalCorners( BdvHandle bdvHandle )
	{
		int width = bdvHandle.getViewerPanel().getWidth();
		int height = bdvHandle.getViewerPanel().getHeight();

		AffineTransform3D viewerToGlobal = bdvHandle.getViewerPanel().state().getViewerTransform().inverse();

		Corners corners = new Corners();
		viewerToGlobal.apply( new double[]{0,0,0}, corners.upperLeft );
		viewerToGlobal.apply( new double[]{width,0,0}, corners.upperRight );
		viewerToGlobal.apply( new double[]{0,height,0}, corners.lowerLeft );
		viewerToGlobal.apply( new double[]{width,height,0}, corners.lowerRight );

		return corners;
	}

	public static double[] getSize( final RealInterval interval )
	{
		int n = interval.numDimensions();
		double[] size = new double[ n ];
		for ( int d = 0; d < n; d++ )
		{
			size[ d ] = interval.realMax( d ) - interval.realMin( d );
		}
		return size;
	}

	public static SpotLabelImage getSpotLabelImage( SourceAndConverter sourceAndConverter )
	{
		Image< ? > image = DataStore.sourceToImage().get( sourceAndConverter );
		return unwrapImage( image, SpotLabelImage.class );
	}

	public static < T > T unwrapImage( Image< ? > image, Class< T > clazz )
	{
		if ( image == null )
			return null;

		if ( clazz.isInstance( image ) )
		{
			return ( T ) image;
		}
		else if ( image instanceof ImageWrapper )
		{
			Image< ? > wrappedImage = ( ( ImageWrapper ) image ).getWrappedImage();
			return unwrapImage( wrappedImage, clazz );
		}
		else
		{
			return null;
		}
	}

	public static @NotNull ArrayList< Type > getTypes( List< SourceAndConverter< ? > > sacs )
	{
		ArrayList< Type > types = new ArrayList<>();
		for ( SourceAndConverter< ? > sac : sacs )
		{
			Image< ? > image = DataStore.sourceToImage().get( sac );

			if ( image instanceof AnnotatedLabelImage )
			{
				RandomAccessibleInterval< ? extends IntegerType< ? > > source = ( ( AnnotatedLabelImage< ? > ) image ).getLabelImage().getSourcePair().getSource().getSource( 0, 0 );
				types.add( Util.getTypeFromInterval( source ) );
			}
			else
			{
				types.add( ( Type ) Util.getTypeFromInterval( sac.getSpimSource().getSource( 0, 0 ) ) );
			}
		}
		return types;
	}
}
