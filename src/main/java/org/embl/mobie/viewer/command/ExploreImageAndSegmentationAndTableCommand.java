/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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
package org.embl.mobie.viewer.command;

import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.results.ResultsTableFetcher;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.TableDataFormatNames;
import org.embl.mobie.viewer.table.saw.TableOpener;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_X;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_Y;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MAX_Z;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_X;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_Y;
import static de.embl.cba.tables.imagesegment.SegmentUtils.BB_MIN_Z;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Explore>Explore Image and Segmentation and Table..."  )
public class ExploreImageAndSegmentationAndTableCommand extends DynamicCommand implements Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String LABEL = "Label";
	public static final String COLUMN_NAME_LABEL_IMAGE_ID = "LabelImage";
	public static final String CENTROID_X = "Centroid.X";
	public static final String CENTROID_Y = "Centroid.Y";
	public static final String CENTROID_Z = "Centroid.Z";
	public static final String MEAN_BREADTH = "MeanBreadth";

	@Parameter ( label = "Intensity image", required = false )
	public ImagePlus image;

	@Parameter ( label = "Label mask image" )
	public ImagePlus segmentation;

	// FIXME https://forum.image.sc/t/fetch-imagej1-resultstable-in-imagej2-command/22843/5
	@Parameter ( label = "Table" )
	public String tableName;

	@Parameter ( label = "Table format", choices = { TableDataFormatNames.MORPHOLIBJ } )
	public String tableFormat;

	@Override
	public void run()
	{
		final ResultsTableFetcher tableFetcher = new ResultsTableFetcher();
		ResultsTable resultsTable = tableFetcher.fetch( tableName );

		final Table table = TableOpener.open( resultsTable );
		final StorageLocation storageLocation = new StorageLocation();
		storageLocation.data = table;

		final AbstractSpimData< ? > imageData = new SpimDataOpener().open( image );
		final AbstractSpimData< ? > segmentationData = new SpimDataOpener().open( segmentation );

		new MoBIE( "ImageJ", imageData, segmentationData, storageLocation, TableDataFormat.fromString( tableFormat ) );
	}

	@Override
	public void initialize()
	{
		final ResultsTableFetcher tableFetcher = new ResultsTableFetcher();
		final HashMap< String, ResultsTable > titleToTable = tableFetcher.fetchCurrentlyOpenResultsTables();
		MutableModuleItem< String > input = getInfo().getMutableInput("tableName", String.class );
		input.setChoices( new ArrayList<>( titleToTable.keySet() ));
	}

	private List< TableRowImageSegment > createMLJTableRowImageSegments( ResultsTable resultsTable, String labelImageId )
	{
		Map< String, List< String > > columns = TableColumns.convertResultsTableToColumns( resultsTable );

		columns = TableColumns.addLabelImageIdColumn(
				columns,
				COLUMN_NAME_LABEL_IMAGE_ID,
				labelImageId );

		// TODO: replace this by proper bounding box
		if ( segmentation.getNSlices() > 1 )
		{
			addBoundingBoxColumn( columns, CENTROID_X, BB_MIN_X, true );
			addBoundingBoxColumn( columns, CENTROID_Y, BB_MIN_Y, true );
			addBoundingBoxColumn( columns, CENTROID_Z, BB_MIN_Z, true );
			addBoundingBoxColumn( columns, CENTROID_X, BB_MAX_X, false );
			addBoundingBoxColumn( columns, CENTROID_Y, BB_MAX_Y, false );
			addBoundingBoxColumn( columns, CENTROID_Z, BB_MAX_Z, false );
		}

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= createSegmentPropertyToColumnMap( columns );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
				columns,
				segmentPropertyToColumn,
				true );

		return segments;
	}

	private void addBoundingBoxColumn(
			Map< String, List< String > > columns, String centroid,
			String bb,
			boolean min )
	{
		final int numRows = columns.values().iterator().next().size();

		final List< String > column = new ArrayList<>();
		for ( int row = 0; row < numRows; row++ )
		{
			final double centre = Double.parseDouble(
					columns.get( centroid ).get( row ) );

			final double meanBreadth = Double.parseDouble(
					columns.get( MEAN_BREADTH ).get( row ) );

			if ( min )
				column.add( "" + (long) ( centre - 0.5 * meanBreadth ) );
			else
				column.add( "" + (long) ( centre + 0.5 * meanBreadth ) );
		}

		columns.put( bb, column );
	}

	private Map< SegmentProperty, List< String > > createSegmentPropertyToColumnMap( Map< String, List< String > > columns )
	{
		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= new HashMap<>();

		segmentPropertyToColumn.put(
				SegmentProperty.LabelImage,
				columns.get( COLUMN_NAME_LABEL_IMAGE_ID ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( LABEL ) );

		segmentPropertyToColumn.put(
				SegmentProperty.X,
				columns.get( CENTROID_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Y,
				columns.get( CENTROID_Y ) );

		if ( segmentation.getNSlices() > 1 )
		{
			segmentPropertyToColumn.put(
					SegmentProperty.Z,
					columns.get( CENTROID_Z ) );

			SegmentUtils.putDefaultBoundingBoxMapping( segmentPropertyToColumn, columns );
		}

		return segmentPropertyToColumn;
	}
}
