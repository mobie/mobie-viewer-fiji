package org.embl.mobie.lib.table;

import org.embl.mobie.lib.io.StorageLocation;

public class TableSource
{
	private TableDataFormat format;
	private StorageLocation location;

	public TableSource( TableDataFormat format, StorageLocation location )
	{
		this.format = format;
		this.location = location;
	}

	public TableDataFormat getFormat()
	{
		return format;
	}

	public StorageLocation getLocation()
	{
		return location;
	}
}
