package org.embl.mobie.viewer.ui;

import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.display.AnnotatedIntervalDisplay;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import org.embl.mobie.viewer.display.SegmentationSourceDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.embl.mobie.viewer.ui.UserInterfaceHelper.*;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JScrollPane displaySettingsScrollPane;
	private final JFrame frame;
	private final JPanel selectionContainer;
	private final UserInterfaceHelper userInterfaceHelper;
	private Map< Object, JPanel > displayToPanel;
	private JSplitPane splitPane;

	public UserInterface( MoBIE moBIE )
	{
		MoBIELookAndFeelToggler.setMoBIELaf();
		userInterfaceHelper = new UserInterfaceHelper( moBIE );

		selectionContainer = userInterfaceHelper.createSelectionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsContainer();
		displaySettingsScrollPane = userInterfaceHelper.createDisplaySettingsScrollPane( displaySettingsContainer );
		JPanel displaySettingsPanel = userInterfaceHelper.createDisplaySettingsPanel( displaySettingsScrollPane );
		displayToPanel = new HashMap<>();

		frame = createAndShowFrame( selectionContainer, displaySettingsPanel, moBIE.getProjectName() + "-" + moBIE.getDatasetName() );
		MoBIELookAndFeelToggler.resetMoBIELaf();
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

	// The name must be unique!
	public void addView( String name, View view )
	{
		Map<String, View> views = new HashMap<>();
		views.put( name, view );
		addViews( views );
	}

	public void addViews( Map<String, View> views )
	{
		userInterfaceHelper.addViewsToSelectionPanel( views );
		refreshSelection();
	}

	public Map< String, Map< String, View > > getGroupingsToViews()
	{
		return userInterfaceHelper.getGroupingsToViews();
	}

	public void addSourceDisplay( SourceDisplay sourceDisplay )
	{
		final JPanel panel = createDisplaySettingPanel( sourceDisplay );
		showDisplaySettingsPanel( sourceDisplay, panel );
	}

	private JPanel createDisplaySettingPanel( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof ImageSourceDisplay)
		{
			return userInterfaceHelper.createImageDisplaySettingsPanel( ( ImageSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationSourceDisplay)
		{
			return userInterfaceHelper.createSegmentationDisplaySettingsPanel( ( SegmentationSourceDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof AnnotatedIntervalDisplay)
		{
			return userInterfaceHelper.createAnnotatedIntervalDisplaySettingsPanel( ( AnnotatedIntervalDisplay ) sourceDisplay );
		}
		else
		{
			throw new UnsupportedOperationException();
		}
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

			// scroll to bottom, so any new panels are visible
			displaySettingsScrollPane.validate();
			JScrollBar vertical = displaySettingsScrollPane.getVerticalScrollBar();
			vertical.setValue( vertical.getMaximum() );

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
