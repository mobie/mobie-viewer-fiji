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
import de.embl.cba.mobie.image.SourceGroupLabelSourceCreator;
import de.embl.cba.mobie.image.SourceGroups;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.mobie2.bdv.BdvViewChanger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils.FileLocation;
import de.embl.cba.tables.image.SourceAndMetadata;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.IntType;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class BookmarkManager
{
	private final SourcesDisplayManager sourcesDisplayManager;
	private final MoBIE moBIE;
	private final Map< String, Bookmark > nameToBookmark;
	private final BookmarkReader bookmarkReader;
	private JComboBox<String> bookmarkDropDown;
	private final String datasetLocation;

	public BookmarkManager( MoBIE moBIE, Map< String, Bookmark > nameToBookmark, BookmarkReader bookmarkReader )
	{
		this.moBIE = moBIE;
		this.sourcesDisplayManager = moBIE.getSourcesDisplayManager();
		this.nameToBookmark = nameToBookmark;
		this.bookmarkReader = bookmarkReader;
		this.datasetLocation = bookmarkReader.getDatasetLocation();
	}

	public void setBookmarkDropDown ( JComboBox<String> bookmarkDropDown) {
		this.bookmarkDropDown = bookmarkDropDown;
	}

	public void setView( String bookmarkId )
	{
		final Bookmark bookmark = nameToBookmark.get( bookmarkId );

		if ( bookmark.layers != null && bookmark.layers.size() > 0 )
		{
			sourcesDisplayManager.removeAllSourcesFromViewers();
			show( bookmark );
		}

		// note: if this is trying to restore the default bookmark
		// it may not do anything because bdv already automatically
		// adapts the viewer transform when restoring the default view
		// in case only one source was added
		adaptViewerTransform( bookmark );
	}

	public void show( Bookmark bookmark )
	{
		final HashMap< String, SourceAndMetadata > sourcesAndMetadata = createSourcesAndMetadata( bookmark );

		if ( bookmark.layouts != null )
		{
			for ( String layoutName : bookmark.layouts.keySet() )
			{
				final Layout layout = bookmark.layouts.get( layoutName );

				final String name = bookmark.name + "-" + layoutName;
				adjustMetadata( sourcesAndMetadata, layout, name );

				final SourceGroupLabelSourceCreator creator = new SourceGroupLabelSourceCreator( sourcesAndMetadata, name + "-labels", layout );
				final SourceAndMetadata< IntType > sam = creator.create();
				// FileAndUrlUtils.combinePath( tableDataLocation,
				sam.metadata().segmentsTablePath = FileAndUrlUtils.combinePath( moBIE.getTablesLocation(), layout.sourceTable );

				sourcesDisplayManager.show( sam );
			}
		}

		for ( SourceAndMetadata< ? > sam : sourcesAndMetadata.values() )
		{
			// display the source
			sourcesDisplayManager.show( sam );

			if ( sam.metadata().groupId != null )
				SourceGroups.addSourceToGroup( sam );
		}
	}

	// TODO: Make own Layout class
	protected void adjustMetadata( HashMap< String, SourceAndMetadata > sourceNameToSourceAndMetadata, Layout layout, String groupName )
	{
		for ( String layer : layout.layers )
		{
			final SourceAndMetadata sourceAndMetadata = sourceNameToSourceAndMetadata.get( layer );
			sourceAndMetadata.metadata().groupId = groupName;
		}

		if ( layout.layoutType.equals( LayoutType.AutoGrid ) )
		{
			final int numSources = layout.layers.size();
			final int numColumns = ( int ) Math.ceil( Math.sqrt( numSources ) );
			FinalRealInterval bounds = Utils.estimateBounds( sourceNameToSourceAndMetadata.get( layout.layers.get( 0 ) ).source() );
			final double spacingFactor = 0.1;
			double border = spacingFactor * ( bounds.realMax( 0 ) - bounds.realMin( 0 ) );
			double offsetX = 0;
			double offsetY = 0;

			for ( int sourceIndex = 0, columnIndex = 0; sourceIndex < numSources; sourceIndex++ )
			{
				final SourceAndMetadata sam = sourceNameToSourceAndMetadata.get( layout.layers.get( sourceIndex ) );

				final AffineTransform3D transform3D = new AffineTransform3D();
				transform3D.translate( offsetX, offsetY, 0 );
				sam.metadata().addedTransform = transform3D.getRowPackedCopy();

				if ( ++columnIndex == numColumns )
				{
					offsetY += bounds.realMax( 1 ) + border;
					offsetX = 0;
					columnIndex = 0;
				}
				else
				{
					offsetX += bounds.realMax( 1 ) + border;
				}
			}
		}
	}

	private HashMap< String, SourceAndMetadata > createSourcesAndMetadata( Bookmark bookmark )
	{
		final HashMap< String, SourceAndMetadata > sourceNameToSourceAndMetadata = new HashMap<>();
		for ( String sourceName : bookmark.layers.keySet() )
		{
			if ( sourcesDisplayManager.getVisibleSourceNames().contains( sourceName ) )
				continue;

			final Source< ? > source
					= sourcesDisplayManager.getSourceAndDefaultMetadata( sourceName ).source();

			final Metadata metadata
					= sourcesDisplayManager.getSourceAndDefaultMetadata( sourceName ).metadata().copy();

			final MutableImageProperties mutableImageProperties
					= bookmark.layers.get( sourceName );

			// write mutable image properties from bookmark into default metadata
			updateSourceMetadata( metadata, mutableImageProperties );

			final SourceAndMetadata< ? > sam
					= new SourceAndMetadata( source, metadata );

			sourceNameToSourceAndMetadata.put( sourceName, sam );
		}

		return sourceNameToSourceAndMetadata;
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
			BdvViewChanger.moveToLocation( sourcesDisplayManager.getBdv(), location );
		}
	}

	public void loadAdditionalBookmarks() {
			Map<String, Bookmark> additionalBookmarks = bookmarkReader.selectAndReadBookmarks();
			if (additionalBookmarks != null) {
				nameToBookmark.putAll(additionalBookmarks);
				bookmarkDropDown.removeAllItems();
				for ( String bookmarkName : nameToBookmark.keySet()) {
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
		Set<String> visibleSourceNames = sourcesDisplayManager.getVisibleSourceNames();

		for (String sourceName : visibleSourceNames) {
			sourcesDisplayManager.updateCurrentMetadata( sourceName );
			MutableImageProperties imageProperties = getMutableImagePropertiesFromCurrentMetadata(sourceName);
			layers.put(sourceName, imageProperties);
		}

		BdvHandle bdv = sourcesDisplayManager.getBdv();
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
		Metadata metadata = sourcesDisplayManager.getSourceAndCurrentMetadata( sourceName ).metadata();

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
