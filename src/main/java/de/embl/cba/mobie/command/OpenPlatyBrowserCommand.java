package de.embl.cba.mobie.command;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.IOException;


@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Open PlatyBrowser")
public class OpenPlatyBrowserCommand implements Command
{
	@Override
	public void run()
	{
		MoBIESettings options = MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5S3 );

		try
		{
			new MoBIE( "https://github.com/mobie/platybrowser-datasets", options );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
