package org.embl.mobie.lib;

import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.ColumnType;
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
	protected Map< String, String > nameToPath = new LinkedHashMap<>();
	protected Map< String, String > nameToTableCell = new LinkedHashMap<>();
	protected GridType gridType;
	protected Table imageTable;
	protected int channel = 0;

	public ImageSources( String name, Table table, String pathColumn, String root, GridType gridType )
	{
		this.name = name;
		this.gridType = gridType;

		createImageTable( table, pathColumn );

		final StringColumn paths = table.stringColumn( pathColumn );
		for ( String path : paths )
		{
			File file = root == null ? new File( path ) : new File( root, path );
			final String imageName = FilenameUtils.removeExtension( file.getName() );
			nameToPath.put( imageName, file.getAbsolutePath() );
			nameToTableCell.put( imageName, path );

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


	}

	private void createImageTable( Table table, String pathColumn )
	{
		// create image table
		// TODO add more columns here
		final List< ColumnType > types = table.types();
		final List< Column< ? > > columns = table.columns();
		final int numColumns = types.size();
		imageTable = null;
		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			if ( types.get( columnIndex ) instanceof NumberColumn )
			{
				imageTable = table.summarize( columns.get( columnIndex ), mean ).by( pathColumn );
				break;
			}
		}
	}

	public GridType getGridType()
	{
		return gridType;
	}

	public String getName()
	{
		return name;
	}

	public Table getImageTable()
	{
		return imageTable;
	}

	public int getChannel()
	{
		return channel;
	}

	public List< String > getSources()
	{
		return new ArrayList<>( nameToPath.keySet() ) ;
	}

	public String getPath( String source )
	{
		return nameToPath.get( source );
	}


}

