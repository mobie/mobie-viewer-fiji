package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.bookmark.write.BookmarkFileWriter;
import de.embl.cba.mobie.bookmark.write.BookmarkWriter;
import de.embl.cba.mobie.bookmark.write.NameAndFileLocation;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileUtils.FileLocation;
import de.embl.cba.tables.image.SourceAndMetadata;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class BookmarkManager
{
	private final SourcesPanel sourcesPanel;
	private Map< String, Bookmark > nameToBookmark;
	private final BookmarkReader bookmarkReader;
	private JComboBox<String> bookmarkDropDown;
	private final String datasetLocation;

	public BookmarkManager( SourcesPanel sourcesPanel, Map< String, Bookmark > nameToBookmark,
							BookmarkReader bookmarkReader )
	{
		this.sourcesPanel = sourcesPanel;
		this.nameToBookmark = nameToBookmark;
		this.bookmarkReader = bookmarkReader;
		this.datasetLocation = bookmarkReader.getDatasetLocation();
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
		if ( bookmark.layout.equals( Layout.AutoGrid ) )
		{
			final int numSources = bookmark.layers.size();

			// bookmark.layers. // adjust the addedTransform
		}


		for ( String sourceName : bookmark.layers.keySet() )
		{
			if ( sourcesPanel.getVisibleSourceNames().contains( sourceName ) )
				return;

			final Source< ? > source
					= sourcesPanel.getSourceAndDefaultMetadata( sourceName ).source();

			final Metadata metadata
					= sourcesPanel.getSourceAndDefaultMetadata( sourceName ).metadata().copy();
			final MutableImageProperties mutableImageProperties
					= bookmark.layers.get( sourceName );

			// write mutable image properties from bookmark into default metadata
			updateSourceMetadata( metadata, mutableImageProperties );

			final SourceAndMetadata< ? > sam
					= new SourceAndMetadata( source, metadata );

			sourcesPanel.addSourceToPanelAndViewer( sam );
		}
	}

	public void updateSourceMetadata( Metadata sourceMetadata, MutableImageProperties value )
	{
		final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
		adapter.setMetadataFromMutableImageProperties( sourceMetadata, value );
	}

	public void adaptViewerTransform( Bookmark bookmark )
	{
		final Location location = getLocationFromBookmark( bookmark );

		if ( location != null )
		{
			BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location );
		}
	}

	public void loadAdditionalBookmarks() {
			Map<String, Bookmark> additionalBookmarks = bookmarkReader.selectAndReadBookmarks();
			if (additionalBookmarks != null) {
				nameToBookmark.putAll(additionalBookmarks);
				bookmarkDropDown.removeAllItems();
				for (String bookmarkName : nameToBookmark.keySet()) {
					bookmarkDropDown.addItem(bookmarkName);
				}
			}
	}

	public void saveCurrentSettingsAsBookmark() {
		NameAndFileLocation bookmarkNameAndFileLocation = BookmarkWriter.bookmarkSaveDialog();
		Bookmark currentBookmark = createBookmarkFromCurrentSettings( bookmarkNameAndFileLocation.name);
		ArrayList<Bookmark> bookmarks = new ArrayList<>();
		bookmarks.add(currentBookmark);

		if ( bookmarkNameAndFileLocation.location.equals( FileLocation.Project ) &&
				datasetLocation.contains( "raw.githubusercontent" )) {
			BookmarkWriter.saveBookmarksToGithub(bookmarks, bookmarkReader );
		} else {
			try {
				BookmarkFileWriter.saveBookmarksToFile(bookmarks, bookmarkNameAndFileLocation.location, datasetLocation );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Bookmark createBookmarkFromCurrentSettings(String bookmarkName) {
		HashMap< String, MutableImageProperties > layers = new HashMap<>();
		Set<String> visibleSourceNames = sourcesPanel.getVisibleSourceNames();

		for (String sourceName : visibleSourceNames) {
			sourcesPanel.updateCurrentMetadata( sourceName );
			MutableImageProperties imageProperties = getMutableImagePropertiesFromCurrentMetadata(sourceName);
			layers.put(sourceName, imageProperties);
		}

		BdvHandle bdv = sourcesPanel.getBdv();
		Bookmark currentBookmark = new Bookmark();
		currentBookmark.name = bookmarkName;
		currentBookmark.layers = layers;
		double[] currentPosition = new double[3];
		BdvUtils.getGlobalMouseCoordinates(bdv).localize(currentPosition);
		currentBookmark.position = currentPosition;
		currentBookmark.normView = Utils.createNormalisedViewerTransformString( bdv, Utils.getMousePosition( bdv ) ).split(",");
		currentBookmark.view = null;

		return currentBookmark;
	}

	private MutableImageProperties getMutableImagePropertiesFromCurrentMetadata( String sourceName )
	{
		Metadata metadata = sourcesPanel.getSourceAndCurrentMetadata( sourceName ).metadata();

		MutableImageProperties imageProperties = new MutableImageProperties();
		final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
		adapter.setMutableImagePropertiesFromMetadata( imageProperties, metadata );

		return imageProperties;
	}

	public static Location getLocationFromBookmark( Bookmark bookmark )
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
