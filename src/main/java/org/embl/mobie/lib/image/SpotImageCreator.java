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

import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.SpotDataSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.TableDataFormat;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.Table;

public class SpotImageCreator
{
	private final SpotDataSource spotDataSource;
	private final StorageLocation tableLocation;
	private final TableDataFormat tableFormat;
	private Image< ? extends IntegerType< ? >  > labelImage;
	private DefaultAnnData< AnnotatedSpot > annData;

	public SpotImageCreator( SpotDataSource spotDataSource,
							 StorageLocation tableLocation,
							 TableDataFormat tableFormat )
	{
		this.spotDataSource = spotDataSource;
		this.tableLocation = tableLocation;
		this.tableFormat = tableFormat;
	}

	public AnnotatedLabelImage< ? > createSpotImage()
	{
		Table table = TableOpener.open( tableLocation, tableFormat );

		// TODO: maybe make the spot column names mapping configurable?
		final TableSawAnnotationCreator< TableSawAnnotatedSpot > annotationCreator
				= new TableSawAnnotatedSpotCreator( table );

		final TableSawAnnotationTableModel< AnnotatedSpot > tableModel =
				new TableSawAnnotationTableModel(
						spotDataSource.getName(),
						annotationCreator,
						tableLocation,
						tableFormat,
						table );

		annData = new DefaultAnnData<>( tableModel );

		labelImage = new SpotLabelImage(
				spotDataSource.getName(),
				annData,
				null,
				spotDataSource.boundingBoxMin,
				spotDataSource.boundingBoxMax );

		final DefaultAnnotationAdapter< TableSawAnnotatedSpot > annotationAdapter
				= new DefaultAnnotationAdapter( annData );

		// annotated spots label image
		AnnotatedLabelImage< ? > annotatedLabelImage =
				new DefaultAnnotatedLabelImage( labelImage, annData, annotationAdapter );

		return annotatedLabelImage;
	}
}
