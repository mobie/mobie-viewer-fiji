package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.Sources;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import ij.gui.GenericDialog;

import java.io.IOException;
import java.util.*;

public class BookmarksManager
{
	private final SourcesPanel sourcesPanel;
	private Map< String, Bookmark > nameToBookmark;
	private BookmarksJsonParser bookmarksJsonParser;
	private String PROJECT = "Project";
	private String FILE_SYSTEM = "File system";

	public BookmarksManager( SourcesPanel sourcesPanel, Map< String, Bookmark > nameToBookmark,
							 BookmarksJsonParser bookmarksJsonParser )
	{
		this.sourcesPanel = sourcesPanel;
		this.nameToBookmark = nameToBookmark;
		this.bookmarksJsonParser = bookmarksJsonParser;
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

		if ( location != null )
		{
			BdvViewChanger.moveToLocation( sourcesPanel.getBdv(), location );
		}
	}

	// public void loadAdditionalBookmarks(String bookmarksDirectory) {
	// 	String bookmarksLocation = null;
	// 	if ( bookmarksDirectory != null )
	// 	{
	// 		final GenericDialog gd = new GenericDialog( "Choose bookmarks source" );
	// 		gd.addChoice( "Load bookmarks from", new String[]{ PROJECT, FILE_SYSTEM }, PROJECT );
	// 		gd.showDialog();
	// 		if ( gd.wasCanceled() ) return null;
	// 		bookmarksLocation = gd.getNextChoice();
	// 	}
	//
	// 	String bookmarksPath = null;
	// 	if ( bookmarksDirectory != null && bookmarksLocation.equals( PROJECT ) && bookmarksDirectory.contains( "raw.githubusercontent" ) )
	// 	{
	// 		tablesPath = selectGitHubTablePath( tablesDirectory );
	// 		if ( tablesPath == null ) return null;
	// 	}
	// 	else
	// 	{
	// 		final JFileChooser jFileChooser = new JFileChooser( tablesDirectory );
	//
	// 		if ( jFileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
	// 			tablesPath = jFileChooser.getSelectedFile().getAbsolutePath();
	// 	}
	//
	// 	if ( tablesPath == null ) return null;
	//
	// 	if ( tablesPath.startsWith( "http" ) )
	// 		tablesPath = resolveTableURL( URI.create( tablesPath ) );
	//
	// 	Map< String, List< String > > columns = TableColumns.openAndOrderNewColumns( table, mergeByColumnName, tablesPath );
	//
	// }

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
			Metadata sourceMetadata = sourcesPanel.getSourceAndMetadata(sourceName).metadata();
			MutableImageProperties sourceImageProperties = new MutableImageProperties();
			new ImagePropertiesToMetadataAdapter().setImageProperties(sourceMetadata, sourceImageProperties);
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
