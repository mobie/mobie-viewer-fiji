/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie3.viewer.annotation;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie3.viewer.table.ColumnNames;
import org.embl.mobie3.viewer.source.SourceHelper;
import net.imglib2.roi.RealMaskRealInterval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RegionCreator
{
	private final Map< String, List< String > > columns;
	private final Map< String, List< String > > regionIdToSourceNames;
	private final Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier;
	private List< RegionTableRow > regionTableRows;

	public RegionCreator( Map< String, List< String > > columns, Map< String, List< String > > regionIdToSourceNames, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		this.columns = columns;
		this.regionIdToSourceNames = regionIdToSourceNames;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;
		createRegions();
	}

	private void createRegions()
	{
		final long currentTimeMillis = System.currentTimeMillis();

		regionTableRows = new ArrayList<>();
		final Set< String > regionIds = regionIdToSourceNames.keySet();
		final List< String > regionIdColumn = columns.get( ColumnNames.REGION_ID );

		System.out.println("numRegions = " + regionIds.size());
		for ( String regionId : regionIds )
		{
			System.out.println( "regionID = " + regionId );
			final ArrayList< Source< ? > > sources = getSources( regionId );
			final RealMaskRealInterval mask = SourceHelper.getUnionMask( sources, 0 );
			System.out.println( "numImages = " + sources.size() + ", region = " + Arrays.toString( mask.minAsDoubleArray() ) + " - " + Arrays.toString( mask.maxAsDoubleArray() ));

			regionTableRows.add(
					new DefaultRegionTableRow(
							regionId,
							mask,
							columns,
							regionIdColumn.indexOf( regionId ) )
			);
		}

		final long durationMillis = System.currentTimeMillis() - currentTimeMillis;
		if ( durationMillis > 100 )
			IJ.log("Created " + regionIds.size() + " annotated intervals in " + durationMillis + " ms.");
	}

	private ArrayList< Source< ? > > getSources( String annotationId )
	{
		final ArrayList< Source< ? > > sources = new ArrayList<>();
		final List< String > sourceNames = regionIdToSourceNames.get( annotationId );
		for ( String sourceName : sourceNames )
		{
			try
			{
				final Source< ? > source = sourceAndConverterSupplier.apply( sourceName ).getSpimSource();
				sources.add( source );
			} catch ( Exception e )
			{
				System.err.println( "Could not find " + sourceName + " among the image sources that are associated to this project.\nPlease check the project's dataset.json file to see whether it may be missing. ");
				e.printStackTrace();
				throw e;
			}
		}
		return sources;
	}

	public List< RegionTableRow > getRegionTableRows()
	{
		return regionTableRows;
	}
}
