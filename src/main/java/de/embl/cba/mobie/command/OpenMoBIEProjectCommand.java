package de.embl.cba.mobie.command;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Open MoBIE Project..." )
public class OpenMoBIEProjectCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/mobie/platybrowser-datasets";

	@Override
	public void run()
	{
		MoBIESettings options = MoBIESettings.settings();

		try
		{
			new MoBIE( projectLocation, options );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
