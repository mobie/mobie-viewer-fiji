package org.embl.mobie.viewer.serialize;

import org.embl.mobie.viewer.io.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;

import java.util.Map;

public class TableData
{
	public String defaultTable;

	public Map< TableDataFormat, StorageLocation > dataStore;
}
