package de.embl.cba.mobie.bookmark;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.mobie.image.ImagePropertiesToMetadataAdapter;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.mobie.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.TableRowsTableView;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

import static de.embl.cba.mobie.ui.viewer.SourcesDisplayUI.getConverterSetups;

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

				sourcesPanel.addSourceToPanelAndViewer( samBookmark );
			}
		}
	}

	public void updateSourceMetadata( Map.Entry< String, MutableImageProperties > entry, Metadata sourceMetadata )
	{
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
		ArrayList<String> bookmarkNameAndLocation = bookmarkSaveDialog();
		Bookmark currentBookmark = getBookmarkFromCurrentSettings(bookmarkNameAndLocation.get(0));
		ArrayList<Bookmark> bookmarks = new ArrayList<>();
		bookmarks.add(currentBookmark);

		if (bookmarkNameAndLocation.get(1) == FileUtils.PROJECT &&
				bookmarksJsonParser.getDatasetLocation().contains( "raw.githubusercontent" )) {
			bookmarksJsonParser.saveBookmarkToGithub(currentBookmark);
		} else {
			try {
				bookmarksJsonParser.saveBookmarksToFile(bookmarks, bookmarkNameAndLocation.get(1));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ArrayList<String> bookmarkSaveDialog () {
		String fileLocation = null;
		String bookmarkName = null;
		final GenericDialog gd = new GenericDialog( "Choose save location" );
		gd.addStringField("Bookmark Name", "name");
		gd.addChoice( "Save to", new String[]{FileUtils.PROJECT, FileUtils.FILE_SYSTEM }, FileUtils.PROJECT );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		bookmarkName = gd.getNextString();
		fileLocation = gd.getNextChoice();
		System.out.println(bookmarkName);
		System.out.println(fileLocation);

		ArrayList<String> bookmarkNameandLocation = new ArrayList<>();
		bookmarkNameandLocation.add(bookmarkName);
		bookmarkNameandLocation.add(fileLocation);
		return bookmarkNameandLocation;
	}

	public Bookmark getBookmarkFromCurrentSettings( String bookmarkName) {
		HashMap< String, MutableImageProperties > layers = new HashMap<>();
		Set<String> visibleSourceNames = sourcesPanel.getVisibleSourceNames();

		for (String sourceName : visibleSourceNames) {
			MutableImageProperties sourceImageProperties = getCurrentImageProperties(sourceName);
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

	private MutableImageProperties getCurrentImageProperties(String sourceName) {
		MutableImageProperties sourceImageProperties = new MutableImageProperties();
		Metadata sourceMetadata = sourcesPanel.getSourceAndCurrentMetadata(sourceName).metadata();

		ARGBType color = sourceMetadata.bdvStackSource.getConverterSetups().get(0).getColor();
		sourceImageProperties.color = color.toString();

		if (sourcesPanel.getSourceNameToLabelViews().containsKey(sourceName)) {
			TableRowsTableView<TableRowImageSegment> sourceTableRowsTableView = sourceMetadata.views.getTableRowsTableView();

			if (!sourceMetadata.views.getSegmentsBdvView().isLabelMaskShownAsBinaryMask()) {
				sourceImageProperties.color = sourceTableRowsTableView.getColoringLUTName();
				sourceImageProperties.colorByColumn = sourceTableRowsTableView.getColoringColumnName();
				sourceImageProperties.valueLimits = sourceTableRowsTableView.getColorByColumnValueLimits();
			}

			ArrayList<TableRowImageSegment> selectedSegments = sourceTableRowsTableView.getSelectedLabelIds();
			if (selectedSegments != null) {
				ArrayList<Double> selectedLabelIds = new ArrayList<>();
				for (TableRowImageSegment segment : selectedSegments) {
					selectedLabelIds.add(segment.labelId());
				}
				sourceImageProperties.selectedLabelIds = selectedLabelIds;
			}

			ArrayList<String> additionalTables = sourceTableRowsTableView.getAdditionalTables();
			if (additionalTables != null & additionalTables.size() > 0 ) {
				sourceImageProperties.tables = new ArrayList<>();
				// ensure tables are unique
				for (String tableName : sourceTableRowsTableView.getAdditionalTables()) {
					if (!sourceImageProperties.tables.contains(tableName)) {
						sourceImageProperties.tables.add(tableName);
					}
				}
			}

			sourceImageProperties.showSelectedSegmentsIn3d = sourceMetadata.views.getSegments3dView().getShowSelectedSegmentsIn3D();
		}

		if (sourceMetadata.content != null) {
			if (sourceMetadata.content.isVisible()) {
				sourceImageProperties.showImageIn3d = true;
			} else {
				sourceImageProperties.showImageIn3d = false;
			}
		} else {
			sourceImageProperties.showImageIn3d = false;
		}

		double[] currentContrastLimits = new double[2];
		currentContrastLimits[0] = getConverterSetups( sourceMetadata.bdvStackSource ).get(0).getDisplayRangeMin();
		currentContrastLimits[1] = getConverterSetups( sourceMetadata.bdvStackSource ).get(0).getDisplayRangeMax();
		sourceImageProperties.contrastLimits = currentContrastLimits;

		return sourceImageProperties;
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
