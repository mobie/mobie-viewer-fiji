package de.embl.cba.platynereis.dataset;

import de.embl.cba.platynereis.json.JsonUtils;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class DatasetsParser
{
	public ArrayList< String > datasetsFromDataSource( String dataSourceLocation )
	{
		try
		{
			return datasetsFromFile( dataSourceLocation + "/datasets.json" );
		}
		catch ( Exception e )
		{
			try
			{
				return datasetsFromFile( dataSourceLocation + "/versions.json" );
			}
			catch ( Exception e2 )
			{
				e.printStackTrace();
				throw new UnsupportedOperationException( "Could not open or parse versions file: " + dataSourceLocation );
			}
		}
	}

	private ArrayList< String > datasetsFromFile( String path ) throws IOException
	{
		InputStream is = FileAndUrlUtils.getInputStream( path );

		final ArrayList< String > datasets = JsonUtils.readStringArray( is );

		return datasets;
	}
}
