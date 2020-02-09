package de.embl.cba.platynereis.platyviews;


import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import de.embl.cba.platynereis.utils.BdvViewChanger;
import de.embl.cba.platynereis.utils.Version;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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
		removeAllSourcesFromPanelAndViewer( bookmark );
		addBookmarkSourcesToPanelAndViewer( bookmark );
		adaptViewerTransform( bookmark );
	}

	public void addBookmarkSourcesToPanelAndViewer( Bookmark bookmark )
	{
		for ( Metadata metadata : bookmark.nameToMetadata.values() )
		{
			if ( ! sourcesPanel.getVisibleSourceNames().contains( metadata.displayName ) )
				sourcesPanel.addSourceToPanelAndViewer( metadata );
		}
	}

	public void removeAllSourcesFromPanelAndViewer( Bookmark bookmark )
	{
		// TODO: maybe do not remove the ones that we want to keep seeing,
		//  however it is a bit of coding work to then change the display settings for only those.

		if ( bookmark.nameToMetadata.size() > 0 )
		{
			final Set< String > visibleSourceNames = sourcesPanel.getVisibleSourceNames();

			for ( String visibleSourceName : visibleSourceNames )
				sourcesPanel.removeSourceFromPanelAndViewers( visibleSourceName );
		}
	}

	public void adaptViewerTransform( Bookmark bookmark )
	{
		if ( bookmark.transform != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.transform );
		else if ( bookmark.position != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.position );
	}

	public Set< String > getBookmarkNames()
	{
		return nameToBookmark.keySet();
	}
}
