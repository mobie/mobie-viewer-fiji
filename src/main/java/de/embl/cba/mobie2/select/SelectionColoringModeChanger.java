package de.embl.cba.mobie2.select;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.utils.Utils;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin( type=BdvPlaygroundActionCommand.class, name = SelectionColoringModeChanger.NAME, menuPath = SelectionColoringModeChanger.NAME )
public class SelectionColoringModeChanger implements BdvPlaygroundActionCommand
{
	public static final String NAME = "Segments - Change Selection Coloring Mode";

	@Parameter
	BdvHandle bdvh;



	@Override
	public void run()
	{
		new Thread( () -> {
			Logger.log( "\nPosition:\n" + BdvUtils.getGlobalMousePositionString( bdvh ) );
			Logger.log( "View:\n" + BdvUtils.getBdvViewerTransformString( bdvh ) );
			Logger.log( "Normalised view:\n" + Utils.createNormalisedViewerTransformString( bdvh, Utils.getMousePosition( bdvh ) ) );
		} ).start();
	}
}
