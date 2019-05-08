package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;
import bdv.util.Prefs;
import de.embl.cba.platynereis.Constants;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

public class PlatyBrowser extends JFrame
{
	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final PlatyBrowserActionPanel actionPanel;

	public PlatyBrowser( File dataFolder ) throws HeadlessException
	{
		sourcesPanel = new PlatyBrowserSourcesPanel( dataFolder );
		sourcesPanel.addSourceToPanelAndViewer( Constants.DEFAULT_EM_RAW_FILE_ID );
		actionPanel = new PlatyBrowserActionPanel( sourcesPanel );
		setJMenuBar( createMenuBar() );
		showFrame();
		initHelpDialog();
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


	public void showFrame()
	{
		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		splitPane.setDividerLocation( 180 );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesPanel );
		splitPane.setAutoscrolls( true );
		setPreferredSize( new Dimension(700, 400));
		getContentPane().setLayout( new GridLayout() );
		getContentPane().add( splitPane );

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

	public void initHelpDialog()
	{
		HelpDialog helpDialog = new HelpDialog( this,
				PlatyBrowser.class.getResource( "/Help.html" ) );

		this.setFocusable( true );
		this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put( KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help" );

		AbstractAction help = new AbstractAction()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				helpDialog.setVisible( ! helpDialog.isVisible() );
			}
		};

		this.getRootPane().getActionMap().put("help", help );
	}

}
