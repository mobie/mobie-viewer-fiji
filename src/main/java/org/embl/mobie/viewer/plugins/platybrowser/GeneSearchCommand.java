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
package org.embl.mobie.viewer.plugins.platybrowser;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.GlobalMousePositionProvider;
import org.embl.mobie.viewer.bdv.CircleOverlay;
import org.embl.mobie.viewer.command.CommandConstants;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "ProSPr Gene Search")
public class GeneSearchCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Search radius [micrometer]")
	private double radius = 3.0;

	private static MoBIE moBIE;

	public static void setMoBIE( MoBIE moBIE )
	{
		GeneSearchCommand.moBIE = moBIE;
	}

	@Override
	public void run()
	{
		new Thread( () ->
		{
			double[] position = new GlobalMousePositionProvider( bdvHandle ).getPositionAsDoubles();

			final CircleOverlay circleOverlay = new CircleOverlay( position, radius );
			BdvFunctions.showOverlay( circleOverlay, "", BdvOptions.options().addTo( bdvHandle ) );

			IJ.log( "Gene search at [um]: " + Arrays.toString( position ) );
			IJ.log( "Gene search: In progress, please wait..." );
			final GeneSearch geneSearch = new GeneSearch( radius, position, moBIE );
			geneSearch.searchGenes();
			IJ.log( "Gene search: Done!" );
		}
		).start();
	}

}
