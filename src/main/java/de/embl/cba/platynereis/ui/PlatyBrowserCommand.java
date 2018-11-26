package de.embl.cba.platynereis.ui;

import de.embl.cba.platynereis.PlatyBrowser;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>Platynereis" )
public class PlatyBrowserCommand implements Command
{

    @Parameter
    public LogService logService;

    @Parameter ( style = "directory" )
	File directory;

    public void run()
    {
    	new PlatyBrowser( directory.toString() );
    }

}