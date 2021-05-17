package de.embl.cba.mobie2.command;

import de.embl.cba.mobie.ui.MoBIESettings;
import de.embl.cba.mobie2.MoBIE2;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE2>Open>Advanced>Open MoBIE Project Branch..." )
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
			new MoBIE2( projectLocation, MoBIESettings.settings().gitProjectBranch( projectBranch ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
