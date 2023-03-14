package org.embl.mobie.command;

import net.imagej.ImageJ;

import java.io.File;

public class OpenImagesAndSegmentationsCommandTest {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static void main( String[] args )
	{
		new ImageJ().ui().showUI(); // initialise SciJava Services

		final OpenImageAndSegmentationCommand command = new OpenImageAndSegmentationCommand();
		command.image = new File( "/Users/tischer/Desktop/matt/preprocessed/XYZ__EGM_8hr_1--act.tif" );
		command.segmentation = null;
		command.table = null;
		command.run();
	}
}