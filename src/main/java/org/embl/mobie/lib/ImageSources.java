package org.embl.mobie.lib;

import ij.IJ;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.io.IOHelper;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class ImageSources
{
	protected final String name;
	protected Map< String, String > nameToFullPath = new LinkedHashMap<>();
	protected Map< String, String > nameToPath = new LinkedHashMap<>(); // TODO: can we get rid of this?
	protected GridType gridType;
	protected Table regionTable;
	protected int channel = 0;
	protected int numTimePoints = 1;
	private String metadataSource;
	// TODO: load the display settings here?!

	public ImageSources( String name, Table table, String pathColumn, String root, GridType gridType )
	{
		this.name = name;
		this.gridType = gridType;

		final StringColumn paths = table.stringColumn( pathColumn );
		for ( String path : paths )
		{
			addImage( root, path );

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

		}

		final List< String > columnNames = table.columnNames();
		final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( columnNames );
		if ( segmentColumnNames != null )
		{
			final NumberColumn timepointColumn = ( NumberColumn ) table.column( segmentColumnNames.timePointColumn() );
			final double min = timepointColumn.min();
			final double max = timepointColumn.max();
			numTimePoints = ( int ) ( max - min + 1 );
			IJ.log("Detected " + numTimePoints + " timepoints for " + name );

			final Table where = table.where( timepointColumn.isEqualTo( max ) );
			final String path = where.stringColumn( pathColumn ).get( 0 );
			metadataSource = nameToPath.entrySet().stream().filter( e -> e.getValue().equals( path ) ).findFirst().get().getKey();
		}

		createRegionTable();
	}

	private void addImage( String root, String path )
	{
		File file = root == null ? new File( path ) : new File( root, path );
		final String imageName = FilenameUtils.removeExtension( file.getName() );
		nameToFullPath.put( imageName, file.getAbsolutePath() );
		nameToPath.put( imageName, path );
	}

	public ImageSources( @Nullable String name, String imagePath, String root, GridType grid )
	{
		if ( name == null )
			this.name = FilenameUtils.removeExtension( new File( imagePath ).getName() );
		else
			this.name = name;

		String[] imagePaths;
		if ( imagePath.contains( "*" ) )
			imagePaths = IOHelper.getPaths( imagePath, 999 );
		else
			imagePaths = new String[]{ imagePath };

		for ( String path : imagePaths )
		{
			addImage( root, path );
		}

		// TODO: how to deal with the inconsistent number of timepoints?
		this.metadataSource = nameToFullPath.keySet().iterator().next();

	}

	private void createRegionTable( )
	{
		// create image table
		regionTable = Table.create( name );
		final List< String > regions = new ArrayList<>( nameToFullPath.keySet() );
		regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );
		final List< String > paths = new ArrayList<>( nameToFullPath.values() );
		regionTable.addColumns( StringColumn.create( "path", paths ) );
	}

	private void createRegionTable( Table table, String pathColumn )
	{
		// create image table
		// TODO add more columns
		final List< Column< ? > > columns = table.columns();
		final int numColumns = columns.size();
		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			if ( columns.get( columnIndex ) instanceof NumberColumn )
			{
				regionTable = table.summarize( columns.get( columnIndex ), mean ).by( pathColumn );
				break;
			}
		}

		final StringColumn regions = StringColumn.create( ColumnNames.REGION_ID, getSources() );
		regionTable.addColumns( regions );
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

	public int getChannel()
	{
		return channel;
	}

	public List< String > getSources()
	{
		return new ArrayList<>( nameToFullPath.keySet() ) ;
	}

	public String getPath( String source )
	{
		return nameToFullPath.get( source );
	}

	public int numTimePoints()
	{
		return numTimePoints;
	}

	public String getMetadataSource()
	{
		return metadataSource;
	}
}

