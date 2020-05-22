package de.embl.cba.platynereis.bookmark;


import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.platynereis.image.MutableImageProperties;
import de.embl.cba.platynereis.platybrowser.SourcesPanel;
import de.embl.cba.platynereis.bdv.BdvViewChanger;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.util.Map;
import java.util.Set;

public class BookmarksManager
{
	private final SourcesPanel sourcesPanel;
	private Map< String, Bookmark > nameToBookmark;

	public BookmarksManager( SourcesPanel sourcesPanel, Map< String, Bookmark > nameToBookmark )
	{
		this.sourcesPanel = sourcesPanel;
		this.nameToBookmark = nameToBookmark;
	}

	public void setView( String bookmarkId )
	{
		final Bookmark bookmark = nameToBookmark.get( bookmarkId );

		if ( bookmark.layers.size() > 0 )
		{
			sourcesPanel.removeAllSourcesFromPanelAndViewers();
		}

		addSourcesToPanelAndViewer( bookmark );
		adaptViewerTransform( bookmark );
	}

	public void addSourcesToPanelAndViewer( Bookmark bookmark )
	{
		for ( Map.Entry< String, MutableImageProperties> entry : bookmark.layers.entrySet() )
		{
			final String sourceName = entry.getKey();
			if ( ! sourcesPanel.getVisibleSourceNames().contains( sourceName ) )
			{
				final Metadata metadata = getAndUpdateSourceMetadata( entry, sourceName );
				sourcesPanel.addSourceToPanelAndViewer( metadata );
			}

		}
	}

	public Metadata getAndUpdateSourceMetadata( Map.Entry< String, MutableImageProperties > entry, String sourceName )
	{
		final Metadata metadata = sourcesPanel.getImageSourcesModel().sources().get( sourceName ).metadata();
		final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
		adapter.setMetadata( metadata, entry.getValue() );
		return metadata;
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
		if ( bookmark.view != null )
			BdvViewChanger.moveToDoubles( sourcesPanel.getBdv(), bookmark.view );

		if ( bookmark.position != null )
		{
			BdvUtils.moveToPosition( sourcesPanel.getBdv(), bookmark.position, 0, 3000 );
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
