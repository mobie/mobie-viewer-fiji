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
package org.embl.mobie.viewer.annotate;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RegionCreator
{
	private final Map< String, List< String > > columns;
	private final Map< String, List< String > > annotationIdToSources;
	private final Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier;
	private List< RegionTableRow > regionTableRows;

	public RegionCreator( Map< String, List< String > > columns, Map< String, List< String > > annotationIdToSources, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		this.columns = columns;
		this.annotationIdToSources = annotationIdToSources;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;
		createAnnotatedMasks();
	}

	private void createAnnotatedMasks()
	{
		final long currentTimeMillis = System.currentTimeMillis();

		regionTableRows = new ArrayList<>();
		final Set< String > annotationIds = annotationIdToSources.keySet();
		final List< String > annotationIdColumn = columns.get( TableColumnNames.REGION_ID );

		for ( String annotationId : annotationIds )
		{
			final ArrayList< Source< ? > > sources = getSources( annotationId );

			final RealMaskRealInterval mask = MoBIEHelper.unionRealMask( sources );
			//System.out.println( annotationId );
			//System.out.println( sources.size() );
			//System.out.println( Arrays.toString( mask.minAsDoubleArray() ));

			regionTableRows.add(
					new DefaultRegionTableRow(
							annotationId,
							mask,
							columns,
							annotationIdColumn.indexOf( annotationId ) )
			);
		}

		final long durationMillis = System.currentTimeMillis() - currentTimeMillis;
		if ( durationMillis > 100 )
			IJ.log("Created " + annotationIds.size() + " annotated intervals in " + durationMillis + " ms.");
	}

	private ArrayList< Source< ? > > getSources( String annotationId )
	{
		final ArrayList< Source< ? > > sources = new ArrayList<>();
		final List< String > sourceNames = annotationIdToSources.get( annotationId );
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
