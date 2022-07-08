/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mobie3.viewer.command;

import ij.gui.GenericDialog;
import mobie3.viewer.MoBIE;
import mobie3.viewer.MoBIESettings;
import mobie3.viewer.project.PublishedFigure;
import mobie3.viewer.project.PublishedFigures;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Open>Open Published MoBIE View..." )
public class OpenMoBIEFigureCommand implements Command
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Override
	public void run()
	{
		select();
	}

	private void select()
	{
		final List< PublishedFigure > figures = new PublishedFigures().getPublishedFigures();

		final ArrayList< String > figureNames = new ArrayList<>();
		for ( PublishedFigure figure : figures )
		{
			figureNames.add( figure.publicationAbbreviation + ": " + figure.name );
		}

		final GenericDialog gd = new GenericDialog( "Please select a view" );

		final String[] items = figureNames.toArray( new String[ figureNames.size() ]);
		gd.addChoice( "Figure", items, items[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		final int choice = gd.getNextChoiceIndex();

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
