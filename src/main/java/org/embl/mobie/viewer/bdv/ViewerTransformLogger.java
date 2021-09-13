package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.Logger;
import org.embl.mobie.viewer.playground.PlaygroundUtils;
import org.embl.mobie.viewer.Utils;
import org.embl.mobie.viewer.serialize.JsonHelper;
import org.embl.mobie.viewer.transform.AffineViewerTransform;
import org.embl.mobie.viewer.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.viewer.transform.PositionViewerTransform;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin( type = BdvPlaygroundActionCommand.class, name = ViewerTransformLogger.NAME, menuPath = ViewerTransformLogger.NAME )
public class ViewerTransformLogger implements BdvPlaygroundActionCommand
{
	public static final String NAME = "BDV - Log Current View";

	@Parameter
	BdvHandle bdv;

	@Override
	public void run()
	{
		new Thread( () -> {

			final int timepoint = bdv.getViewerPanel().state().getCurrentTimepoint();
			final double[] position = PlaygroundUtils.getWindowCentreInCalibratedUnits( bdv );

			// position
			final PositionViewerTransform positionViewerTransform = new PositionViewerTransform( position, timepoint );

			// affine
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			bdv.getViewerPanel().state().getViewerTransform( affineTransform3D );
			final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( affineTransform3D.getRowPackedCopy(), timepoint );

			// normalized affine
			final AffineTransform3D normalisedViewerTransform = Utils.createNormalisedViewerTransform( bdv,
					PlaygroundUtils.getWindowCentreInPixelUnits( bdv ) );
			final NormalizedAffineViewerTransform normalizedAffineViewerTransform = new NormalizedAffineViewerTransform( normalisedViewerTransform.getRowPackedCopy(), timepoint );

			// print
			final Gson gson = JsonHelper.buildGson( false );
			Logger.log( "# Current view " );
			Logger.log( "To restore the view, any of below lines can be pasted into the \'location\' text field." );
			Logger.log( "To share views with other people we recommend \'normalizedAffine\'." );

			Logger.log( gson.toJson( positionViewerTransform ) );
			Logger.log( gson.toJson( affineViewerTransform ) );
			Logger.log( gson.toJson( normalizedAffineViewerTransform ) );

		} ).start();
	}
}
