package org.embl.mobie.viewer.serialize;

import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;

import java.util.Map;

public class TableData
{
	public String defaultTable;

	public Map< TableDataFormat, StorageLocation > dataStores;
}
