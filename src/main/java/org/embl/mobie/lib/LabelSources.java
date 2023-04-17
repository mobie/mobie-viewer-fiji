package org.embl.mobie.lib;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.table.columns.SegmentColumnNames;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LabelSources extends ImageSources
{
	protected Map< String, TableSource > nameToLabelTable = new LinkedHashMap<>();

	public LabelSources( String name, Table table, String columnName, Integer channelIndex, String root, GridType gridType )
	{
		super( name, table, columnName, channelIndex, root, gridType);

		final SegmentColumnNames segmentColumnNames = TableDataFormat.getSegmentColumnNames( table.columnNames() );

		if ( segmentColumnNames != null )
		{
			for ( Map.Entry< String, String > entry : nameToPath.entrySet() )
			{
				Table rowSubset = table.where( table.stringColumn( columnName ).isEqualTo( entry.getValue() ) );
				final StorageLocation storageLocation = new StorageLocation();
				storageLocation.data = rowSubset;
				final TableSource tableSource = new TableSource( TableDataFormat.Table, storageLocation );
				nameToLabelTable.put( entry.getKey(), tableSource );
			}
		}
		else
		{
			/*
			The table does not contain any segment information that can be parsed.
			 */
		}
	}

	public LabelSources( String name, String labelsPath, Integer channelIndex, String root, GridType grid )
	{
		super( name, labelsPath, channelIndex, root, grid );
	}

	public LabelSources( String name, String path, Integer channelIndex, String labelTablePath, String root, GridType grid )
	{
		super( name, path, channelIndex, root, grid );

		final List< String > labelTablePaths = getFullPaths( labelTablePath, root );
		final ArrayList< String > labelMaskNames = new ArrayList<>( nameToFullPath.keySet() );
		for ( int tableIndex = 0; tableIndex < labelTablePaths.size(); tableIndex++ )
		{
			final StorageLocation storageLocation = new StorageLocation();
			final File tableFile = new File( labelTablePaths.get( tableIndex ) );
			storageLocation.absolutePath = tableFile.getParent();
			storageLocation.defaultChunk = tableFile.getName();
			final TableDataFormat tableDataFormat = TableDataFormat.fromPath( labelTablePaths.get( tableIndex ) );
			final TableSource tableSource = new TableSource( tableDataFormat, storageLocation );
			nameToLabelTable.put( labelMaskNames.get( tableIndex ), tableSource );
		}
	}

	public TableSource getLabelTable( String name )
	{
		// May return null if there are no tables
		// for the label mask images
		return nameToLabelTable.get( name );
	}
}
