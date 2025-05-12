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
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.io.TableImageSource;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.embl.mobie.lib.table.saw.Aggregators;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.TextColumn;
import tech.tablesaw.columns.Column;

import java.util.*;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class SourcesFromTableCreator
{
	private final List< ImageGridSources > imageGridSources;
	private final List< LabelGridSources > labelSources;
	private Table regionTable;

	public SourcesFromTableCreator( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, String pathMapping, GridType gridType )
	{
		final Table table = TableOpener.openDelimitedFile( tablePath );

		// images
		//
		imageGridSources = new ArrayList<>();

		for ( String imageColumn : imageColumns )
		{
			if ( imageColumn.contains( "_IMG" ) )
			{
				IJ.log( "Detected AutoMicTools table" );

				// the image path is distributed into two columns: file name and folder
				String fileName = table.getString( 0, imageColumn);
				String relativeFolder = table.getString( 0, imageColumn.replace(  "FileName_", "PathName_" ) );
				String referenceImagePath = MoBIEHelper.createAbsolutePath( root, fileName, relativeFolder );
				IJ.log( "Determining number of channels of \"" + imageColumn + "\" from " + referenceImagePath + "..." );
				ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( referenceImagePath );
				int numChannels = DataStore.fetchImageData( referenceImagePath, imageDataFormat, ThreadHelper.sharedQueue ).getNumDatasets();
				IJ.log( "Number of channels: " + numChannels );
				for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
				{
					imageGridSources.add( new ImageGridSources(
							imageColumn + "_C" + channelIndex,
							table,
							imageColumn,
							channelIndex,
							root,
							pathMapping,
							gridType ) );
				}
			}
			else
			{
				// Default table
				final TableImageSource tableImageSource = new TableImageSource( imageColumn );
				imageGridSources.add( new ImageGridSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, pathMapping, gridType ) );
			}
		}

		// segmentations
		//
		labelSources = new ArrayList<>();
		if ( ! labelColumns.isEmpty() )
		{
			final String firstLabel = labelColumns.get( 0 );
			for ( String label : labelColumns )
			{
				final TableImageSource tableImageSource = new TableImageSource( label );
				labelSources.add( new LabelGridSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, pathMapping, gridType, label.equals( firstLabel ) ) );
			}
		}


		// region table for grid view
		//
		if ( imageGridSources.isEmpty() )
			throw new RuntimeException("No images found in the table! Please check your table and image column names: " + imageColumns );

		int numSources = imageGridSources.get( 0 ).getSources().size();

		if ( table.rowCount() == numSources )
		{
			// the input table is an image table and
			// can thus be used as the region table
			String imageColumn = imageColumns.get( 0 );
			final List< String > regions = table.stringColumn( imageColumn ).asList();
			regionTable = table;
			if ( ! table.containsColumn( ColumnNames.REGION_ID ) )
				regionTable = table.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );
			regionTable.setName( "image table" );
		}
		else
		{
			// the input table is an object table
			// thus we need to summarize it into an image table
			// to be used as a region table
			regionTable = Table.create( "image table" );

			// init columns
			LinkedHashSet< String > uniqueRegionNames = new LinkedHashSet<>(); // important not to change the order!
			String imageColumnName = new TableImageSource( imageColumns.get( 0 ) ).columnName;
			StringColumn imageColumn = table.stringColumn( imageColumnName );
			for ( String imagePath : imageColumn )
			{
				// TODO: It would be nice to shorten the names, e.g. by removing everything that is common to all image paths
				uniqueRegionNames.add( imagePath );
			}
			final List< String > regions = new ArrayList<>( uniqueRegionNames );
			regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) ); // needed for region table
			regionTable.addColumns( StringColumn.create( imageColumnName, regions ) ); // needed for joining the tables

			final List< Column< ? > > columns = table.columns();
			for ( final Column< ? > column : columns )
			{
				if ( column instanceof NumberColumn )
				{
					final Table summary = table.summarize( column, mean ).by( imageColumn );
					regionTable = regionTable.joinOn( imageColumnName ).leftOuter( summary );
				}
				else if ( column instanceof StringColumn )
				{
					final Table summary = table.summarize( column, Aggregators.firstString ).by( imageColumn );
					regionTable = regionTable.joinOn( imageColumnName ).leftOuter( summary );
				}
				else if ( column instanceof TextColumn )
				{
					final Table summary = table.summarize( column.asStringColumn(), Aggregators.firstString ).by( imageColumn );
					regionTable = regionTable.joinOn( imageColumnName ).leftOuter( summary );
				}
				else
				{
					throw new RuntimeException( "Unsupported column type " + column.getClass() + " of column " + column.name() );
				}
			}
		}
	}

	public List< ImageGridSources > getImageSources()
	{
		return imageGridSources;
	}

	public List< LabelGridSources > getLabelSources()
	{
		return labelSources;
	}

	public Table getRegionTable()
	{
		return regionTable;
	}

}
