package org.embl.mobie.viewer.command;

import ij.gui.GenericDialog;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.project.PublishedFigure;
import org.embl.mobie.viewer.project.PublishedFigures;
import org.embl.mobie.viewer.project.PublishedProject;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.HashMap;


@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Open>Open Published MoBIE Figure..." )
public class OpenPublishedMoBIEFigureCommand implements Command
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Override
	public void run()
	{
		select();
	}

	private void select()
	{
		final HashMap< String, PublishedFigure > figures = new PublishedFigures().getPublishedFigures();

		final GenericDialog gd = new GenericDialog( "Please select a figure" );

		final String[] items = figures.keySet().toArray( new String[ figures.size() ]);
		gd.addChoice( "Figure", items, items[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		final String choice = gd.getNextChoice();

		final PublishedFigure figure = figures.get( choice );

		try
		{
			new MoBIE( figure.location, MoBIESettings.settings().publicationURL( figure.publicationURL ).view( figure.view ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
