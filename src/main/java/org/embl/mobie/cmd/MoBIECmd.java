package org.embl.mobie.cmd;

import org.embl.mobie.lib.MoBIE;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie", mixinStandardHelpOptions = true, version = "3.0.11", description = "Visualise multi-modal big image data, see https://mobie.github.io/")
public class MoBIECmd implements Callable<Void> {

	// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/926

	@Option(names = {"-p", "--project"}, required = false, description = "project, e.g. -p \"https://github.com/mobie/platybrowser-datasets\"")
	public String project = null;

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image, e.g. -i \"/home/image.tif\"")
	public String[] images = null;

	@Option(names = {"-s", "--segmentation"}, required = false, description = "segmentation label mask image, e.g. -s \"/home/labels.tif\"")
	public String[] segmentations = null;

	@Option(names = {"-t", "--table"}, required = false, description = "segments feature table, e.g. -t \"/home/features.csv\"")
	public String[] tables = null;

	@Override
	public Void call() throws Exception {

		// I don't understand whether I should use callable or runnable
		// Runnable does not allow me to throw an Exception
		// Callable wants to return something, but what?

		if ( project != null )
			new MoBIE( project );
		else
			new MoBIE( "", images, segmentations, tables );

		return null;
	}

	public static final void main( final String... args ) {

		// Show help if there are no arguments
		if ( args == null || args.length == 0 )
		{
			final int exitCode = new CommandLine( new MoBIECmd() ).execute( "--help" );
			System.exit( exitCode );
		}

		final int exitCode = new CommandLine( new MoBIECmd() ).execute( args );
		System.exit( exitCode );
	}
}