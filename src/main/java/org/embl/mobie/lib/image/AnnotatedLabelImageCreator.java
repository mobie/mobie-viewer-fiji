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
package org.embl.mobie.lib.image;

import net.imglib2.type.numeric.RealType;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.annotation.LazyAnnotatedSegmentAdapter;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.TableDataSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.LazyAnnotatedSegmentTableModel;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegment;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSegmentCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.Table;

public class AnnotatedLabelImageCreator
{
	private AnnotatedLabelImage< ? > annotatedLabelImage;

	public AnnotatedLabelImageCreator( MoBIE moBIE,
									   TableDataSource dataSource,
									   Image< ? extends RealType< ? > > labelImage
	)
	{
		if ( dataSource.getTableData() != null )
		{
			//System.out.println(dataSource.getName() + ": initialising.." );

			final StorageLocation tableLocation = moBIE.getTableLocation( dataSource.getTableData() );
			final TableDataFormat tableFormat = moBIE.getTableDataFormat( dataSource.getTableData() );

			Table table = dataSource.preInit() ?
					TableOpener.open( tableLocation, tableFormat ) : null;

			final TableSawAnnotatedSegmentCreator annotationCreator = new TableSawAnnotatedSegmentCreator( table );

			final TableSawAnnotationTableModel< TableSawAnnotatedSegment >  tableModel = new TableSawAnnotationTableModel( dataSource.getName(), annotationCreator, tableLocation, tableFormat, table );

			final DefaultAnnData< TableSawAnnotatedSegment > annData = new DefaultAnnData<>( tableModel );

			final DefaultAnnotationAdapter< TableSawAnnotatedSegment > annotationAdapter = new DefaultAnnotationAdapter( annData );

			annotatedLabelImage = new DefaultAnnotatedLabelImage( labelImage, annData, annotationAdapter );
		}
		else
		{
			// label image without annotation table
			final LazyAnnotatedSegmentTableModel tableModel = new LazyAnnotatedSegmentTableModel( labelImage.getName() );
			final DefaultAnnData< AnnotatedSegment > annData = new DefaultAnnData<>( tableModel );
			final LazyAnnotatedSegmentAdapter segmentAdapter = new LazyAnnotatedSegmentAdapter( labelImage.getName(), tableModel );
			annotatedLabelImage = new DefaultAnnotatedLabelImage( labelImage, annData, segmentAdapter );
		}
	}

	public AnnotatedLabelImage< ? > create()
	{
		return annotatedLabelImage;
	}
}
