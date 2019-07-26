package publication;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;

public class RegistrationVisualisationMuscles
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.2.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		final PlatyBrowserSourcesPanel sourcesPanel = platyBrowser.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-muscles" );
		sourcesPanel.addSourceToPanelAndViewer( "prospr-6dpf-1-whole-MHCL4-MED" );

		sourcesPanel.setSourceColor( "sbem-6dpf-1-whole-segmented-muscles", new Color( 255,0,255,255) );
		sourcesPanel.setSourceColor( "prospr-6dpf-1-whole-MHCL4-MED", new Color( 0,255,0,255) );

		platyBrowser.getActionPanel().setSliceView( "View: (-0.747871017528087, -0.6676436522042726, 2.0869538546605892, 291.53347494758805, 1.4642245659643167, -1.7927905904117456, -0.04882433847545138, 337.95348110873715, 1.6300830832316395, 1.3040664665853112, 1.0013367511280067, -535.9999606550252)" );

		// TODO: get this via keyboard shortcut
		platyBrowser.getActionPanel().setVolumeView("center =  1.0 0.0 0.0 122.88 0.0 1.0 0.0 122.88 0.0 0.0 1.0 140.40001 0.0 0.0 0.0 1.0\n" +
						"translate =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0\n" +
						"rotate =  -0.2545095 0.66575783 0.701421 0.0 -0.14783792 -0.7435648 0.65211606 0.0 0.9557033 0.062273122 0.28766856 0.0 0.0 0.0 0.0 1.0\n" +
						"zoom =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 410.15137 0.0 0.0 0.0 1.0\n" +
						"animate =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0\n"
		);

	}
}
