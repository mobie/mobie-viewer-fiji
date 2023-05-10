package org.embl.mobie.command;

import ij.macro.Interpreter;
import net.imagej.ImageJ;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MorpholibJ3DOutputViewingMacroTest
{
	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		StringBuilder contentBuilder = new StringBuilder();
		Files.lines( Paths.get( "/Users/tischer/Documents/mobie/scripts/visualise_morpholibj_3d_output.ijm" ) ).forEach( s -> contentBuilder.append(s).append("\n") );
		final String macro = contentBuilder.toString();
		new Interpreter().run( macro );
	}
}
