package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileUtils.FileLocation;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.gui.GenericDialog;

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
				final SourceAndMetadata< ? > sam = new SourceAndMetadata(samDefault.source(), samDefault.metadata().copy());
				updateSourceMetadata(entry, sam.metadata());

				sourcesPanel.addSourceToPanelAndViewer( sam );
			}
		}
	}

	public void updateSourceMetadata( Map.Entry< String, MutableImageProperties > entry, Metadata sourceMetadata )
	{
		final ImagePropertiesToMetadataAdapter adapter = new ImagePropertiesToMetadataAdapter();
		adapter.setMetadataFromMutableImageProperties( sourceMetadata, entry.getValue() );
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
			Map<String, Bookmark> additionalBookmarks = bookmarksJsonParser.selectAndLoadBookmarks();
			if (additionalBookmarks != null) {
				nameToBookmark.putAll(additionalBookmarks);
				bookmarkDropDown.removeAllItems();
				for (String bookmarkName : nameToBookmark.keySet()) {
					bookmarkDropDown.addItem(bookmarkName);
				}
			}
	}

	public void saveCurrentSettingsAsBookmark () {
		NameAndLocation bookmarkNameAndLocation = bookmarkSaveDialog();
		Bookmark currentBookmark = createBookmarkFromCurrentSettings(bookmarkNameAndLocation.name);
		ArrayList<Bookmark> bookmarks = new ArrayList<>();
		bookmarks.add(currentBookmark);

		if ( bookmarkNameAndLocation.location.equals( FileLocation.Project ) &&
				bookmarksJsonParser.getDatasetLocation().contains( "raw.githubusercontent" )) {
			bookmarksJsonParser.saveBookmarksToGithub(bookmarks);
		} else {
			try {
				bookmarksJsonParser.saveBookmarksToFile(bookmarks, bookmarkNameAndLocation.location);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class NameAndLocation {
		String name;
		FileLocation location;
	}

	private NameAndLocation bookmarkSaveDialog () {
		FileLocation fileLocation = null;
		String bookmarkName = null;
		final GenericDialog gd = new GenericDialog( "Choose save location" );
		gd.addStringField("Bookmark Name", "name");
		gd.addChoice( "Save to", new String[]{ FileLocation.Project.toString(),
				FileLocation.File_system.toString() }, FileLocation.Project.toString() );
		gd.showDialog();

		if ( gd.wasCanceled() ) return null;
		bookmarkName = gd.getNextString();
		fileLocation = FileLocation.valueOf( gd.getNextChoice() );

		NameAndLocation bookmarkNameAndLocation = new NameAndLocation();
		bookmarkNameAndLocation.name = bookmarkName;
		bookmarkNameAndLocation.location = fileLocation;

		return bookmarkNameAndLocation;
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
