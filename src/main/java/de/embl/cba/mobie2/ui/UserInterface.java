package de.embl.cba.mobie2.ui;

import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.display.SegmentationDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.grid.GridOverlayDisplay;
import de.embl.cba.mobie2.view.View;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.mobie2.ui.UserInterfaceHelper.resetSystemSwingLookAndFeel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JFrame frame;
	private final JPanel selectionContainer;
	private final UserInterfaceHelper userInterfaceHelper;
	private Map< Object, JPanel > displayToPanel;
	private JSplitPane splitPane;

	public UserInterface( MoBIE2 moBIE )
	{
		userInterfaceHelper = new UserInterfaceHelper( moBIE );

		selectionContainer = userInterfaceHelper.createSelectionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsPanel();
		displayToPanel = new HashMap<>();

		setMoBIESwingLookAndFeel();
		frame = createAndShowFrame( selectionContainer, displaySettingsContainer, moBIE.getProjectName() + "-" + moBIE.getDatasetName() );
		resetSystemSwingLookAndFeel();
	}

	private JFrame createAndShowFrame( JPanel actionPanel, JPanel displaySettingsPanel, String panelName )
	{
		JFrame frame = new JFrame( "MoBIE: " + panelName );

		splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();


		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( 550, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		return frame;
	}

	private void refreshDisplaySettings()
	{
		displaySettingsContainer.revalidate();
		displaySettingsContainer.repaint();
		frame.revalidate();
		frame.repaint();
	}

	private void refreshSelection()
	{
		selectionContainer.revalidate();
		selectionContainer.repaint();
		// update the location of the splitpane divider, so any new uiSelectionGroups are visible
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();
		splitPane.setDividerLocation( actionPanelHeight );
		frame.revalidate();
		frame.repaint();
	}

	public void addSourceDisplay( Display display )
	{
		final JPanel panel = createDisplaySettingPanel( display );
		showDisplaySettingsPanel( display, panel );
		refreshDisplaySettings();
	}

	public void addViews( Map<String, View> views ) {
		userInterfaceHelper.addViewsToSelectionPanel( views );
		refreshSelection();
	}

	private JPanel createDisplaySettingPanel( Display display )
	{
		if ( display instanceof ImageDisplay )
		{
			return userInterfaceHelper.createImageDisplaySettingsPanel( ( ImageDisplay ) display );
		}
		else if ( display instanceof SegmentationDisplay )
		{
			return userInterfaceHelper.createSegmentationDisplaySettingsPanel( ( SegmentationDisplay ) display );
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}

	public void addGridView( GridOverlayDisplay gridOverlayDisplay )
	{
		final JPanel panel = userInterfaceHelper.createGridViewDisplaySettingsPanel( gridOverlayDisplay );
		showDisplaySettingsPanel( gridOverlayDisplay, panel );
	}

	public void removeDisplaySettingsPanel( Object display )
	{
		SwingUtilities.invokeLater( () -> {
			final JPanel jPanel = displayToPanel.get( display );
			displaySettingsContainer.remove( jPanel );
			displayToPanel.remove( display );
			refreshDisplaySettings();
		} );
	}

	protected void showDisplaySettingsPanel( Object display, JPanel panel )
	{
		SwingUtilities.invokeLater( () -> {
			displayToPanel.put( display, panel );
			displaySettingsContainer.add( panel );
			refreshDisplaySettings();
		});
	}

	public Window getWindow()
	{
		return frame;
	}

	public void close()
	{
		frame.dispose();
	}
}
