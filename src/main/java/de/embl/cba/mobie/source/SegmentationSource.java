package de.embl.cba.mobie.source;

import de.embl.cba.mobie.table.TableDataFormat;

import java.util.Map;

public class SegmentationSource extends ImageSource
{
	public Map< TableDataFormat, StorageLocation > tableData;
}
