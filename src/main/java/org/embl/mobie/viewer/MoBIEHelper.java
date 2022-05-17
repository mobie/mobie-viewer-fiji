package org.embl.mobie.viewer;

import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.util.Intervals;
import org.embl.mobie.io.util.FileAndUrlUtils;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.transform.MergedGridSource;
import org.embl.mobie.viewer.transform.TransformHelper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_Z;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_Z;
import static org.embl.mobie.viewer.ui.SwingHelper.selectionDialog;

public abstract class MoBIEHelper
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static int[] asInts( long[] longs) {
		int[] ints = new int[longs.length];

		for(int i = 0; i < longs.length; ++i)
		{
			ints[i] = (int)longs[i];
		}

		return ints;
	}

	public static long[] asLongs( int[] ints) {
		long[] longs = new long[ints.length];

		for(int i = 0; i < longs.length; ++i)
		{
			longs[i] = ints[i];
		}

		return longs;
	}

	/**
	 * Recursively fetch all root sources
	 * @param source
	 * @param rootSources
	 */
	public static void fetchRootSources( Source< ? > source, Set< Source< ? > > rootSources )
	{
		if ( source instanceof SpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof LabelSource )
		{
			final Source< ? > wrappedSource = (( LabelSource ) source).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof MergedGridSource )
		{
			final MergedGridSource< ? > mergedGridSource = ( MergedGridSource ) source;
			final List< ? extends Source< ? > > gridSources = mergedGridSource.getGridSources();
			for ( Source< ? > gridSource : gridSources )
			{
				fetchRootSources( gridSource, rootSources );
			}
		}
		else if (  source instanceof ResampledSource )
		{
			final ResampledSource resampledSource = ( ResampledSource ) source;
			final Source< ? > wrappedSource = resampledSource.getOriginalSource();
			fetchRootSources( wrappedSource, rootSources );
		}
		else
		{
			throw new IllegalArgumentException("For sources of type " + source.getClass().getName() + " the root source currently cannot be determined.");
		}
	}

	public static ImagePlus openWithBioFormats( String path, int seriesIndex )
	{
		try
		{
			ImporterOptions opts = new ImporterOptions();
			opts.setId( path );
			opts.setSeriesOn( seriesIndex, true );
			ImportProcess process = new ImportProcess( opts );
			process.execute();
			ImagePlusReader impReader = new ImagePlusReader( process );
			ImagePlus[] imps = impReader.openImagePlus();
			return imps[ 0 ];
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static RealMaskRealInterval unionRealMask( List< ? extends Source< ? > > sources )
	{
		RealMaskRealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealMaskRealInterval mask = getMask( source );

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.equals( mask, union ) )
				{
					continue;
				}
				else
				{
					union = union.or( mask );
				}
			}
		}

		return union;
	}

	public enum FileLocation {
		Project,
		FileSystem
	}

	private static String chooseCommonFileName( List<String> directories, String objectName ) {
		Map<String, Integer> fileNameCounts = new HashMap<>();
		ArrayList<String> commonFileNames = new ArrayList<>();

		for ( String directory: directories ) {
			String[] directoryFileNames = FileAndUrlUtils.getFileNames( directory );
			for ( String directoryFileName: directoryFileNames ) {
				if ( fileNameCounts.containsKey( directoryFileName ) ) {
					int count = fileNameCounts.get(directoryFileName);
					fileNameCounts.put( directoryFileName, count + 1 );
				} else {
					fileNameCounts.put( directoryFileName, 1 );
				}
			}
		}

		for ( String fileName: fileNameCounts.keySet() ) {
			if ( fileNameCounts.get( fileName ) == directories.size() ) {
				commonFileNames.add( fileName );
			}
		}

		if ( commonFileNames.size() > 0 ) {
			String[] choices = new String[commonFileNames.size()];
			for (int i = 0; i < choices.length; i++) {
				choices[i] = commonFileNames.get(i);
			}
			return selectionDialog(choices, objectName);
		} else {
			return null;
		}
	}

	public static FileLocation loadFromProjectOrFileSystemDialog() {
		final GenericDialog gd = new GenericDialog("Choose source");
		gd.addChoice("Load from", new String[]{FileLocation.Project.toString(), FileLocation.FileSystem.toString()}, FileLocation.Project.toString());
		gd.showDialog();
		if (gd.wasCanceled()) return null;
		return FileLocation.valueOf(gd.getNextChoice());
	}

	public static String selectCommonFileNameFromProject( List<String> directories, String objectName ) {
		if ( directories == null ) {
			return null;
		}

		String fileName;
		if ( directories.size() > 1) {
			// when there are multiple directories, we only allow selection of items that are present with the same name in
			// all of them
			fileName = chooseCommonFileName(directories, objectName);
		} else {
			String[] fileNames = FileAndUrlUtils.getFileNames( directories.get(0) );
			fileName = selectionDialog( fileNames, objectName );
		}

		return fileName;
	}

	public static String selectPathFromProject( String directory, String objectName ) {
		if ( directory == null ) {
			return null;
		}

		String[] fileNames = FileAndUrlUtils.getFileNames( directory );
		String fileName = selectionDialog( fileNames, objectName );
		if ( fileName != null ) {
			return FileAndUrlUtils.combinePath( directory, fileName );
		} else {
			return null;
		}
	}

	public static File lastSelectedDir;

	// objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
	public static String selectFilePath( String fileExtension, String objectName, boolean open ) {
		final JFileChooser jFileChooser = new JFileChooser( lastSelectedDir );
		if ( fileExtension != null ) {
			jFileChooser.setFileFilter(new FileNameExtensionFilter(fileExtension, fileExtension));
		}
		jFileChooser.setDialogTitle( "Select " + objectName );
		return selectPath( jFileChooser, open );
	}

	// objectName is used for the dialog labels e.g. 'table', 'bookmark' etc...
	public static String selectDirectoryPath( String objectName, boolean open ) {
		final JFileChooser jFileChooser = new JFileChooser( lastSelectedDir );
		jFileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		jFileChooser.setDialogTitle( "Select " + objectName );
		return selectPath( jFileChooser, open );
	}

	private static String selectPath( JFileChooser jFileChooser, boolean open ) {
		final AtomicBoolean isDone = new AtomicBoolean( false );
		final String[] path = new String[ 1 ];
		Runnable r = () -> {
			if ( open ) {
				path[0] = selectOpenPathFromFileSystem( jFileChooser);
			} else {
				path[0] = selectSavePathFromFileSystem( jFileChooser );
			}
			isDone.set( true );
		};

		SwingUtilities.invokeLater(r);

		while ( ! isDone.get() ){
			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e )
			{ e.printStackTrace(); }
		};
		return path[ 0 ];
	}

	private static void setLastSelectedDir( String filePath ) {
		File selectedFile = new File( filePath );
		if ( selectedFile.isDirectory() ) {
			lastSelectedDir = selectedFile;
		} else {
			lastSelectedDir = selectedFile.getParentFile();
		}
	}

	public static String selectOpenPathFromFileSystem( JFileChooser jFileChooser ) {
		String filePath = null;
		if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			filePath = jFileChooser.getSelectedFile().getAbsolutePath();
			setLastSelectedDir( filePath );
		}
		return filePath;
	}

	public static String selectSavePathFromFileSystem( JFileChooser jFileChooser )
	{
		String filePath = null;
		if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			filePath = jFileChooser.getSelectedFile().getAbsolutePath();
			setLastSelectedDir( filePath );
		}
		return filePath;
	}

	public static SourceAndConverter< ? > getSourceAndConverter( List< SourceAndConverter< ? > > sourceAndConverters, String name )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			if ( sourceAndConverter.getSpimSource().getName().equals( name ) )
				return sourceAndConverter;
		}

		return null;
	}

	public static double[] delimitedStringToDoubleArray( String s, String delimiter) {

		String[] sA = s.split(delimiter);
		double[] nums = new double[sA.length];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Double.parseDouble(sA[i].trim());
		}

		return nums;
	}

	public static void log( String text )
	{
		IJ.log( text );
	}

	public static List< TableRowImageSegment > createAnnotatedImageSegmentsFromTableFile(
			String tablePath,
			String imageId )
	{
		tablePath = resolveTablePath( tablePath );

		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );

		TableColumns.addLabelImageIdColumn(
				columns,
				TableColumnNames.LABEL_IMAGE_ID,
				imageId );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumn( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, segmentPropertyToColumn, false );

		return segments;
	}

	public static String resolveTablePath( String tablePath )
	{
		if ( tablePath.startsWith( "http" ) ) {
			tablePath = FileAndUrlUtils.resolveURL( URI.create( tablePath ) );
		} else {
			tablePath = FileAndUrlUtils.resolvePath( tablePath );
		}
		return tablePath;
	}

	public static Map< SegmentProperty, List< String > > createSegmentPropertyToColumn(
			Map< String, List< String > > columns )
	{
		final HashMap< SegmentProperty, List< String > > segmentPropertyToColumn
				= new HashMap<>();

		segmentPropertyToColumn.put(
				SegmentProperty.LabelImage,
				columns.get( TableColumnNames.LABEL_IMAGE_ID ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( TableColumnNames.SEGMENT_LABEL_ID ) );

		segmentPropertyToColumn.put(
				SegmentProperty.X,
				columns.get( TableColumnNames.ANCHOR_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Y,
				columns.get( TableColumnNames.ANCHOR_Y ) );

		if ( columns.containsKey( TableColumnNames.ANCHOR_Z ) )
			segmentPropertyToColumn.put(
					SegmentProperty.Z,
					columns.get( TableColumnNames.ANCHOR_Z ) );

		segmentPropertyToColumn.put(
				SegmentProperty.T,
				columns.get( TableColumnNames.TIMEPOINT ) );

		SegmentUtils.putDefaultBoundingBoxMapping( segmentPropertyToColumn, columns );

		if ( ! columns.containsKey( BB_MIN_Z )  )
			segmentPropertyToColumn.remove( SegmentProperty.BoundingBoxZMin );

		if ( ! columns.containsKey( BB_MAX_Z ) )
			segmentPropertyToColumn.remove( SegmentProperty.BoundingBoxZMax );

		return segmentPropertyToColumn;
	}

	public static String createNormalisedViewerTransformString( BdvHandle bdv, double[] position )
	{
		final AffineTransform3D view = TransformHelper.createNormalisedViewerTransform( bdv.getViewerPanel(), position );
		final String replace = view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
		final String collect = Arrays.stream( replace.split( "," ) ).map( x -> "n" + x.trim() ).collect( Collectors.joining( "," ) );
		return collect;
	}

	public static double[] getMousePosition( BdvHandle bdv )
	{
		final RealPoint realPoint = new RealPoint( 2 );
		bdv.getViewerPanel().getMouseCoordinates( realPoint );
		final double[] doubles = new double[ 3 ];
		realPoint.localize( doubles );
		return doubles;
	}

	public static AffineTransform3D asAffineTransform3D( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	public static RealMaskRealInterval getMask( Source< ? > source )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( 0, 0, affineTransform3D );
		final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );
		final double[] min = rai.minAsDoubleArray();
		final double[] max = rai.maxAsDoubleArray();
		final double[] voxelSizes = source.getVoxelDimensions().dimensionsAsDoubleArray();
		for ( int d = 0; d < 3; d++ )
		{
			min[ d ] -= voxelSizes[ d ];
			max[ d ] += voxelSizes[ d ];
		}
		final RealMaskRealInterval mask = GeomMasks.closedBox( min, max ).transform( affineTransform3D.inverse() );

		return mask;
	}

	public static String getName( String path )
	{
		if ( path.startsWith( "http" ) )
		{
			final String[] split = path.split( "/" );
			return split[ split.length - 1 ];
		}
		else
		{
			return new File( path ).getName();
		}
	}

	public static void toDoubleStrings( List< String > values )
	{
		final int size = values.size();
		for ( int i = 0; i < size; i++ )
		{
			values.set( i, String.valueOf( Double.parseDouble( values.get( i ) ) ) );
		}
	}

}
