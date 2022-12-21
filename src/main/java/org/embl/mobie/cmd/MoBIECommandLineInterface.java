package org.embl.mobie.cmd;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import org.jetbrains.annotations.TestOnly;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class MoBIECommandLineInterface implements Callable<Void> {

	@Option(names = {"-i", "--image"}, required = true, description = "intensity image, e.g. -i /home/image.tif")
	private String image = null;

	@Option(names = {"-s", "--segmentation"}, required = true, description = "label mask image, e.g. -s /home/labels.tif")
	private String segmentation = null;

	@Override
	public Void call() throws Exception {
		run( image, segmentation );
		return null;
	}

	public void run( String image, String segmentation )
	{
		final ImageJ imageJ = new ImageJ();
		//imageJ.ui().showUI(); // TODO maybe don't?

		final ImagePlus intensityImp = IJ.openImage( image );
		final ImagePlus segmentationImp = IJ.openImage( segmentation );

		new MoBIE( "", intensityImp, segmentationImp );
	}

	public static final void main( final String... args ) {
		new CommandLine( new MoBIECommandLineInterface() ).execute( args );
	}
}