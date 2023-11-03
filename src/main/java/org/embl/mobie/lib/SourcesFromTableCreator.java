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
package org.embl.mobie.lib;

import ij.IJ;
import org.embl.mobie.lib.files.ImageFileSources;
import org.embl.mobie.lib.files.LabelFileSources;
import org.embl.mobie.lib.io.TableImageSource;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.table.saw.Aggregators;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class SourcesFromTableCreator
{
	private final List< ImageFileSources > imageFileSources;
	private final List< LabelFileSources > labelSources;
	private Table regionTable;

	public SourcesFromTableCreator( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, GridType gridType )
	{
		final Table table = TableOpener.openDelimitedTextFile( tablePath );

		// images
		//
		imageFileSources = new ArrayList<>();

		for ( String imageColumn : imageColumns )
		{
			if ( imageColumn.contains( "_IMG" ) )
			{
				IJ.log( "Detected AutoMicTools table" );

				// the image path is distributed into two columns: file name and folder
				String fileName = table.getString( 0, imageColumn);
				String relativeFolder = table.getString( 0, imageColumn.replace(  "FileName_", "PathName_" ) );
				String referenceImagePath = MoBIEHelper.createAbsolutePath( root, fileName, relativeFolder );
				IJ.log( "Determining number of channels of " + imageColumn + ", using " + referenceImagePath + "..." );
				int numChannels = MoBIEHelper.getMetadataFromImageFile( referenceImagePath, 0 ).numChannelsContainer;
				IJ.log( "Number of channels is " + numChannels );
				for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
				{
					imageFileSources.add( new ImageFileSources( imageColumn + "_C" + channelIndex, table, imageColumn, channelIndex, root, gridType ) );
				}
			}
			else
			{
				// Default table
				final TableImageSource tableImageSource = new TableImageSource( imageColumn );
				imageFileSources.add( new ImageFileSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType ) );
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
				labelSources.add( new LabelFileSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType, label.equals( firstLabel ) ) );
			}
		}

		// region table for grid view
		//
		int numSources = imageFileSources.get( 0 ).getSources().size();

		if ( numSources == 1 )
		{
			// no region table and grid view needed
		}
		else if ( table.rowCount() == numSources )
		{
			// the input table is an image table and
			// can thus be used as the region table
			String imageColumn = imageColumns.get( 0 );
			final List< String > regions = table.stringColumn( imageColumn ).asList();
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
			LinkedHashSet< String > uniqueImagePaths = new LinkedHashSet<>(); // important not to change the order!
			String imageColumnName = new TableImageSource( imageColumns.get( 0 ) ).columnName;
			StringColumn imageColumn = table.stringColumn( imageColumnName );
			for ( String imagePath : imageColumn )
			{
				uniqueImagePaths.add( imagePath );
			}
			final List< String > regions = new ArrayList<>( uniqueImagePaths );
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
				else
				{
					throw new RuntimeException( "Unsupported column type " + column.getClass() );
				}
			}
		}
	}

	public List< ImageFileSources > getImageSources()
	{
		return imageFileSources;
	}

	public List< LabelFileSources > getLabelSources()
	{
		return labelSources;
	}

	public Table getRegionTable()
	{
		return regionTable;
	}

}
