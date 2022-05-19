package org.embl.mobie.viewer.command;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.project.PublishedProject;
import org.embl.mobie.viewer.project.PublishedProjects;
import ij.gui.GenericDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.HashMap;


@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Open>Open Published MoBIE Project..." )
public class OpenMoBIEPublishedProjectCommand implements Command
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Override
	public void run()
	{
		selectProject();
	}

	private void selectProject()
	{
		final HashMap< String, PublishedProject> projects = new PublishedProjects().getPublishedProjects();

		final GenericDialog gd = new GenericDialog( "Please select a project" );

		final String[] items = ( String[] ) projects.keySet().toArray( new String[ projects.size() ]);
		gd.addChoice( "Project", items, items[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		final String choice = gd.getNextChoice();

		final PublishedProject project = projects.get( choice );

		try
		{
			new MoBIE( project.location, MoBIESettings.settings().publicationURL( project.pulicationURL ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
