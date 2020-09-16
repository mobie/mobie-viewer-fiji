package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;

import java.util.Arrays;
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
		final BdvHandle bdv = sourcesPanel.getBdv();

		final Location location = getLocationFromBookmark( bookmark, bdv );

		if ( bookmark.name.equals( "default" ) && location == null )
		{
			// remember current view for users to come back to it
			bookmark.normView = Utils.createNormalisedViewerTransformStringArray( bdv );

			// below code is needed to force the deadlock during switching of datasets
//			final Location location2 = getLocationFromBookmark( bookmark, bdv );
//			BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location2 );
		}
		else if ( location != null )
		{
			BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location );
		}
	}

	public static Location getLocationFromBookmark( Bookmark bookmark, BdvHandle bdv )
	{
		if ( bookmark.normView != null )
		{
			final double[] doubles = Arrays.stream( bookmark.normView ).mapToDouble( x -> Double.parseDouble( x.replace( "n", "" ) ) ).toArray();

			return new Location( LocationType.NormalisedViewerTransform, doubles );
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
			return null;
		}
	}

	public Set< String > getBookmarkNames()
	{
		return nameToBookmark.keySet();
	}
}
