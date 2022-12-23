package org.embl.mobie.viewer.command;

import ij.IJ;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.command.util.ResultsTableFetcher;
import org.embl.mobie.viewer.table.TableDataFormatNames;

class ExploreImageAndSegmentationAndTableTest
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/mobie/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		IJ.open(  root + "src/test/resources/golgi-cell-features.csv");
		final ResultsTable resultsTable = new ResultsTableFetcher().fetch( "golgi-cell-features.csv" );


		final ExploreImageAndSegmentationAndTableCommand command = new ExploreImageAndSegmentationAndTableCommand();
		command.image = IJ.openImage( root + "src/test/resources/golgi-intensities.tif" );
		command.segmentation = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		command.tableName = resultsTable.getTitle();
		command.tableFormat = TableDataFormatNames.MLJ;

		command.run();
	}
}