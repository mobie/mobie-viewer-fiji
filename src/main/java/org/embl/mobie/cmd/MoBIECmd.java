package org.embl.mobie.cmd;

import org.embl.mobie.Data;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.transform.GridType;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise multi-modal big image data, see https://mobie.github.io/")
public class MoBIECmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	// FIXME: https://github.com/mobie/mobie-viewer-fiji/issues/926

	// TODO add --hcs

	@Option(names = {"-p", "--project"}, required = false, description = "open a MoBIE project, e.g. -p \"https://github.com/mobie/platybrowser-datasets\"")
	public String project = null;

	@Option(names = {"-v", "--view"}, required = false, description = "open a specific view within the above MoBIE project, e.g. -v \"cells")
	public String view = null;

	@Option(names = {"-r", "--root"}, required = false, description = "")
	public String root = null;

	@Option(names = {"-t", "--table"}, required = false, description = "create a MoBIE project from a table with image and segmentation paths")
	public String table = null;

	@Option(names = {"-i", "--image"}, required = false, description = "open an intensity image from a path, e.g., -i \"/home/image.tif\"; you can use wild-cards to open several images, e.g., -i \"/home/*-image.tif\"")
	public String[] images = null;

	@Option(names = {"-l", "--labels"}, required = false, description = "opens a segmentation label mask image from a path, e.g. -s \"/home/labels.tif\"; wild cards are supported (see --image)")
	public String[] labels = null;

	@Option(names = {"-g", "--grid"}, required = false, description = "grid type: none, merge, transform")
	public String grid = null;

	@Override
	public Void call() throws Exception {

		if ( project == null && images == null && labels == null )
		{
			System.out.println( "Please either provide a project (-p), or an image (-i) and/or a segmentation (-s).");
			System.exit( 1 );
		}

		MoBIE.openedFromCLI = true;

		if ( project != null )
		{
			final MoBIESettings settings = new MoBIESettings();
			if ( view != null ) settings.view( view );
			new MoBIE( project, settings );
		}
		else if ( table != null )
		{
			new MoBIE( Data.Table, table, images, labels, root, GridType.fromString( grid ) );
		}
		else
		{
			//new MoBIE( "", images, labels, tables, grids );
		}

		return null;
	}

	public static final void main( final String... args ) {

		final MoBIECmd moBIECmd = new MoBIECmd();

		if ( args == null || args.length == 0 )
			new CommandLine( moBIECmd ).execute( "--help" );
		else
			new CommandLine( moBIECmd ).execute( args );
	}
}