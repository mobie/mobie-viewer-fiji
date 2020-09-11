package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;

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

		if ( bookmark.layers != null && bookmark.layers.size() > 0 )
		{
			sourcesPanel.removeAllSourcesFromPanelAndViewers();
			addSourcesToPanelAndViewer( bookmark );
		}

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
				sourcesPanel.addSourceToPanelAndViewer( metadata.displayName );
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

	public void adaptViewerTransform( Bookmark bookmark )
	{
		final Location location = getLocation( bookmark, sourcesPanel.getBdv() );

		if ( location == null ) return; // not all Bookmarks have a location, some are just layers

		BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location );
	}

	public static Location getLocation( Bookmark bookmark, BdvHandle bdv )
	{
		if ( bookmark.normView != null )
		{
			return new Location( LocationType.NormalisedViewerTransform, bookmark.normView );
		}
		else if ( bookmark.view != null  )
		{
			return new Location( LocationType.ViewerTransform, bookmark.view );
		}
		else if ( bookmark.position != null )
		{
			return new Location( LocationType.Position3d, bookmark.position );
		}
		else
		{
			if ( bookmark.name.equals( "default" ) )
			{
				bookmark.normView = Utils.createNormalisedViewerTransform( bdv ).getRowPackedCopy();
				return new Location( LocationType.NormalisedViewerTransform, bookmark.normView );
			}
			else
			{
				return null;
			}
		}
	}

	public Set< String > getBookmarkNames()
	{
		return nameToBookmark.keySet();
	}
}
