package publication;

import de.embl.cba.platynereis.platybrowser.PlatyBrowser;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import net.imagej.ImageJ;

import java.awt.*;

public class RegistrationVisualisationNuclei
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final PlatyBrowser platyBrowser = new PlatyBrowser(
				"0.2.1",
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		final PlatyBrowserSourcesPanel sourcesPanel = platyBrowser.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "sbem-6dpf-1-whole-segmented-nuclei-labels" );
		sourcesPanel.addSourceToPanelAndViewer( "prospr-6dpf-1-whole-Ref-SPM" );

		sourcesPanel.setSourceColor( "sbem-6dpf-1-whole-segmented-nuclei-labels", new Color( 255,0,255,255) );
		sourcesPanel.setSourceColor( "prospr-6dpf-1-whole-Ref-SPM", new Color( 0,255,0,255) );

		// OVERVIEW

		platyBrowser.getActionPanel().setView( "View: (-0.747871017528087, -0.6676436522042726, 2.0869538546605892, 291.53347494758805, 1.4642245659643167, -1.7927905904117456, -0.04882433847545138, 337.95348110873715, 1.6300830832316395, 1.3040664665853112, 1.0013367511280067, -535.9999606550252)" );

		platyBrowser.getActionPanel().setVolumeView("center =  1.0 0.0 0.0 122.88 0.0 1.0 0.0 122.88 0.0 0.0 1.0 140.40001 0.0 0.0 0.0 1.0\n" +
				"translate =  1.0 0.0 0.0 0.117842264 0.0 1.0 0.0 0.15685147 0.0 0.0 1.0 -0.52159584 0.0 0.0 0.0 1.0\n" +
				"rotate =  -0.22049984 -0.6480566 -0.72897357 0.0 -0.22329058 0.76105946 -0.60904 0.0 0.94948465 0.028479699 -0.31251845 0.0 0.0 0.0 0.0 1.0\n" +
				"zoom =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 358.10345 0.0 0.0 0.0 1.0\n" +
				"animate =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0\n"
		);


		// DETAIL ZOOM
		// View: (-6.099350109804703, -5.3990077927603535, 18.614782509183787, -202.73480935758062, 12.078494318540145, -16.320823077240274, -0.7760115017778567, 117.84084743329208, 15.158145929086958, 10.832494343760871, 8.108585761855771, -4294.781037288093)





	}
}
