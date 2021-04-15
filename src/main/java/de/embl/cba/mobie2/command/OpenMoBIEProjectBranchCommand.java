package de.embl.cba.mobie2.command;

import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie2.MoBIE2;
import net.imagej.ImageJ;
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
			new MoBIE2( projectLocation, MoBIEOptions.options().gitProjectBranch( projectBranch ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
