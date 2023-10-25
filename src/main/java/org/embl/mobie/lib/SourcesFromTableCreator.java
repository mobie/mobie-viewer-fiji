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
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

public class SourcesFromTableCreator
{
	private final List< ImageFileSources > imageFileSources;
	private final List< LabelFileSources > labelSources;

	public SourcesFromTableCreator( String tablePath, List< String > imageColumns, List< String > labelColumns, String root, GridType gridType )
	{
		final Table table = TableOpener.openDelimitedTextFile( tablePath );

		imageFileSources = new ArrayList<>();

		for ( String image : imageColumns )
		{
			if ( table.columnNames().contains( "FileName_" + image + "_IMG" ) )
			{
				// This is an AutoMicTools table, where the image path is distributed into the two columns (file name and folder)
				gridType = GridType.Transformed; // To accommodate rotations
				String fileName = table.getString( 0, "FileName_" + image + "_IMG" );
				String relativeFolder = table.getString( 0, "PathName_" + image + "_IMG" );
				String referenceImagePath = MoBIEHelper.createAbsolutePath( root, fileName, relativeFolder );
				IJ.log("Detected AutoMicTools table");
				IJ.log("Determining number of channels of " + image + ", using " + referenceImagePath +  "...");
				int numChannels = MoBIEHelper.getMetadataFromImageFile( referenceImagePath, 0 ).numChannelsContainer;
				IJ.log("Number of channels is " + numChannels);
				for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
				{
					imageFileSources.add( new ImageFileSources( image + "_C" + channelIndex, table, image, channelIndex, root, gridType ) );
				}
			}
			else
			{
				// Default table
				final TableImageSource tableImageSource = new TableImageSource( image );
				imageFileSources.add( new ImageFileSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType ) );
			}
		}

		labelSources = new ArrayList<>();
		if ( labelColumns.isEmpty() )
			return;

		final String firstLabel = labelColumns.get( 0 );
		for ( String label : labelColumns )
		{
			final TableImageSource tableImageSource = new TableImageSource( label );
			labelSources.add( new LabelFileSources( tableImageSource.name, table, tableImageSource.columnName, tableImageSource.channelIndex, root, gridType, label.equals( firstLabel ) ) );
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
}
