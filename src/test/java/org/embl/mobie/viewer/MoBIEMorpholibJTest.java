package org.embl.mobie.viewer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.command.util.ResultsTableFetcher;

class MoBIEMorpholibJTest
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/mobie/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		IJ.open(  root + "src/test/resources/golgi-cell-features.csv");
		final ResultsTable resultsTable = new ResultsTableFetcher().fetch( "golgi-cell-features.csv" );
		final ImagePlus intensityImp = IJ.openImage( root + "src/test/resources/golgi-intensities.tif" );
		//intensityImp.show();
		final ImagePlus labelImp = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		//labelImp.show();

		// no table
		new MoBIE( "MLJ test", intensityImp, labelImp );
	}
}