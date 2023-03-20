package org.embl.mobie.lib;

import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageSources
{
	protected final String name;
	protected Map< String, String > valueToAbsolutePath = new LinkedHashMap<>();
	protected GridType gridType;

	public ImageSources( String name, Table table, String columnName, String root, GridType gridType )
	{
		this.name = name;
		this.gridType = gridType;

		final StringColumn images = table.stringColumn( columnName );
		for ( String image : images )
		{
			File file = root == null ?  new File( image ) : new File( root, image );
			valueToAbsolutePath.put( image, file.getAbsolutePath() );
		}
	}
}
