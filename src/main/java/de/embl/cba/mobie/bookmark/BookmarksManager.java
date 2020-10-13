package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.image.SourceAndMetadata;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class BookmarksManager
{
	private final SourcesPanel sourcesPanel;
	private Map< String, Bookmark > nameToBookmark;
	private BookmarksJsonParser bookmarksJsonParser;
	private JComboBox<String> bookmarkDropDown;

	public BookmarksManager( SourcesPanel sourcesPanel, Map< String, Bookmark > nameToBookmark,
							 BookmarksJsonParser bookmarksJsonParser )
	{
		this.sourcesPanel = sourcesPanel;
		this.nameToBookmark = nameToBookmark;
		this.bookmarksJsonParser = bookmarksJsonParser;
	}

	public void setBookmarkDropDown (JComboBox<String> bookmarkDropDown) {
		this.bookmarkDropDown = bookmarkDropDown;
	}

	public void setView( String bookmarkId )
	{
		final Bookmark bookmark = nameToBookmark.get( bookmarkId );

		if ( bookmark.layers != null && bookmark.layers.size() > 0 )
		{
			sourcesPanel.removeAllSourcesFromPanelAndViewers();
			addSourcesToPanelAndViewer( bookmark );
		}

		// note: if this is trying to restore the default bookmark
		// it may not do anything because bdv already automatically
		// adapts the viewer transform when restoring the default view
		// in case only one source was added
		adaptViewerTransform( bookmark );
	}

	public void addSourcesToPanelAndViewer( Bookmark bookmark )
	{
		for ( Map.Entry< String, MutableImageProperties> entry : bookmark.layers.entrySet() )
		{
			final String sourceName = entry.getKey();
			if ( ! sourcesPanel.getVisibleSourceNames().contains( sourceName ) )
			{
				final SourceAndMetadata< ? > samDefault = sourcesPanel.getSourceAndDefaultMetadata( sourceName );
				final SourceAndMetadata< ? > samBookmark = new SourceAndMetadata(samDefault.source(), samDefault.metadata().copy());
				updateSourceMetadata(entry, samBookmark.metadata());

				// final Metadata metadata = getAndUpdateSourceMetadata( entry, sourceName );
				// new SourceAndMetadata<>()
				sourcesPanel.addSourceToPanelAndViewer( samBookmark );
			}
		}
	}

	public void updateSourceMetadata( Map.Entry< String, MutableImageProperties > entry, Metadata sourceMetadata )
	{
		// final Metadata metadata = sourcesPanel.getImageSourcesModel().sources().get( sourceName ).metadata();
		final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
		adapter.setMetadata( sourceMetadata, entry.getValue() );
	}

	public void adaptViewerTransform( Bookmark bookmark )
	{
		final BdvHandle bdv = sourcesPanel.getBdv();

		final Location location = getLocationFromBookmark( bookmark, bdv );

		if ( location != null )
		{
			BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location );
		}
	}

	public void loadAdditionalBookmarks() {
			Map<String, Bookmark> additionalBookmarks = bookmarksJsonParser.selectAndLoadBookmarks();
			nameToBookmark.putAll(additionalBookmarks);
			bookmarkDropDown.removeAllItems();
			for (String bookmarkName : nameToBookmark.keySet()) {
				bookmarkDropDown.addItem(bookmarkName);
			}
	}

	public void saveCurrentSettingsAsBookmark () {
		// TODO - make bookmark name user definable
		Bookmark currentBookmark = getBookmarkFromCurrentSettings("test");
		ArrayList<Bookmark> bookmarks = new ArrayList<>();
		bookmarks.add(currentBookmark);
		try {
			bookmarksJsonParser.saveBookmarks(bookmarks);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Bookmark getBookmarkFromCurrentSettings( String bookmarkName) {
		HashMap< String, MutableImageProperties > layers = new HashMap<>();
		Set<String> visibleSourceNames = sourcesPanel.getVisibleSourceNames();

		for (String sourceName : visibleSourceNames) {
			MutableImageProperties sourceImageProperties = sourcesPanel.getCurrentImageProperties(sourceName);
			layers.put(sourceName, sourceImageProperties);
		}

		BdvHandle bdv = sourcesPanel.getBdv();
		Bookmark currentBookmark = new Bookmark();
		currentBookmark.name = bookmarkName;
		currentBookmark.layers = layers;
		// TODO - add to bdv utils
		double[] currentPosition = new double[3];
		BdvUtils.getGlobalMouseCoordinates(bdv).localize(currentPosition);
		currentBookmark.position = currentPosition;
		currentBookmark.normView = Utils.createNormalisedViewerTransformString( bdv, Utils.getMousePosition( bdv ) ).split(",");
		currentBookmark.view = null;

		return currentBookmark;
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
