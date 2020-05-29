package publication.platybrowser;

import de.embl.cba.mobie.viewer.MoBIEViewer;
import de.embl.cba.mobie.viewer.SourcesPanel;
import net.imagej.ImageJ;

import java.awt.*;

public class RegistrationVisualisationNeuropil
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"/Volumes/arendt/EM_6dpf_segmentation/platy-browser-data/data",
				"https://git.embl.de/tischer/platy-browser-tables/raw/dev/data" );

		final SourcesPanel sourcesPanel = moBIEViewer.getSourcesPanel();

		sourcesPanel.addSourceToPanelAndViewer( "prospr-6dpf-1-whole-segmented-aridane-Neuropil" );
		sourcesPanel.addSourceToPanelAndViewer( "prospr-6dpf-1-whole-Glt1-MED" );

		sourcesPanel.setSourceColor( "prospr-6dpf-1-whole-segmented-aridane-Neuropil", new Color( 255,0,255,255) );
		sourcesPanel.setSourceColor( "prospr-6dpf-1-whole-Glt1-MED", new Color( 0,255,0,255) );


		// OVERVIEW

		moBIEViewer.getActionPanel().setView( "View: (-0.7478710175280877, -0.6676436522042725, 2.086953854660592, 291.5334749475883, 1.4642245659643176, -1.7927905904117467, -0.048824338475451055, 337.95348110873744, 1.6300830832316395, 1.3040664665853117, 1.0013367511280067, -516.0999606550258)" );

		// TODO: get this via keyboard shortcut
		moBIEViewer.getActionPanel().setVolumeView("center =  1.0 0.0 0.0 122.88 0.0 1.0 0.0 122.88 0.0 0.0 1.0 140.40001 0.0 0.0 0.0 1.0\n" +
				"translate =  1.0 0.0 0.0 0.117842264 0.0 1.0 0.0 0.15685147 0.0 0.0 1.0 -0.52159584 0.0 0.0 0.0 1.0\n" +
				"rotate =  -0.22049984 -0.6480566 -0.72897357 0.0 -0.22329058 0.76105946 -0.60904 0.0 0.94948465 0.028479699 -0.31251845 0.0 0.0 0.0 0.0 1.0\n" +
				"zoom =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 358.10345 0.0 0.0 0.0 1.0\n" +
				"animate =  1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0\n"
		);


		// DETAIL ZOOM
		// View: (-5.457887183190528, 6.892455295688354, -0.09128822640025808, 243.5120593221493, -3.0323636942634073, -2.2962312393397, 7.926846697708436, 869.0433860416233, 6.19023779584073, 4.952190236672585, 3.8025746460164447, -2153.870206057355)





	}
}
