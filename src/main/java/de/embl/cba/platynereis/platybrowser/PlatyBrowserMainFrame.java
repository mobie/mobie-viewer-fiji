package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.modelview.views.bdv.ImageSegmentsBdvView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PlatyBrowserMainFrame extends JFrame
{
	private final ImageSegmentsBdvView bdvView;

	private final PlatyBrowserActionPanel actionPanel;
	private final PlatyBrowserSourcesPanel sourcesPanel;
	private HelpDialog helpDialog;
	private final JSplitPane splitPane;
	private AbstractAction help;

	public PlatyBrowserMainFrame( ImageSegmentsBdvView bdvView ) throws HeadlessException
	{
		this.bdvView = bdvView;

		actionPanel = new PlatyBrowserActionPanel( this, bdvView );
		sourcesPanel = new PlatyBrowserSourcesPanel( this, bdvView );

		splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		splitPane.setDividerLocation( 200 );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesPanel );

		setPreferredSize( new Dimension(700, 800));

		// the contentPane is the container that holds all our components
		getContentPane().setLayout( new GridLayout() );  // the default GridLayout is like a grid with 1 column and 1 row,

		// we only add one element to the window itself
		getContentPane().add( splitPane );               // due to the GridLayout, our splitPane will now fill the whole window

		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.pack();
		this.setVisible( true );

		initHelpDialog();

	}

	public void initHelpDialog()
	{
		helpDialog = new HelpDialog( this, PlatyBrowserMainFrame.class.getResource( "/Help.html" ) );

		this.setFocusable( true );
		this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put( KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help" );

		help = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				helpDialog.setVisible( ! helpDialog.isVisible() );
			}
		};

		this.getRootPane().getActionMap().put("help", help );
	}


	public PlatyBrowserActionPanel getActionPanel()
	{
		return actionPanel;
	}

	public PlatyBrowserSourcesPanel getSourcesPanel()
	{
		return sourcesPanel;
	}



}
