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
import ij.ImagePlus;
import org.embl.mobie.lib.files.ImageFileSources;
import org.embl.mobie.lib.files.LabelFileSources;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.embl.mobie.lib.transform.GridType;
import tech.tablesaw.api.Table;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SourcesFromAutoMicTableCreator
{
	private final List< ImageFileSources > imageFileSources;
	private final List< LabelFileSources > labelSources;

	public SourcesFromAutoMicTableCreator( String tablePath, GridType gridType )
	{
		File tableFile = new File( tablePath );
		Path rootFolder =  Paths.get( tableFile.getParent() );
		final Table table = TableOpener.openDelimitedTextFile( tablePath );
		table.columnNames();

		//String[] imageFileTags = tableModel.getImageFileTags();
		String[] imageTags = new String[]{ "Result.Image" }; // FIXME: Do not hard-code

		imageFileSources = new ArrayList<>();
		for ( String imageTag : imageTags )
		{
			String referenceImagePath = MoBIEHelper.getAbsoluteImagePathFromAutoMicTable( table, imageTag, rootFolder, 0 );

			int numChannels = getNumChannels( imageTag, referenceImagePath );

			for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
			{
				imageFileSources.add( new ImageFileSources( imageTag + "_C" + channelIndex, table, rootFolder, imageTag, channelIndex, gridType ) );
			}
		}

		labelSources = new ArrayList<>(); // TODO: AutoMicTools uses ROIs....
	}

	private static int getNumChannels( String imageTag, String metadataImagePath ) {
		try
		{
			IJ.log( "Determining number of channels of " + imageTag + "...");
			final ImagePlus imagePlus = MoBIEHelper.openWithBioFormats( metadataImagePath, 0 );
           	int numChannels = imagePlus.getNChannels();
			IJ.log( "...number of channels is " + numChannels );
			return numChannels;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
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
