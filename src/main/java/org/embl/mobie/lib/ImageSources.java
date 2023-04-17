package org.embl.mobie.lib;

import ij.IJ;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.source.Metadata;
import org.embl.mobie.lib.io.IOHelper;
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

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class ImageSources
{
	protected final String name;
	protected Map< String, String > nameToFullPath = new LinkedHashMap<>();
	protected Map< String, String > nameToPath = new LinkedHashMap<>(); // for loading from tables
	protected GridType gridType;
	protected Table regionTable;
	protected Integer channelIndex = null;
	protected Metadata metadata = new Metadata();
	private String metadataSource;

	public ImageSources( String name, String regex, Integer channelIndex, String root, GridType gridType )
	{
		this.gridType = gridType;
		this.name = name;
		this.channelIndex = channelIndex;

		List< String > paths = getFullPaths( regex, root );

		for ( String path : paths )
		{
			final String fileName = new File( path ).getName();
			String imageName = createImageName( channelIndex, fileName );
			nameToFullPath.put( imageName, path );
		}

		// TODO: how to deal with the inconsistent metadata (e.g. number of timepoints)?
		this.metadataSource = nameToFullPath.keySet().iterator().next();
		this.metadata = MoBIEHelper.getMetadataFromImageFile( nameToFullPath.get( metadataSource ) );

		createRegionTable();
	}

	protected static List< String > getFullPaths( String regex, String root )
	{
		if ( root != null )
			regex = new File( root, regex ).getAbsolutePath();

		List< String > paths = IOHelper.getPaths( regex, 999 );
		return paths;
	}

	public ImageSources( String name, Table table, String pathColumn, Integer channelIndex, String root, GridType gridType )
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

		final List< String > columnNames = table.columnNames();
		final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( columnNames );
		if ( segmentColumnNames != null )
		{
			final NumberColumn timepointColumn = ( NumberColumn ) table.column( segmentColumnNames.timePointColumn() );
			final double min = timepointColumn.min();
			final double max = timepointColumn.max();
			metadata.numTimePoints = ( int ) ( max - min + 1 );
			IJ.log("Detected " + metadata.numTimePoints  + " timepoints for " + name );

			final Table where = table.where( timepointColumn.isEqualTo( max ) );
			final String path = where.stringColumn( pathColumn ).get( 0 );
			metadataSource = nameToPath.entrySet().stream().filter( e -> e.getValue().equals( path ) ).findFirst().get().getKey();
		}

		createRegionTable();

		// add column for joining on
		regionTable.addColumns( StringColumn.create( pathColumn, new ArrayList<>( nameToPath.values() )  ) );

		// add table columns to region table
		final List< Column< ? > > columns = table.columns();
		final int numColumns = columns.size();
		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			final Column< ? > column = columns.get( columnIndex );

			if ( regionTable.containsColumn( column.name() ) )
			{
				continue;
			}

			if( column.size() == regionTable.rowCount() )
			{
				// it is an image table, thus we can just append the columns.
				// note: this check could be done just once outside the loop
				// because all columns have the same size, but
				// it also works here.
				regionTable.addColumns( column );
				continue;
			}

			// it is an object table, thus we need to summarise
			// the columns such that we have only one row per image

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
		return nameToFullPath.get( source );
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