package de.embl.cba.mobie.ui;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.bookmark.BookmarkManager;
import de.embl.cba.mobie.bookmark.Location;
import de.embl.cba.mobie.bookmark.LocationType;
import de.embl.cba.mobie.platybrowser.GeneSearch;
import de.embl.cba.mobie.universe.UniverseConfigurationDialog;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie.utils.ui.BdvTextOverlay;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;

public class BdvBehaviourInstaller
{
	private static final String RESTORE_DEFAULT_VIEW_TRIGGER = "ctrl R";

	private final MoBIE moBIE;
	private final BdvHandle bdv;
	private final BookmarkManager bookmarkManager;

	public BdvBehaviourInstaller( MoBIE moBIE, BdvHandle bdv )
	{
		this.moBIE = moBIE;
		this.bookmarkManager = moBIE.getBookmarkManager();
		this.bdv = bdv;

		install();
	}

	private void install( )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "MoBIE behaviours" );

		// TODO: Use BDV-Playground instead
		BdvPopupMenus.addScreenshotAction( bdv );

		BdvPopupMenus.addAction( bdv, "Log Current Location",
				() -> {
					new Thread( () -> {
						Logger.log( "\nPosition:\n" + BdvUtils.getGlobalMousePositionString( bdv ) );
						Logger.log( "View:\n" + BdvUtils.getBdvViewerTransformString( bdv ) );
						Logger.log( "Normalised view:\n" + Utils.createNormalisedViewerTransformString( bdv, Utils.getMousePosition( bdv ) ) );
					} ).start();
				});

		BdvPopupMenus.addAction( bdv, "Load Additional Bookmarks",
				() -> {
					new Thread( () -> {
						SwingUtilities.invokeLater( () -> bookmarkManager.loadAdditionalBookmarks() );
					} ).start();
				});

		BdvPopupMenus.addAction( bdv, "Save Current Settings As Bookmark",
				() -> {
					new Thread( () -> {
						SwingUtilities.invokeLater( () -> bookmarkManager.saveCurrentSettingsAsBookmark() );
					} ).start();
				});

		BdvPopupMenus.addAction( bdv, "Restore Default View" + BdvUtils.getShortCutString( RESTORE_DEFAULT_VIEW_TRIGGER ) ,
				() -> new Thread( () -> restoreDefaultView() ).start() );

		BdvPopupMenus.addAction( bdv, "Configure 3D View...",
				() -> new Thread( () -> new UniverseConfigurationDialog( moBIE.getSourcesDisplayManager() ).showDialog() ).start() );

		if ( moBIE.getProjectLocation().contains( "platybrowser" ) )
		{
			BdvPopupMenus.addAction( bdv, "Search Genes...", ( x, y ) ->
			{
				double[] micrometerPosition = new double[ 3 ];
				BdvUtils.getGlobalMouseCoordinates( bdv ).localize( micrometerPosition );

				final BdvTextOverlay bdvTextOverlay
						= new BdvTextOverlay( bdv,
						"Searching expressed genes; please wait...", micrometerPosition );

				new Thread( () ->
				{
					final GeneSearch geneSearch = new GeneSearch( 3.0, micrometerPosition, moBIE.getSourcesModel() );
					geneSearch.searchGenes();
					bdvTextOverlay.setText( "" );
				}
				).start();
			} );
		}

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			(new Thread( () -> {
				restoreDefaultView();
			} )).start();
		}, "Toggle point overlays", RESTORE_DEFAULT_VIEW_TRIGGER ) ;
	}

	private void restoreDefaultView()
	{
		final Location location = new Location( LocationType.NormalisedViewerTransform, moBIE.getDefaultNormalisedViewerTransform().getRowPackedCopy() );
		BdvViewChanger.moveToLocation( bdv, location );
	}
}
