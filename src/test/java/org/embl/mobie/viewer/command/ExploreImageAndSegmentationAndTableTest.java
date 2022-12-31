package org.embl.mobie.viewer.command;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.table.TableDataFormatNames;

import java.io.IOException;

class ExploreImageAndSegmentationAndTableTest
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
			final ExploreImageAndSegmentationAndTableCommand command = new ExploreImageAndSegmentationAndTableCommand();
			command.image = image;
			command.segmentation = segmentation;
			command.tableName = resultsTable.getTitle();
			command.run();
		}
	}
}