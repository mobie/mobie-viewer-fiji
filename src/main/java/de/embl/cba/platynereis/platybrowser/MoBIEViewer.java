package de.embl.cba.platynereis.platybrowser;

import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.platysources.PlatyBrowserImageSourcesModel;
import de.embl.cba.platynereis.platyviews.Bookmark;
import de.embl.cba.platynereis.platyviews.BookmarkParser;
import de.embl.cba.platynereis.platyviews.BookmarkManager;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import de.embl.cba.platynereis.utils.Utils;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MoBIEViewer extends JFrame
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final PlatyBrowserActionPanel actionPanel;
	private final PlatyBrowserImageSourcesModel imageSourcesModel;
	private String imageDataLocation;
	private String tableDataLocation;

	private int frameWidth;
	private BookmarkManager bookmarkManager;

	public MoBIEViewer(
			String dataSet,
			String imageDataLocation,
			String tableDataLocation ) throws HeadlessException
	{
		configureDataLocations( dataSet, imageDataLocation, tableDataLocation );

		imageSourcesModel = new PlatyBrowserImageSourcesModel( this.imageDataLocation, this.tableDataLocation );

		sourcesPanel = new PlatyBrowserSourcesPanel( imageSourcesModel );

		fetchBookmarks( this.tableDataLocation );

		final double[] levelingVector = fetchLeveling( this.tableDataLocation );

		actionPanel = new PlatyBrowserActionPanel( sourcesPanel, bookmarkManager, levelingVector );

		setJMenuBar( createMenuBar() );
		showFrame( dataSet );
		adaptLogWindowPositionAndSize();

		sourcesPanel.setParentComponent( this );
		sourcesPanel.addSourceToPanelAndViewer( Constants.DEFAULT_EM_RAW_FILE_ID );

		actionPanel.setBdv( sourcesPanel.getBdv() );
	}

	private double[] fetchLeveling( String tableDataLocation )
	{
		// TODO
		return new double[]{0.70,0.56,0.43};
	}

	public void configureDataLocations( String dataSet, String aImageDataLocation, String aTableDataLocation )
	{
		this.imageDataLocation = aImageDataLocation;
		this.tableDataLocation = aTableDataLocation;

		imageDataLocation = FileAndUrlUtils.removeTrailingSlash( imageDataLocation );
		tableDataLocation = FileAndUrlUtils.removeTrailingSlash( tableDataLocation );

		adaptUrl( imageDataLocation );

		imageDataLocation = FileAndUrlUtils.combinePath( imageDataLocation, dataSet );
		tableDataLocation = FileAndUrlUtils.combinePath( tableDataLocation, dataSet );

		Utils.log( "");
		Utils.log( "# Fetching data");
		Utils.log( "Fetching image data from: " + imageDataLocation );
		Utils.log( "Fetching table data from: " + tableDataLocation );
	}

	public String adaptUrl( String url )
	{
		if ( url.contains( "github.com" ) )
		{
			url = url.replace( "github.com", "raw.githubusercontent.com" );
			url += "/master/data";
		}
		return url;
	}

	public void fetchBookmarks( String tableDataLocation )
	{
		Map< String, Bookmark > nameToBookmark = new BookmarkParser( tableDataLocation, imageSourcesModel ).call();

		bookmarkManager = new BookmarkManager( sourcesPanel, nameToBookmark );
	}

	public void adaptLogWindowPositionAndSize()
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( this.getLocationOnScreen().y + this.getHeight() + 20 );
			log.setSize( this.getWidth(), logWindowHeight  );
			log.setLocation( this.getLocationOnScreen().x, this.getLocationOnScreen().y + this.getHeight() );
		}
	}

	private JMenuBar createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		menuBar.add( createMainMenu() );
		return menuBar;
	}

	private JMenu createMainMenu()
	{
		final JMenu main = new JMenu( "Main" );
		main.add( createPreferencesMenuItem() );
		return main;
	}

	private JMenuItem createPreferencesMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Preferences..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( ()
						-> showPreferencesDialog() ) );
		return menuItem;
	}

	private void showPreferencesDialog()
	{
		new Thread( () -> {
			final NonBlockingGenericDialog gd
					= new NonBlockingGenericDialog( "Preferences" );
			gd.addNumericField( "3D View Voxel Size [micrometer]",
					sourcesPanel.getVoxelSpacing3DView(), 2 );
			gd.addNumericField( "3D View Mesh Smoothing Iterations [#]",
					sourcesPanel.getMeshSmoothingIterations(), 0 );
			gd.addNumericField( "Gene Search Radius [micrometer]",
					actionPanel.getGeneSearchRadiusInMicrometer(), 1 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			sourcesPanel.setVoxelSpacing3DView( gd.getNextNumber() );
			sourcesPanel.setMeshSmoothingIterations( ( int ) gd.getNextNumber() );
			actionPanel.setGeneSearchRadiusInMicrometer( gd.getNextNumber() );

		} ).start();
	}

	public void showFrame( String version )
	{
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int numModalities = actionPanel.getSortedModalities().size();
		final int actionPanelHeight = ( numModalities + 7 ) * 40;
		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesPanel );
		splitPane.setAutoscrolls( true );
		frameWidth = 600;
		setPreferredSize( new Dimension( frameWidth, actionPanelHeight + 200 ) );
		getContentPane().setLayout( new GridLayout() );
		getContentPane().add( splitPane );

		this.setTitle( "PlatyBrowser - Data Version " + version );

		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.pack();
		this.setVisible( true );
	}

	public PlatyBrowserSourcesPanel getSourcesPanel()
	{
		return sourcesPanel;
	}

	public PlatyBrowserActionPanel getActionPanel()
	{
		return actionPanel;
	}



}
