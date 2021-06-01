package de.embl.cba.mobie.bdv;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.Utils;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin( type=BdvPlaygroundActionCommand.class, name = BdvLocationLogger.NAME, menuPath = BdvLocationLogger.NAME )
public class BdvLocationLogger implements BdvPlaygroundActionCommand
{
	public static final String NAME = "BDV - Log Current Location";

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
