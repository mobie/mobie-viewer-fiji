package de.embl.cba.mobie.command;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Open>Advanced>Open MoBIE Project Branch..." )
public class OpenMoBIEProjectBranchCommand implements Command
{
	@Parameter ( label = "Project Location" )
	public String projectLocation = "https://github.com/platybrowser/platybrowser";

	@Parameter ( label = "Project Branch" )
	public String projectBranch = "master";

	@Override
	public void run()
	{
		try
		{
			new MoBIE( projectLocation, MoBIESettings.settings().gitProjectBranch( projectBranch ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
