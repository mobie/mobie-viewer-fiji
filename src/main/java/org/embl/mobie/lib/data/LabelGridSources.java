/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.data;

import ij.IJ;
import omero.cmd.IHandle;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.util.MoBIEHelper;
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

public class LabelGridSources extends ImageGridSources
{
	protected Map< String, TableSource > nameToLabelTable = new LinkedHashMap<>();
	private static boolean logLabelParsingError = true;

	public LabelGridSources( String name, Table table, String columnName, Integer channelIndex, String root, String pathMapping, GridType gridType, boolean useTableForSegments )
	{
		super( name, table, columnName, channelIndex, root, pathMapping, gridType);

		if ( useTableForSegments )
		{
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
				if ( logLabelParsingError )
				{
					IJ.log( "[WARNING] The table does not contain parseable segments (labels) information."  );
					logLabelParsingError = false;
				}
			}
		}
	}

	public LabelGridSources( String name, String labelsPath, Integer channelIndex, String root, GridType grid )
	{
		super( name, labelsPath, channelIndex, root, grid );
	}

	public LabelGridSources( String name, String path, Integer channelIndex, String labelTablePath, String root, GridType grid )
	{
		super( name, path, channelIndex, root, grid );

		// deal with tables
		//
		if ( IOHelper.getType( labelTablePath ).equals( IOHelper.ResourceType.FILE ) )
		{
			// deal with potential relative paths and also potential regexp
			final List< String > labelTablePaths = MoBIEHelper.getFullPaths( labelTablePath, root );
			final ArrayList< String > labelMaskNames = new ArrayList<>( nameToFullPath.keySet() );
			for ( int tableIndex = 0; tableIndex < labelTablePaths.size(); tableIndex++ )
			{
				final StorageLocation storageLocation = new StorageLocation();
				final File tableFile = new File( labelTablePaths.get( tableIndex ) );
				storageLocation.absolutePath = tableFile.getParent();
				storageLocation.defaultChunk = tableFile.getName();

				final TableSource tableSource =
						new TableSource(
								TableDataFormat.fromPath( labelTablePaths.get( tableIndex ) ),
								storageLocation );
				nameToLabelTable.put( labelMaskNames.get( tableIndex ), tableSource );
			}
		}
		else // absolute paths to HTTP hosted data
		{
			final StorageLocation storageLocation = new StorageLocation();
			String fileName = IOHelper.getFileName( labelTablePath );
			String folder = labelTablePath.replace( fileName, "" );
			storageLocation.absolutePath = folder;
			storageLocation.defaultChunk = fileName;

			final TableSource tableSource =
					new TableSource(
							TableDataFormat.fromPath( labelTablePath ),
							storageLocation );
			String dataSourceName = nameToFullPath.keySet().iterator().next();
			nameToLabelTable.put( dataSourceName, tableSource );
		}
	}

	public TableSource getLabelTable( String name )
	{
		if ( nameToLabelTable.containsKey( name ) )
			return nameToLabelTable.get( name );
		else
			return null;
	}

	public int getNumLabelTables()
	{
		return nameToLabelTable.size();
	}
}
