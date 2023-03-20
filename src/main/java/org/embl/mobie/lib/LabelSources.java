package org.embl.mobie.lib;

import org.embl.mobie.lib.table.TableSource;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class LabelSources extends ImageSources
{
	protected Map< String, TableSource > nameToTableSource = new LinkedHashMap<>();
	GridType gridType;

	public LabelSources( String name, Table table, String columnName, String root, GridType gridType )
	{
		super( name, table, columnName, root, gridType);

		for ( String value : valueToAbsolutePath.keySet() )
		{

		}
		Table t = table.where( table.stringColumn( columnName ).isEqualTo(  ) );
		final StringColumn images = table.stringColumn( columnName );
		for ( String image : images )
		{
			File file = root == null ?  new File( image ) : new File( root, image );
			valueToAbsolutePath.put( file.getName(), file.getAbsolutePath() );
		}
	}
}
