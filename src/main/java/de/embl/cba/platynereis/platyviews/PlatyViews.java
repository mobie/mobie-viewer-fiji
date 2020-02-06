package de.embl.cba.platynereis.platyviews;


import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import de.embl.cba.platynereis.utils.BdvViewChanger;
import de.embl.cba.platynereis.utils.Version;

import java.util.Map;

/**
 * TODO: probably read this from a editable text file so that users can add own views.
 *
 *
 */
public class PlatyViews
{
	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final String versionString;
	private Map< String, Bookmark > nameToBookmark;

	public PlatyViews( PlatyBrowserSourcesPanel sourcesPanel, String viewsSourcePath )
	{
		this( sourcesPanel, viewsSourcePath, "0.0.0" );
	}

	public PlatyViews( PlatyBrowserSourcesPanel sourcesPanel, String viewsSourcePath, String versionString )
	{
		this.sourcesPanel = sourcesPanel;
		this.versionString = versionString;
		final Version version = new Version( versionString );

		nameToBookmark = new BookmarkParser( viewsSourcePath ).call();
	}


	public void setView( String bookmarkId )
	{
		final Bookmark bookmark = nameToBookmark.get( bookmarkId );
		adaptViewerTransform( bookmark );

	}

	public void adaptViewerTransform( Bookmark bookmark )
	{
		if ( bookmark.transform != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.transform );
		else if ( bookmark.position != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.position );

		for ( Metadata metadata : bookmark.imageLayers )
		{
			sourcesPanel.addSourceToPanelAndViewer( metadata );
		}
	}
}
