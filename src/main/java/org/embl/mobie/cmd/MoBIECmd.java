package org.embl.mobie.cmd;

import org.embl.mobie.lib.MoBIE;
import org.embl.mobie.lib.MoBIESettings;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise multi-modal big image data, see https://mobie.github.io/")
public class MoBIECmd implements Callable<Void> {

	// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/926

	@Option(names = {"-p", "--project"}, required = false, description = "project, e.g. -p \"https://github.com/mobie/platybrowser-datasets\"")
	public String project = null;

	@Option(names = {"-v", "--view"}, required = false, description = "opens a specific view within a project (-p), e.g. -v \"cells")
	public String view = null;

	@Option(names = {"-i", "--image"}, required = false, description = "intensity image, e.g. -i \"/home/image.tif\"")
	public String[] images = null;

	@Option(names = {"-s", "--segmentation"}, required = false, description = "segmentation label mask image, e.g. -s \"/home/labels.tif\"")
	public String[] segmentations = null;

	@Option(names = {"-t", "--table"}, required = false, description = "segments feature table, e.g. -t \"/home/features.csv\"")
	public String[] tables = null;

	@Override
	public Void call() throws Exception {

		if ( project == null && images == null && segmentations == null )
		{
			System.exit( 0 );
			return null;
		}

		// I don't understand whether I should use callable or runnable
		// Runnable does not allow me to throw an Exception
		// Callable wants to return something, but what?

		MoBIE.openedFromCLI = true;

		if ( project != null )
		{
			final MoBIESettings settings = new MoBIESettings();
			if ( view != null ) settings.view( view );
			new MoBIE( project, settings );
		}
		else
			new MoBIE( "", images, segmentations, tables );

		return null;
	}

	public static final void main( final String... args ) {

		// Only show help if there are no arguments
		if ( args == null || args.length == 0 )
		{
			final MoBIECmd cmd = new MoBIECmd();
			final int exitCode = new CommandLine( cmd ).execute( "--help" );
			//System.exit( exitCode ); // MoBIE would be terminated immediately
		}

		final int exitCode = new CommandLine( new MoBIECmd() ).execute( args );
		//System.exit( exitCode );
	}
}