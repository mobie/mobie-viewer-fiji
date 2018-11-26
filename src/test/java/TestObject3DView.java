import bdv.util.Bdv;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.PlatyBrowser;
import de.embl.cba.platynereis.Utils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.logic.BitType;
import org.scijava.vecmath.Color3f;

import java.util.ArrayList;

public class TestObject3DView
{
	public static void main( String[] args )
	{

		new ImageJ();

//		final ImagePlus imagePlus = IJ.openImage( "/Users/tischer/Documents/fiji-plugin-platynereisViewer/src/test/resources/mri-stack.zip" );
//		Image3DUniverse univ = new Image3DUniverse( );
//		univ.show( );
//		final Content content = univ.addMesh( imagePlus, null, "somename", 100, new boolean[]{ true, true, true }, 2 );

		final PlatyBrowser platyBrowser = new PlatyBrowser( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr" );
		platyBrowser.getMainFrame().getBdvSourcesPanel().addSourceToViewerAndPanel( "em-segmented-cells-gut-labels" );

		Bdv bdv = platyBrowser.getBdv();

		BdvUtils.centerBdvViewToPosition(
				bdv,
				new double[]{222,157,57},
				10 );

//		final ArrayList< RandomAccessibleInterval< BitType > > masks = new ArrayList<>();
//		final ArrayList< double[] > calibrations = new ArrayList<>();
//
//		Utils.log( "Extracing object..." );
//		BdvUtils.extractSelectedObject( bdv, new RealPoint( new float[]{222,157,57} ), 2, masks, calibrations );
//
//		Utils.log( "Create and show imageplus..." );
//		final ImagePlus mask = Utils.asImagePlus( masks.get( 0 ), calibrations.get( 0 ) );
//		final ImagePlus duplicate = mask.duplicate();
//
//		Utils.log( "Create and show in 3d viewer..." );
//		Image3DUniverse univ = new Image3DUniverse( );
//		univ.show( );
//		final Content content = univ.addMesh( duplicate, null, "object", 250, new boolean[]{ true, true, true }, 2 );
//		content.setColor( new Color3f(1.0f, 1.0f, 1.0f ) );

	}
}
