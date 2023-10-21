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
package org.embl.mobie.lib.files;

import automic.table.TableModel;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.table.saw.Aggregators;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.embl.mobie.io.util.IOHelper.getPaths;
import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class ImageFileSources
{
	protected final String name;
	protected Map< String, AffineTransform3D > nameToAffineTransform = new LinkedHashMap<>();
	protected Map< String, String > nameToFullPath = new LinkedHashMap<>();
	protected Map< String, String > nameToPath = new LinkedHashMap<>(); // for loading from tables

	protected GridType gridType;
	protected Table regionTable;
	protected Integer channelIndex;

	protected Metadata metadata;
	private String metadataSource;

	public ImageFileSources( String name, String pathRegex, Integer channelIndex, String root, GridType gridType )
	{
		this.gridType = gridType;
		this.name = name;
		this.channelIndex = channelIndex;

		List< String > paths = getFullPaths( pathRegex, root );

		for ( String path : paths )
		{
			final String fileName = new File( path ).getName();
			String imageName = createImageName( channelIndex, fileName );
			nameToFullPath.put( imageName, path );
		}

		// TODO: how to deal with the inconsistent metadata (e.g. number of timepoints)?
		this.metadataSource = nameToFullPath.keySet().iterator().next();
		this.metadata = MoBIEHelper.getMetadataFromImageFile( nameToFullPath.get( metadataSource ), channelIndex );

		createRegionTable();
	}

	public ImageFileSources( String name, Table table, String pathColumn, Integer channelIndex, String root, GridType gridType )
	{
		this.name = name;
		this.channelIndex = channelIndex;
		this.gridType = gridType;

		final StringColumn paths = table.stringColumn( pathColumn );

		// for joining
		nameToPath = new LinkedHashMap<>();

		for ( String path : paths )
		{
			File file = root == null ? new File( path ) : new File( root, path );
			String imageName = createImageName( channelIndex, file.getName() );
			nameToFullPath.put( imageName, file.getAbsolutePath() );
			nameToPath.put( imageName, path );
		}

		metadataSource = nameToFullPath.keySet().iterator().next();
		metadata = MoBIEHelper.getMetadataFromImageFile( nameToFullPath.get( metadataSource ), channelIndex );

		final List< String > columnNames = table.columnNames();
		dealWithObjectTableIfNeeded( name, table, pathColumn, columnNames );

		createRegionTable();

		// add column for joining on
		regionTable.addColumns( StringColumn.create( pathColumn, new ArrayList<>( nameToPath.values() )  ) );

		// add table columns to region table
		final List< Column< ? > > columns = table.columns();
		final int numColumns = columns.size();
		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			final Column< ? > column = columns.get( columnIndex );

			if ( column instanceof NumberColumn )
			{
				final Table summary = table.summarize( column, mean ).by( pathColumn );
				regionTable = regionTable.joinOn( pathColumn ).leftOuter( summary );
			}

			if ( column instanceof StringColumn )
			{
				final Table summary = table.summarize( column, Aggregators.firstString ).by( pathColumn );
				regionTable = regionTable.joinOn( pathColumn ).leftOuter( summary );
			}
		}
	}

	// For creating image sources from an AutoMicTools table
	public ImageFileSources( String name, TableModel tableModel, String imageTag, Integer channelIndex, GridType gridType )
	{
		this.name = name;
		this.gridType = gridType;
		this.channelIndex = channelIndex;

		int rowCount = tableModel.getRowCount();
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
		{
			try
			{
				String fileName = tableModel.getFileName( rowIndex, imageTag, "IMG" );
				String path = tableModel.getFileAbsolutePathString( rowIndex, imageTag, "IMG" );
				String imageName = createImageName( channelIndex, fileName );
				nameToFullPath.put( imageName, path );
				// FIXME: Construct the affine transform from the table
				nameToAffineTransform.put( imageName, new AffineTransform3D() );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				throw new RuntimeException( e );
			}
		}

		this.metadataSource = nameToFullPath.keySet().iterator().next();
		this.metadata = MoBIEHelper.getMetadataFromImageFile( nameToFullPath.get( metadataSource ), channelIndex );

		createRegionTable();

		// add column for joining on // TODO
//		regionTable.addColumns( StringColumn.create( pathColumn, new ArrayList<>( nameToPath.values() )  ) );
		// add table columns to region table
//		final List< Column< ? > > columns = table.columns();
//		final int numColumns = columns.size();
//		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
//		{
//			final Column< ? > column = columns.get( columnIndex );
//
//			if ( column instanceof NumberColumn )
//			{
//				final Table summary = table.summarize( column, mean ).by( pathColumn );
//				regionTable = regionTable.joinOn( pathColumn ).leftOuter( summary );
//			}
//
//			if ( column instanceof StringColumn )
//			{
//				final Table summary = table.summarize( column, Aggregators.firstString ).by( pathColumn );
//				regionTable = regionTable.joinOn( pathColumn ).leftOuter( summary );
//			}
//		}
	}

	protected static List< String > getFullPaths( String regex, String root )
	{
		if ( root != null )
			regex = new File( root, regex ).getAbsolutePath();

		List< String > paths = getPaths( regex, 999 );

		return paths;
	}

	private void dealWithObjectTableIfNeeded( String name, Table table, String pathColumn, List< String > columnNames )
	{
		final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( columnNames );

		if ( segmentColumnNames != null && table.containsColumn( segmentColumnNames.timePointColumn()) )
		{
			// it can be that not all sources have the same number of time points,
			// thus we find out here which one has the most
			final NumberColumn timepointColumn = ( NumberColumn ) table.column( segmentColumnNames.timePointColumn() );
			final double min = timepointColumn.min();
			final double max = timepointColumn.max();
			metadata.numTimePoints = ( int ) ( max - min + 1 );
			IJ.log("Detected " + metadata.numTimePoints  + " timepoints for " + name );

			final Table where = table.where( timepointColumn.isEqualTo( max ) );
			final String path = where.stringColumn( pathColumn ).get( 0 );
			metadataSource = nameToPath.entrySet().stream().filter( e -> e.getValue().equals( path ) ).findFirst().get().getKey();
		}
	}

	private String createImageName( Integer channelIndex, String fileName )
	{
		String imageName = FilenameUtils.removeExtension( fileName );

		if ( channelIndex != null )
		{
			imageName += "_c" + channelIndex;
		}

		return imageName;
	}

	private void createRegionTable()
	{
		regionTable = Table.create( name );
		final List< String > regions = new ArrayList<>( nameToFullPath.keySet() );
		regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );
		final List< String > paths = new ArrayList<>( nameToFullPath.values() );
		regionTable.addColumns( StringColumn.create( "source_path", paths ) );
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
		//   for multifile sources we could use:
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


//			// TODO also determine the grid position
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
