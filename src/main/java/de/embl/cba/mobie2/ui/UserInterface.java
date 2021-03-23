package de.embl.cba.mobie2.ui;

import de.embl.cba.mobie2.ImageDisplay;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.SourceDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private HashMap< Object, JPanel > displayToPanel;
	private final JFrame frame;
	private final JPanel actionContainer;
	private final UserInterfaceHelper userInterfaceHelper;

	public UserInterface( MoBIE2 moBIE )
	{
		userInterfaceHelper = new UserInterfaceHelper( moBIE );
		displayToPanel = new HashMap<>();

		actionContainer = userInterfaceHelper.createActionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsPanel();

		frame = createAndShowFrame( actionContainer, displaySettingsContainer, moBIE.getProjectName() + "-" + moBIE.getCurrentDatasetName() );
	}

	public void dispose()
	{
		frame.dispose();
	}

	private JFrame createAndShowFrame( JPanel actionPanel, JPanel displaySettingsPanel, String panelName )
	{
		JFrame frame = new JFrame( "MoBIE: " + panelName );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int sourceSelectionPanelHeight = userInterfaceHelper.getViewsSelectionPanelHeight();
		final int actionPanelHeight = sourceSelectionPanelHeight + 7 * 40;

		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( 600, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		return frame;
	}

	private void refresh()
	{
		displaySettingsContainer.revalidate();
		displaySettingsContainer.repaint();
		frame.revalidate();
		frame.repaint();
	}

	private void addDisplaySettingsPanel( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof ImageDisplay )
		{
			userInterfaceHelper.addImageDisplaySettings( this, ( ImageDisplay ) sourceDisplay );
			refresh();
		}
	}

	public void removeDisplaySettings( JPanel panel )
	{
		displaySettingsContainer.remove( panel );
		refresh();
	}

	public void addDisplaySettings( JPanel panel )
	{
		displaySettingsContainer.remove( panel );
		refresh();
	}

	public JFrame getFrame()
	{
		return frame;
	}
}
