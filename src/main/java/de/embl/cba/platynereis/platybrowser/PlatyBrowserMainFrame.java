package de.embl.cba.platynereis.platybrowser;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.modelview.views.ImageSegmentsBdvView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PlatyBrowserMainFrame extends JFrame
{

	private final PlatyBrowserSourcesPanel sourcesPanel;
	private final PlatyBrowserActionPanel actionPanel;

	public PlatyBrowserMainFrame( ImageSegmentsBdvView bdvView ) throws HeadlessException
	{
		sourcesPanel = new PlatyBrowserSourcesPanel( bdvView );
		actionPanel = new PlatyBrowserActionPanel( sourcesPanel, bdvView );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		splitPane.setDividerLocation( 200 );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesPanel );

		setPreferredSize( new Dimension(700, 800));
		getContentPane().setLayout( new GridLayout() );
		getContentPane().add( splitPane );

		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.pack();
		this.setVisible( true );

		initHelpDialog();

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
		HelpDialog helpDialog = new HelpDialog( this, PlatyBrowserMainFrame.class.getResource( "/Help.html" ) );

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
