package de.embl.cba.platynereis.ui;

import bdv.util.Bdv;
import de.embl.cba.platynereis.MainCommand;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame
{
	final Bdv bdv;
	final MainCommand mainCommand;
	private final ActionPanel actionPanel;
	private final LegendPanel legendPanel;

	public MainFrame( Bdv bdv, MainCommand mainCommand ) throws HeadlessException
	{
		this.bdv = bdv;
		this.mainCommand = mainCommand;

		actionPanel = new ActionPanel( bdv, mainCommand );
		legendPanel = new LegendPanel( mainCommand );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );  // we want it to split the window verticaly
		splitPane.setDividerLocation( 200);                    // the initial position of the divider is 200 (our window is 400 pixels high)
		splitPane.setLeftComponent( actionPanel );                  // at the top we want our "topPanel"
		splitPane.setRightComponent( legendPanel );

		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
//		setOpaque( true ); //content panes must be opaque
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );
		this.setContentPane( splitPane );
		//Display the window.
		this.pack();
		this.setVisible( true );

	}

	public ActionPanel getActionPanel()
	{
		return actionPanel;
	}

	public LegendPanel getLegendPanel()
	{
		return legendPanel;
	}



}
