package de.embl.cba.platynereis.platyviews;


import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.capture.BdvViewCaptures;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.platybrowser.PlatyBrowserSourcesPanel;
import de.embl.cba.platynereis.bdv.BdvViewChanger;

import java.util.Map;
import java.util.Set;

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
		nameToBookmark = new BookmarkParser( viewsSourcePath, sourcesPanel.getImageSourcesModel() ).call();
	}

	public void setView( String bookmarkId )
	{
		final Bookmark bookmark = nameToBookmark.get( bookmarkId );
		removeAllSourcesFromPanelAndViewer( bookmark );
		addSourcesToPanelAndViewer( bookmark );
		adaptViewerTransform( bookmark );
	}

	public void addSourcesToPanelAndViewer( Bookmark bookmark )
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
		// however it is a bit of coding work to then change the display settings for only those.

		if ( bookmark.nameToMetadata.size() > 0 )
		{
			final Set< String > visibleSourceNames = sourcesPanel.getVisibleSourceNames();

			for ( String visibleSourceName : visibleSourceNames )
				sourcesPanel.removeSourceFromPanelAndViewers( visibleSourceName );
		}
	}

	/**
	 * The transform is specific to the size of the
	 * Bdv window, leading to views appearing off centre.
	 * Thus, if given, we also move to center the position.
	 *
	 * @param bookmark
	 */
	public void adaptViewerTransform( Bookmark bookmark )
	{
		if ( bookmark.transform != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.transform );

		if ( bookmark.position != null )
		{
			BdvUtils.moveToPosition( sourcesPanel.getBdv(),
					bookmark.position.stream().mapToDouble( d -> d ).toArray(),
					0, 100 );
//			BdvViewChanger.enablePointOverlay( false );
//			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.position );
//			BdvViewChanger.enablePointOverlay( true );
		}
	}

	public Set< String > getBookmarkNames()
	{
		return nameToBookmark.keySet();
	}
}
