package org.embl.mobie.viewer.command;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Advanced>Open MoBIE Project With S3 Credentials..." )
public class OpenMoBIEProjectWithS3CredentialsCommand implements Command
{
	@Parameter ( label = "S3 Project Location" )
	public String projectLocation = "https://s3.embl.de/comulis";

	@Parameter ( label = "S3 Access Key", persist = false )
	public String s3AccessKey = "";

	@Parameter ( label = "S3 Secret Key", persist = false )
	public String s3SecretKey = "";

	@Override
	public void run()
	{
		try
		{
			new MoBIE(
					projectLocation,
					MoBIESettings.settings()
							.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } )
			);
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
