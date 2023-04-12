package org.embl.mobie.cmd;

import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.transform.GridType;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie-project", mixinStandardHelpOptions = true, version = "3.0.13", description = "Visualise multi-modal big image data stored as a MoBIE project, see https://mobie.github.io/")
public class ProjectCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-p", "--project"}, required = false, description = "MoBIE project e.g. -p \"https://github.com/mobie/platybrowser-datasets\"")
	public String project = null;

	@Option(names = {"-v", "--view"}, required = false, description = "view within the above MoBIE project, e.g. -v \"cells")
	public String view = null;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings()
				.cli( true );

		if ( view != null ) settings.view( view );

		new MoBIE( project, settings );

		return null;
	}

	public static final void main( final String... args ) {

		final ProjectCmd projectCmd = new ProjectCmd();

		if ( args == null || args.length == 0 )
			new CommandLine( projectCmd ).execute( "--help" );
		else
			new CommandLine( projectCmd ).execute( args );
	}
}