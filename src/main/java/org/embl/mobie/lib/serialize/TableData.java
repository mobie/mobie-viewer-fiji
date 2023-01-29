package org.embl.mobie.lib.serialize;

import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;

import java.util.Map;

public class TableData
{
	public String defaultTable;

	public Map< TableDataFormat, StorageLocation > dataStore;
}
