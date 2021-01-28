package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Open MoBIE Project..." )
public class OpenMoBIEProjectCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/platybrowser/platybrowser";

	@Override
	public void run()
	{
		MoBIEOptions options = MoBIEOptions.options();

		if ( ! projectLocation.startsWith( "http" ) )
			options = options.imageDataStorageModality( MoBIEOptions.ImageDataStorageModality.FileSystem );

		new MoBIEViewer( projectLocation, options );
	}
}
