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
package org.embl.mobie.lib.files;

import org.apache.commons.compress.utils.FileNameUtils;
import org.embl.mobie.lib.io.FileImageSource;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourcesFromPathsCreator
{
	private List< ImageFileSources > imageFileSources;
	private List< LabelFileSources > labelSources;
	private Table regionTable;

	public SourcesFromPathsCreator( List < String > imagePaths, List < String > labelPaths, List < String > labelTablePaths, String root, GridType grid )
	{
		// images
		//
		imageFileSources = new ArrayList<>();
		for ( String imagePath : imagePaths )
		{
			final FileImageSource fileImageSource = new FileImageSource( imagePath );
			imageFileSources.add( new ImageFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
		}

		// segmentation images
		//
		labelSources = new ArrayList<>();
		for ( int labelSourceIndex = 0; labelSourceIndex < labelPaths.size(); labelSourceIndex++ )
		{
			final FileImageSource fileImageSource = new FileImageSource( labelPaths.get( labelSourceIndex ) );

			if ( labelTablePaths.size() > labelSourceIndex )
			{
				final String labelTablePath = labelTablePaths.get( labelSourceIndex );
				labelSources.add( new LabelFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, labelTablePath, root, grid ) );
			}
			else
			{
				labelSources.add( new LabelFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
			}
		}

		// region table
		//
		regionTable = Table.create( "image table" );
		final List< String > regions = imagePaths.stream().map( path -> FileNameUtils.getBaseName(  path  ) ).collect( Collectors.toList() );
		regionTable.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );
	}

	// TODO consider adding back the functionality of groups for sorting the grid
	//			final List< String > groups = MoBIEHelper.getGroupNames( regex );
	//			if ( groups.size() > 0 )
	//			{
	//				final Pattern pattern = Pattern.compile( regex );
	//				final Set< String > set = new LinkedHashSet<>();
	//				for ( String path : paths )
	//				{
	//					final Matcher matcher = pattern.matcher( path );
	//					matcher.matches();
	//					set.add( matcher.group( 1 ) );
	//				}
	//
	//				final ArrayList< String > categories = new ArrayList<>( set );
	//				final int[] numSources = new int[ categories.size() ];
	//				grid.positions = new ArrayList<>();
	//				for ( String source : sources )
	//				{
	//					final Matcher matcher = pattern.matcher( source );
	//					matcher.matches();
	//					final int row = categories.indexOf( matcher.group( rowGroup ) );
	//					final int column = numSources[ row ];
	//					numSources[ row ]++;
	//					grid.positions.add( new int[]{ column, row } );
	//				}
	//			}
	//			}
	//			else
	//			{

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
