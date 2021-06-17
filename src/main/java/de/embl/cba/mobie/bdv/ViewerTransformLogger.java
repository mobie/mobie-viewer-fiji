package de.embl.cba.mobie.bdv;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.serialize.JsonHelper;
import de.embl.cba.mobie.transform.AffineViewerTransform;
import de.embl.cba.mobie.transform.NormalizedAffineViewerTransform;
import de.embl.cba.mobie.transform.PositionViewerTransform;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin( type = BdvPlaygroundActionCommand.class, name = ViewerTransformLogger.NAME, menuPath = ViewerTransformLogger.NAME )
public class ViewerTransformLogger implements BdvPlaygroundActionCommand
{
	public static final String NAME = "BDV - Log Current View";

	@Parameter
	BdvHandle bdvh;

	@Override
	public void run()
	{
		new Thread( () -> {

			final int timepoint = bdvh.getViewerPanel().state().getCurrentTimepoint();

			// position
			final PositionViewerTransform positionViewerTransform = new PositionViewerTransform( BdvUtils.getGlobalMouseCoordinates( bdvh ).positionAsDoubleArray(), timepoint );

			// affine
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			bdvh.getViewerPanel().state().getViewerTransform( affineTransform3D );
			final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( affineTransform3D.getRowPackedCopy(), timepoint );

			// normalized affine
			final AffineTransform3D normalisedViewerTransform = Utils.createNormalisedViewerTransform( bdvh, Utils.getMousePosition( bdvh ) );
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
