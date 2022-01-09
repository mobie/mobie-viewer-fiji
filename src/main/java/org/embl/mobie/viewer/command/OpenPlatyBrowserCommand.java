package org.embl.mobie.viewer.command;

import org.embl.mobie.io.n5.util.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
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
