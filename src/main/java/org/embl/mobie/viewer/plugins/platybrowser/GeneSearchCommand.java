package org.embl.mobie.viewer.plugins.platybrowser;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.BdvGlobalMousePositionProvider;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = "Platybrowser>ProSPr Gene Search")
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
		double[] position = new BdvGlobalMousePositionProvider( bdvHandle ).getPositionAsDoubles();

		new Thread( () ->
		{
			IJ.log( "Gene search at [um]: " + Arrays.toString( position ) );
			IJ.log( "Gene search: In progress, please wait..." );
			final GeneSearch geneSearch = new GeneSearch( radius, position, moBIE );
			geneSearch.searchGenes();
			IJ.log( "Gene search: Done!" );
		}
		).start();
	}

}
