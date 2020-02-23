package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.platynereis.Constants;
import de.embl.cba.platynereis.platyviews.PlatyViews;
import de.embl.cba.platynereis.utils.FileAndUrlUtils;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PlatyBrowser extends JFrame
{
	public static final String PROTOTYPE_DISPLAY_VALUE = "01234567890123456789";

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final PlatyBrowserActionPanel actionPanel;
	private int frameWidth;

	public PlatyBrowser(
			String version,
			String imageDataLocation,
			String tableDataLocation ) throws HeadlessException
	{
		imageDataLocation = FileAndUrlUtils.removeTrailingSlash( imageDataLocation );
		tableDataLocation = FileAndUrlUtils.removeTrailingSlash( tableDataLocation );

		sourcesPanel = new PlatyBrowserSourcesPanel(
				version,
				imageDataLocation,
				tableDataLocation );

		final PlatyViews platyViews = new PlatyViews( sourcesPanel, FileAndUrlUtils.combinePath( tableDataLocation, version, "misc/bookmarks.json" ) );

		actionPanel = new PlatyBrowserActionPanel( sourcesPanel, platyViews );

		setJMenuBar( createMenuBar() );
		showFrame( version );
		adaptLogWindowPositionAndSize();

		sourcesPanel.setParentComponent( this );
		sourcesPanel.addSourceToPanelAndViewer( Constants.DEFAULT_EM_RAW_FILE_ID );

		actionPanel.setBdv( sourcesPanel.getBdv() );
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
		frameWidth = 550;
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
