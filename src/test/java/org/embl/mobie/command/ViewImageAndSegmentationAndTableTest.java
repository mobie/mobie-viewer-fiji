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
package org.embl.mobie.command;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;
import org.embl.mobie.command.view.ViewImageAndSegmentationAndTableCommand;

import java.io.IOException;

class ViewImageAndSegmentationAndTableTest
{
	public static void main( String[] args ) throws IOException
	{
		String root = "/Users/tischer/Documents/mobie/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ResultsTable resultsTable = ResultsTable.open( root + "src/test/resources/golgi-cell-features-mlj.csv" );
		resultsTable.show( "MLJ" );

		final ImagePlus image = IJ.openImage( root + "src/test/resources/golgi-intensities.tif" );
		final ImagePlus segmentation = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif" );

		boolean interactive = false;

		if ( interactive )
		{
			resultsTable.show( resultsTable.getTitle() );
			image.show();
			segmentation.show();
		}
		else
		{
			final ViewImageAndSegmentationAndTableCommand command = new ViewImageAndSegmentationAndTableCommand();
			command.image = image;
			command.segmentation = segmentation;
			command.tableName = resultsTable.getTitle();
			command.run();
		}
	}
}
