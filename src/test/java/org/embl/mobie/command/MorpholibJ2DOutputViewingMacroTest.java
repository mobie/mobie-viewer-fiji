package org.embl.mobie.command;

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

		// Manually run the code within ViewImageAndLabelsAndTableCommand
		// TODO: make this accessible as a method within the above command
		final AbstractSpimData< ? > imageData = new SpimDataOpener().open( WindowManager.getImage( "INCENP_T1" ) );
		final AbstractSpimData< ? > labelData = new SpimDataOpener().open( WindowManager.getImage( "INCENP_T1_binary-lbl" ) );
		final ResultsTableFetcher tableFetcher = new ResultsTableFetcher();
		final ResultsTable resultsTable = tableFetcher.fetch( "INCENP_T1_binary-lbl-Morphometry" );
		final TableDataFormat tableDataFormat = TableDataFormat.ResultsTable;
		final StorageLocation tableStorageLocation = new StorageLocation();
		tableStorageLocation.data = resultsTable;

		new MoBIE( "ImageJ", imageData, labelData, tableStorageLocation, tableDataFormat );

		// continue from UI (I did not manage to make the below work yet)

//		final ViewImageAndLabelsAndTableCommand command = new ViewImageAndLabelsAndTableCommand();
//		command.image = WindowManager.getImage( "INCENP_T1" );
//		command.labels = WindowManager.getImage( "INCENP_T1_binary-lbl" );
//		command.tableName = "INCENP_T1_binary-lbl-Morphometry";
//		command.initialize();
//		command.run();
	}
}
