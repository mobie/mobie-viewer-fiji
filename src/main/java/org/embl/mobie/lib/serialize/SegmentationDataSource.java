/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.serialize;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.TableSource;

import java.util.HashMap;
import java.util.Map;

public class SegmentationDataSource extends ImageDataSource
{
	// Serialisation
	public Map< TableDataFormat, StorageLocation > tableData;

	public SegmentationDataSource( )
	{
	}

	public SegmentationDataSource( String name, ImageDataFormat imageDataFormat, StorageLocation imageLocation )
	{
		super( name, imageDataFormat, imageLocation );
	}

	public SegmentationDataSource( String name, ImageDataFormat imageDataFormat, StorageLocation imageLocation, TableDataFormat tableDataFormat, StorageLocation tableLocation )
	{
		super( name, imageDataFormat, imageLocation );
		if ( tableDataFormat != null && tableLocation != null)
		{
			this.tableData = new HashMap<>();
			this.tableData.put( tableDataFormat, tableLocation );
		}
	}

	public static SegmentationDataSource create( String name, ImageDataFormat imageDataFormat, StorageLocation storageLocation, TableSource tableSource )
	{
		if ( tableSource != null )
		{
			return new SegmentationDataSource( name, imageDataFormat, storageLocation, tableSource.getFormat(), tableSource.getLocation() );
		}
		else
		{
			return new SegmentationDataSource( name, imageDataFormat, storageLocation, null, null );
		}
	}

}
