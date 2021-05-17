package de.embl.cba.mobie2.ui;

import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.display.ImageSourceDisplay;
import de.embl.cba.mobie2.display.SegmentationSourceDisplay;
import de.embl.cba.mobie2.display.SourceDisplay;
import de.embl.cba.mobie2.grid.GridOverlaySourceDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

	public void addViews( Map<String, View> views ) {
		userInterfaceHelper.addViewsToSelectionPanel(views);
		refreshSelection();
	}

	public void addSourceDisplay( SourceDisplay sourceDisplay )
	{
		final JPanel panel = createDisplaySettingPanel( sourceDisplay );
		showDisplaySettingsPanel( sourceDisplay, panel );
		refreshDisplaySettings();
	}

	private JPanel createDisplaySettingPanel( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof ImageSourceDisplay )
		{
			return userInterfaceHelper.createImageDisplaySettingsPanel( ( ImageSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationSourceDisplay )
		{
			return userInterfaceHelper.createSegmentationDisplaySettingsPanel( ( SegmentationSourceDisplay ) sourceDisplay );
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}

	public void addGridView( GridOverlaySourceDisplay gridOverlayDisplay )
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

	public String[] getUISelectionGroupNames() {

		Set<String> groupings = userInterfaceHelper.getGroupings();
		String[] groupNames = new String[groupings.size()];
		int i = 0;
		for ( String groupName: groupings ) {
			groupNames[i] = groupName;
			i++;
		}
		return groupNames;
	}

	public void close()
	{
		frame.dispose();
	}
}
