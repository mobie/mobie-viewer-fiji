package org.embl.mobie.cmd;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.MoBIE;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class MoBIECmd implements Callable<Void> {

	// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/926

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image, e.g. -i /home/image.tif")
	private String[] images = null;

	@Option(names = {"-s", "--segmentation"}, required = false, description = "segmentation label mask image, e.g. -s /home/labels.tif")
	private String[] segmentations = null;

	@Option(names = {"-t", "--table"}, required = false, description = "segments feature table, e.g. -t /home/features.csv")
	private String[] tables = null;

	@Override
	public Void call() throws Exception {
		run( images, segmentations, tables );
		return null;
	}

	public void run( String[] images, String[] segmentations, String[] tables ) throws SpimDataException
	{
		new ImageJ();// init SciJava services
		new MoBIE( "", images, segmentations, tables );
	}

	public static final void main( final String... args ) {
		new CommandLine( new MoBIECmd() ).execute( args );
	}
}