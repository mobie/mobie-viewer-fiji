package org.embl.mobie.viewer.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelpers;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Display>Configure Label Rendering")
public class LabelRenderingConfiguratorCommand implements BdvPlaygroundActionCommand
{
	@Parameter
	BdvHandle bdvHandle;

	@Parameter
	SourceAndConverter[] sourceAndConverters;

	@Parameter ( label = "Show as boundaries")
	boolean showAsBoundary;

	@Parameter ( label = "Boundary thickness [pixels]", min = "1")
	int boundaryThickness;

	@Override
	public void run()
	{
		Arrays.stream( sourceAndConverters ).filter( sac -> SourceHelpers.getLabelSource( sac ) != null ).forEach( sac ->
		{
			final LabelSource< ? > labelSource = SourceHelpers.getLabelSource( sac );
			labelSource.showAsBoundary( showAsBoundary, boundaryThickness );

			if ( sac.asVolatile() != null )
			{
				( ( LabelSource ) sac.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
			}
		});

		bdvHandle.getViewerPanel().requestRepaint();
	}
}
