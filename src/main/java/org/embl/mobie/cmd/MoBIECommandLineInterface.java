package org.embl.mobie.cmd;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class MoBIECommandLineInterface implements Callable<Void> {

	// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/926

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image, e.g. -i /home/image.tif")
	private String[] images = null;

	@Option(names = {"-s", "--segmentation"}, required = false, description = "label mask image, e.g. -s /home/labels.tif")
	private String[] segmentations = null;

	@Override
	public Void call() throws Exception {
		run( images, segmentations );
		return null;
	}

	public void run( String[] images, String[] segmentations )
	{
		final ImageJ imageJ = new ImageJ();
		//imageJ.ui().showUI(); // TODO maybe don't?

		final ImagePlus intensityImp = IJ.openImage( images[ 0 ] );
		final ImagePlus segmentationImp = IJ.openImage( segmentations[ 0 ] );

		new MoBIE( "", intensityImp, segmentationImp );
	}

	public static final void main( final String... args ) {
		new CommandLine( new MoBIECommandLineInterface() ).execute( args );
	}
}