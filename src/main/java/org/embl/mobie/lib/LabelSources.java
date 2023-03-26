package org.embl.mobie.lib;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.util.LinkedHashMap;
import java.util.Map;

public class LabelSources extends ImageSources
{
	protected Map< String, TableSource > nameToLabelTable = new LinkedHashMap<>();

	public LabelSources( String name, Table table, String columnName, String root, GridType gridType )
	{
		super( name, table, columnName, root, gridType);

		for ( Map.Entry< String, String > entry : nameToPath.entrySet() )
		{
			Table rowSubset = table.where( table.stringColumn( columnName ).isEqualTo( entry.getValue() ) );
			final StorageLocation storageLocation = new StorageLocation();
			storageLocation.data = rowSubset;
			final TableSource tableSource = new TableSource( TableDataFormat.Table, storageLocation );
			nameToLabelTable.put( entry.getKey(), tableSource );

		}
	}

	public LabelSources( String name, String labelsPath, String root, GridType grid )
	{
		super( name, labelsPath, root, grid );
	}

	public TableSource getLabelTable( String name )
	{
		// May return null if there are no tables
		// for the label mask images
		return nameToLabelTable.get( name );
	}
}
