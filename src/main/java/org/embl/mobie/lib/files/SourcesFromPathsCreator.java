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
package org.embl.mobie.lib.files;

import org.embl.mobie.lib.io.FileImageSource;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

public class SourcesFromPathsCreator
{
	private final List< ImageFileSources > imageSources;
	private final List< LabelFileSources > labelSources;

	public SourcesFromPathsCreator( List < String > imagePaths, List < String > labelPaths, List < String > labelTablePaths, String root, GridType grid )
	{
		// images
		//
		imageSources = new ArrayList<>();
		for ( String imagePath : imagePaths )
		{
			final FileImageSource fileImageSource = new FileImageSource( imagePath );
			imageSources.add( new ImageFileSources( fileImageSource.name, fileImageSource.path, fileImageSource.channelIndex, root, grid ) );
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
	}

	public List< ImageFileSources > getImageSources()
	{
		return imageSources;
	}

	public List< LabelFileSources > getLabelSources()
	{
		return labelSources;
	}

	public Table getRegionTable()
	{
		// all images should be shown on the same grid,
		// thus we just return one of the region tables
		if ( ! imageSources.isEmpty() )
			return imageSources.get( 0 ).getRegionTable();
		else
			return labelSources.get( 0 ).getRegionTable();
	}
}
