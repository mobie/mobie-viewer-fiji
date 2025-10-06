/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.data;

import bdv.viewer.Source;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.util.Constants;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.*;

public class ImageGridSources
{
	protected final String name;
	protected Map< String, AffineTransform3D > nameToAffineTransform = new LinkedHashMap<>();
	protected Map< String, String > nameToFullPath = new LinkedHashMap<>();
	protected Map< String, String > nameToPath = new LinkedHashMap<>();

	protected GridType gridType;
	protected Table regionTable;
	protected Integer channelIndex;

	protected Metadata metadata;
	private String metadataSource;

	public ImageGridSources( String name, String path, Integer channelIndex, String root, GridType gridType )
	{
		this.gridType = gridType;
		this.name = name;
		this.channelIndex = channelIndex;

		if ( path.contains( "http" ) || path.startsWith( "s3" ) )
		{
			nameToFullPath.put( name, path );
		}
		else
		{
			// path could be a regExp
			List< String > paths = MoBIEHelper.getFullPaths( path, root );

			if ( paths.size() == 1 ) // no regExp
			{
				nameToFullPath.put( name, path );
			}
			else
			{
				// regExp
				for ( String aPath : paths )
				{
					final String fileName = new File( aPath ).getName();
					String imageName = createImageName( channelIndex, fileName );
					nameToFullPath.put( imageName, aPath );
				}
			}
		}


		// TODO: how to deal with the inconsistent metadata (e.g. number of time-points)?
		// FIXME: We don't always want to prefetch this as this is expensive..
		setMetadata( channelIndex );

		// TODO: move this out to a separate function
		regionTable = Table.create( name + " table" );
		regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, new ArrayList<>( nameToFullPath.keySet() ) ) );
		regionTable.addColumns( StringColumn.create( "source_path", new ArrayList<>( nameToFullPath.values() ) ) );
	}

	public ImageGridSources( String name, Table table, String imageColumn, Integer channelIndex, String root, String pathMapping, GridType gridType )
	{
		this.name = name;
		this.channelIndex = channelIndex;
		this.gridType = gridType;

		nameToPath = new LinkedHashMap<>(); // needed for joining the tables below when creating the region table

		int numRows = table.rowCount();
		if ( imageColumn.contains( "_IMG" )  ) // Automic table
		{
			for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
			{
				String fileName = table.getString( rowIndex, imageColumn );
				String relativeFolderName = table.getString( rowIndex, imageColumn.replace( "FileName_", "PathName_"  ) );
				String path = MoBIEHelper.createAbsolutePath( root, fileName, relativeFolderName );
				String imageName = createImageName( channelIndex, fileName );
				nameToFullPath.put( imageName, applyPathMapping( pathMapping, path ) );
				nameToPath.put( imageName, fileName );

				if ( table.columnNames().contains( "Rotation_NUM" ) ) // TODO can we have this generic?
				{
					double rotation = table.doubleColumn( "Rotation_NUM" ).get( rowIndex );
					AffineTransform3D affineTransform3D = new AffineTransform3D();
					affineTransform3D.rotate( 2, rotation * Math.PI / 180 );
					nameToAffineTransform.put( imageName, affineTransform3D );
				}
			}
		}
		else if ( isCellProfilerColumn( imageColumn, table ) )
		{
			String postfix = imageColumn.substring("FileName_".length());
			String folderColumn = "PathName_" + postfix;

			for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
			{
				String fileName = table.getString( rowIndex, imageColumn );
				String folder = root != null ? root : table.getString( rowIndex, folderColumn );
				String path = IOHelper.combinePath( folder, fileName );
				String imageName = createImageName( channelIndex, fileName );
				nameToFullPath.put( imageName, applyPathMapping( pathMapping, path ) );
				nameToPath.put( imageName, fileName );
			}
		}
		else
		{
			// Default table
			for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
			{
				String path = table.getString( rowIndex, imageColumn );
				String imageName = createImageName( channelIndex, IOHelper.getFileName( path ) );
				if ( path.startsWith( "http" ) )
				{
					nameToFullPath.put( imageName, path );
					nameToPath.put( imageName, path );
				}
				else
				{
					File file = root == null ? new File( path ) : new File( root, path );
					nameToFullPath.put( imageName, applyPathMapping( pathMapping, file.getAbsolutePath() )  );
					nameToPath.put( imageName, path );
				}
			}
		}

		setMetadata( channelIndex );
		dealWithTimepointsInObjectTableIfNeeded( name, table, imageColumn );
	}

	protected static boolean isCellProfilerColumn( String column, Table table )
	{
		if ( ! column.startsWith( "FileName_" ) ) return false;

		String postfix = column.substring("FileName_".length());
		String folderColumn = "PathName_" + postfix;
		boolean containsFolderColumn = table.containsColumn( folderColumn );

		return containsFolderColumn;
	}

	private void setMetadata( Integer setupIndex )
	{
		// Take the first source of the grid to fetch metadata
		metadataSource = nameToFullPath.keySet().iterator().next();
		long start = System.currentTimeMillis();
		IJ.log( "Fetching metadata for \"" + name + "\", setup " + setupIndex );
		String uri = nameToFullPath.get( metadataSource );
		IJ.log( "Source: " + uri );
		ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( uri );
		String[] s3AccessAndSecretKey = MoBIE.getInstance().getSettings().values.getS3AccessAndSecretKey();
		imageDataFormat.setS3SecretAndAccessKey( s3AccessAndSecretKey );
		ImageData< ? > imageData = DataStore.fetchImageData(
				uri,
				imageDataFormat,
				ThreadHelper.sharedQueue );

		setupIndex = setupIndex == null ? 0 : setupIndex;
		CanonicalDatasetMetadata canonicalDatasetMetadata = imageData.getMetadata( setupIndex );
		metadata = new Metadata( canonicalDatasetMetadata );
		Source< ? > source = imageData.getSourcePair( setupIndex ).getA();
		RandomAccessibleInterval< ? > highResRAI = source.getSource( 0, 0 );
		metadata.numZSlices = (int) highResRAI.dimension( 2 );
		metadata.numTimePoints = SourceHelper.getNumTimePoints( source );
		IJ.log( "Slices: " + metadata.numZSlices );
		IJ.log( "Frames: " + metadata.numTimePoints );

		// determine contrast limits, if affordable
		RandomAccessibleInterval< ? > lowResRAI = source.getSource( 0, source.getNumMipmapLevels() - 1 );
		long numElements = Intervals.numElements( lowResRAI.dimensionsAsLongArray() );
		if ( numElements < 1024 * 1024 )
			metadata.contrastLimits = SourceHelper.estimateMinMax( ( RandomAccessibleInterval ) lowResRAI );

		IJ.log( "Contrast limits: " + Arrays.toString( metadata.contrastLimits ) );
		IJ.log( "Fetched metadata in " + ( System.currentTimeMillis() - start ) + " ms" );

	}

	private static String applyPathMapping( String pathMapping, String path )
	{
		if ( pathMapping != null && ! pathMapping.isEmpty() )
		{
			String[] fromTo = pathMapping.split( "," );
			String from = fromTo[ 0 ];
			String to = fromTo[ 1 ];
			path = path.replace( from, to );
		}

		return path;
	}

	private void dealWithTimepointsInObjectTableIfNeeded( String name, Table table, String pathColumn )
	{
		try
		{
			final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( table.columnNames() );

			if ( table.containsColumn( segmentColumnNames.timePointColumn() ) )
			{
				// it can be that not all sources have the same number of time points,
				// thus we find out here which one has the most
				final NumberColumn timepointColumn = ( NumberColumn ) table.column( segmentColumnNames.timePointColumn() );
				final double min = timepointColumn.min();
				final double max = timepointColumn.max();
				metadata.numTimePoints = ( int ) ( max - min + 1 );
				IJ.log( "Detected " + metadata.numTimePoints + " timepoint(s) for " + name );

				final Table where = table.where( timepointColumn.isEqualTo( max ) );
				final String path = where.stringColumn( pathColumn ).get( 0 );
				metadataSource = nameToPath.entrySet().stream().filter( e -> e.getValue().equals( path ) ).findFirst().get().getKey();
			}
		}
		catch ( Exception e )
		{
			// the table probably is an image table as TableDataFormat.getSegmentColumnNames( ) errors
		}
	}

	private String createImageName( Integer channelIndex, String fileName )
	{
		String imageName = MoBIEHelper.removeExtension( fileName );

		if ( channelIndex != null )
		{
			// TODO: it is a bit ugly to have that
			//   - if there is only one image
			//   - if the image is a segmentation
			imageName += Constants.CHANNEL_POSTFIX + channelIndex;
		}

		return imageName;
	}

	public GridType getGridType()
	{
		return gridType;
	}

	public String getName()
	{
		return name;
	}

	public Table getRegionTable()
	{
		return regionTable;
	}

	public int getChannelIndex()
	{
		return channelIndex == null ? 0 : channelIndex;
	}

	public List< String > getSources()
	{
		return new ArrayList<>( nameToFullPath.keySet() ) ;
	}

	public String getPath( String source )
	{
		// TODO:
		//   for multi-file sources we could use:
		//   private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();
		//   or we leave the group regex in the paths
		return nameToFullPath.get( source );
	}

	public AffineTransform3D getTransform( String source )
	{
		return nameToAffineTransform.get( source );
	}

	public String getMetadataSource()
	{
		return metadataSource;
	}

	public Metadata getMetadata()
	{
		return metadata;
	}


}

//	// TODO also determine the grid position
//   add a function for this? arrange grid by TableColumn?
//			final List< String > groupNames = MoBIEHelper.getGroupNames( regex );
//
//			if ( rowGroup != null )
//			{
//				final List< String > sources = channelToSources.values().iterator().next();
//				final HashSet< String > categorySet = new HashSet<>();
//				for ( String source : sources )
//				{
//					final Matcher matcher = pattern.matcher( source );
//					matcher.matches();
//					categorySet.add( matcher.group( rowGroup ) );
//				}
//
//				final ArrayList< String > categories = new ArrayList<>( categorySet );
//				final int[] numSources = new int[ categories.size() ];
//				grid.positions = new ArrayList<>();
//				for ( String source : sources )
//				{
//					final Matcher matcher = pattern.matcher( source );
//					matcher.matches();
//					final int row = categories.indexOf( matcher.group( rowGroup ) );
//					final int column = numSources[ row ];
//					numSources[ row ]++;
//					grid.positions.add( new int[]{ column, row } );
//				}
//			}


//	private void createRegionTable( Table table, String pathColumn )
//	{
//		// create image table
//		// TODO add more columns
//		final List< Column< ? > > columns = table.columns();
//		final int numColumns = columns.size();
//		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
//		{
//			if ( columns.get( columnIndex ) instanceof NumberColumn )
//			{
//				regionTable = table.summarize( columns.get( columnIndex ), mean ).by( pathColumn );
//				break;
//			}
//		}
//
//		final StringColumn regions = StringColumn.create( ColumnNames.REGION_ID, getSources() );
//		regionTable.addColumns( regions );
//	}
