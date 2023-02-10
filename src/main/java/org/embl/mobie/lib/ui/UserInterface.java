/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.ui;

import org.embl.mobie.lib.MoBIE;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.display.Display;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.serialize.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JScrollPane displaySettingsScrollPane;
	private final JFrame frame;
	private final JPanel selectionPanel;
	private final UserInterfaceHelper userInterfaceHelper;
	private Map< Object, JPanel > displayToPanel;
	private JSplitPane splitPane;
	private boolean closedByUser = true;

	public UserInterface( MoBIE moBIE )
	{
		MoBIELaf.MoBIELafOn();
		userInterfaceHelper = new UserInterfaceHelper( moBIE );
		selectionPanel = userInterfaceHelper.createSelectionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsContainer();
		displaySettingsScrollPane = userInterfaceHelper.createDisplaySettingsScrollPane( displaySettingsContainer );
		JPanel displaySettingsPanel = userInterfaceHelper.createDisplaySettingsPanel( displaySettingsScrollPane );
		displayToPanel = new HashMap<>();
		frame = createAndShowFrame( selectionPanel, displaySettingsPanel, moBIE.getProjectName() + " " + moBIE.getCurrentDatasetName() );
		MoBIELaf.MoBIELafOff();
		configureWindowClosing( moBIE );
	}

	private void configureWindowClosing( MoBIE moBIE )
	{
		frame.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent ev )
				{
					frame.dispose();
					moBIE.close();
					if ( MoBIE.openedFromCLI )
						System.exit( 0 );
				}
			});
	}

	private JFrame createAndShowFrame( JPanel selectionPanel, JPanel displaySettingsPanel, String panelName )
	{
		JFrame frame = new JFrame( "MoBIE " + panelName );

		splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();

		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( selectionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( 550, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		frame.setLocation( UserInterfaceHelper.SPACING, frame.getY() );

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
		selectionPanel.revalidate();
		selectionPanel.repaint();
		// update the location of the splitpane divider, so any new uiSelectionGroups are visible
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();
		splitPane.setDividerLocation( actionPanelHeight );
		frame.revalidate();
		frame.repaint();
	}

	public void addViews( Map<String, View > views )
	{
		MoBIELaf.MoBIELafOn();
		userInterfaceHelper.addViewsToSelectionPanel( views );
		refreshSelection();
		MoBIELaf.MoBIELafOff();
	}

	public Map< String, Map< String, View > > getGroupingsToViews()
	{
		return userInterfaceHelper.getGroupingsToViews();
	}

	public void addSourceDisplay( Display display )
	{
		MoBIELaf.MoBIELafOn();
		final JPanel panel = createDisplaySettingPanel( display );
		showDisplaySettingsPanel( display, panel );
		MoBIELaf.MoBIELafOff();
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
		else if ( display instanceof RegionDisplay )
		{
			return userInterfaceHelper.createRegionDisplaySettingsPanel( ( RegionDisplay ) display );
		}
		else if ( display instanceof SpotDisplay )
		{
			return userInterfaceHelper.createSpotDisplaySettingsPanel( ( SpotDisplay ) display );
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
