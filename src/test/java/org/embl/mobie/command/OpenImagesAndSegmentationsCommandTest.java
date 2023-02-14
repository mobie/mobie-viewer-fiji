package org.embl.mobie.command;

import net.imagej.ImageJ;

public class OpenImagesAndSegmentationsCommandTest {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static void main( String[] args )
	{
		new ImageJ().ui().showUI(); // initialise SciJava Services

		final OpenImagesAndSegmentationsCommand command = new OpenImagesAndSegmentationsCommand();
		command.image = "/Users/tischer/Desktop/Kristina/2022_11_18/*-pro.tif";
		command.segmentation = "/Users/tischer/Desktop/Kristina/2022_11_18/*-seg.tif";
		command.table = "/Users/tischer/Desktop/Kristina/2022_11_18/*csv";
		command.run();
	}
}