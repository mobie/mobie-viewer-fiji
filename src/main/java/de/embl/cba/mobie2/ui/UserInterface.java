package de.embl.cba.mobie2.ui;

import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setSystemSwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setLafSwingLookAndFeel;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JFrame frame;
	private final JPanel actionContainer;
	private final UserInterfaceHelper userInterfaceHelper;
	private Map< Display, JPanel > sourceDisplayToPanel;

	public UserInterface( MoBIE2 moBIE )
	{
		userInterfaceHelper = new UserInterfaceHelper( moBIE );

		actionContainer = userInterfaceHelper.createActionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsPanel();
		sourceDisplayToPanel = new HashMap<>();

		frame = createAndShowFrame( actionContainer, displaySettingsContainer, moBIE.getProjectName() + "-" + moBIE.getCurrentDatasetName() );
	}

	private JFrame createAndShowFrame( JPanel actionPanel, JPanel displaySettingsPanel, String panelName )
	{
		JFrame frame = new JFrame( "MoBIE: " + panelName );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int sourceSelectionPanelHeight = userInterfaceHelper.getViewsSelectionPanelHeight();
		final int actionPanelHeight = sourceSelectionPanelHeight + 3 * 40;

		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( (int) UserInterfaceHelper.getDefaultWindowWidth(), actionPanelHeight + 200 ) );
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

	public void addSourceDisplay( Display display )
	{
		setLafSwingLookAndFeel();

		if ( display instanceof ImageDisplay )
		{
			userInterfaceHelper.addImageDisplaySettingsPanel( this, ( ImageDisplay ) display );
			refresh();
		}
		else if ( display instanceof SegmentationDisplay )
		{
			userInterfaceHelper.addSegmentationDisplaySettingsPanel( this, ( SegmentationDisplay ) display );
			refresh();
		}

		setSystemSwingLookAndFeel();
	}

	public void removeSourceDisplay( Display display )
	{
		final JPanel jPanel = sourceDisplayToPanel.get( display );
		displaySettingsContainer.remove( jPanel );
		sourceDisplayToPanel.remove( display );
	}

	protected void showDisplaySettingsPanel( Display display, JPanel panel )
	{
		sourceDisplayToPanel.put( display, panel );
		displaySettingsContainer.add( panel );
		refresh();
	}

	public Window getWindow()
	{
		return frame;
	}
}
