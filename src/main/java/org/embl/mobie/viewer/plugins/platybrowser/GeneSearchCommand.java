package org.embl.mobie.viewer.plugins.platybrowser;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.RealPoint;
import org.embl.mobie.viewer.color.LabelConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

import static de.embl.cba.bdv.utils.BdvUtils.getGlobalMouseCoordinates;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = "Platybrowser>ProSPr Gene Search")
public class GeneSearchCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Search radius [micrometer]")
	private double micrometerRadius = 3.0;

	@Override
	public void run()
	{
		double[] micrometerPosition = new double[ 3 ];
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		realPoint.localize( micrometerPosition );

		new Thread( () ->
		{
			IJ.log( "Gene search at [um]: " + Arrays.toString( micrometerPosition ) );
			IJ.log( "Gene search: In progress, please wait..." );
			final GeneSearch geneSearch = new GeneSearch( micrometerRadius, micrometerPosition, Arrays.asList( sourceAndConverters ) );
			geneSearch.searchGenes();
			IJ.log( "Gene search: Done!" );
		}
		).start();
	}

}
