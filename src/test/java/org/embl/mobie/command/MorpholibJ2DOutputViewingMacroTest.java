package org.embl.mobie.command;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.view.ViewImageAndLabelsAndTableCommand;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.table.TableDataFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MorpholibJ2DOutputViewingMacroTest
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		StringBuilder contentBuilder = new StringBuilder();
		Files.lines( Paths.get( "/Users/tischer/Documents/mobie/scripts/visualise_morpholibj_2d_output.ijm" ) ).forEach( s -> contentBuilder.append(s).append("\n") );
		final String macro = contentBuilder.toString();
		new Interpreter().run( macro );

		final ResultsTableFetcher tableFetcher = new ResultsTableFetcher();
		final ResultsTable resultsTable = tableFetcher.fetch( "INCENP_T1_binary-lbl-Morphometry" );
		final ImagePlus image = WindowManager.getImage( "INCENP_T1" );
		final ImagePlus labels = WindowManager.getImage( "INCENP_T1_binary-lbl" );

		final ViewImageAndLabelsAndTableCommand command = new ViewImageAndLabelsAndTableCommand();
		command.view( image, labels, resultsTable );
	}
}
