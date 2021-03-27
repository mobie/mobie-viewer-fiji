package de.embl.cba.mobie2.ui;

import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JFrame frame;
	private final JPanel actionContainer;
	private final UserInterfaceHelper userInterfaceHelper;

	public UserInterface( MoBIE2 moBIE )
	{
		userInterfaceHelper = new UserInterfaceHelper( moBIE );

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
		frame.setPreferredSize( new Dimension( Toolkit.getDefaultToolkit().getScreenSize().width / 3, actionPanelHeight + 200 ) );
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

	public void addDisplaySettings( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof ImageDisplay )
		{
			userInterfaceHelper.addImageDisplaySettings( this, ( ImageDisplay ) sourceDisplay );
			refresh();
		}
		else if ( sourceDisplay instanceof SegmentationDisplay )
		{
			userInterfaceHelper.addSegmentationDisplaySettings( this, ( SegmentationDisplay ) sourceDisplay );
			refresh();
		}
	}

	protected void removeDisplaySettings( JPanel panel )
	{
		displaySettingsContainer.remove( panel );
		refresh();
	}

	protected void addDisplaySettings( JPanel panel )
	{
		displaySettingsContainer.add( panel );
		refresh();
	}

	public Window getWindow()
	{
		return frame;
	}
}
