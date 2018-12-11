package de.embl.cba.platynereis.ui;

import bdv.tools.HelpDialog;
import bdv.util.Bdv;
import de.embl.cba.platynereis.PlatyBrowser;
import net.imagej.ops.Ops;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MainFrame extends JFrame
{
	final Bdv bdv;
	final PlatyBrowser platyBrowser;
	private final ActionPanel actionPanel;
	private final BdvSourcesPanel bdvSourcesPanel;
	private HelpDialog helpDialog;

	public MainFrame( Bdv bdv, PlatyBrowser platyBrowser ) throws HeadlessException
	{
		this.bdv = bdv;
		this.platyBrowser = platyBrowser;

		actionPanel = new ActionPanel( this, bdv, platyBrowser );
		bdvSourcesPanel = new BdvSourcesPanel( this, bdv, platyBrowser );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );  // we want it to split the window verticaly
		splitPane.setDividerLocation( 200 );                    // the initial position of the divider is 200 (our window is 400 pixels high)
		splitPane.setTopComponent( actionPanel );                  // at the top we want our "topPanel"
		splitPane.setBottomComponent( bdvSourcesPanel );

		setPreferredSize( new Dimension(700, 800));     // let's open the window with a default size of 400x400 pixels
		// the contentPane is the container that holds all our components
		getContentPane().setLayout( new GridLayout() );  // the default GridLayout is like a grid with 1 column and 1 row,
		// we only add one element to the window itself
		getContentPane().add( splitPane );               // due to the GridLayout, our splitPane will now fill the whole window


		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
//		setOpaque( true ); //content panes must be opaque
		//Display the window.
		this.pack();
		this.setVisible( true );

		initHelpDialog();

	}

	public void initHelpDialog()
	{
		helpDialog = new HelpDialog( this, MainFrame.class.getResource( "Help.html" ) );

		this.addKeyListener( new KeyListener()
		{
			@Override
			public void keyTyped( KeyEvent e )
			{

			}

			@Override
			public void keyPressed( KeyEvent e )
			{

			}

			@Override
			public void keyReleased( KeyEvent e )
			{
				if (e.getKeyCode()==KeyEvent.VK_F1)
				{
					helpDialog.setVisible( !helpDialog.isVisible() );
				}
			}
		} );
	}


	public ActionPanel getActionPanel()
	{
		return actionPanel;
	}

	public BdvSourcesPanel getBdvSourcesPanel()
	{
		return bdvSourcesPanel;
	}



}
