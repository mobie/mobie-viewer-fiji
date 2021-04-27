package de.embl.cba.mobie.ui;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.image.SourceAndMetadataChangedListener;
import de.embl.cba.mobie2.ui.WindowArrangementHelper;
import de.embl.cba.tables.image.SourceAndMetadata;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class UserInterface implements SourceAndMetadataChangedListener
{
	private final UserInterfaceComponentsProvider componentsProvider;
	private final JPanel displaySettingsPanel;
	private final SourcesDisplayManager displayManager;
	private HashMap< Object, JPanel > sourceToPanel;
	private final JFrame frame;
	private final JPanel actionPanel;

	public UserInterface( MoBIE moBIE )
	{
		displayManager = moBIE.getSourcesDisplayManager();
		displayManager.listeners().add( this );

		componentsProvider = new UserInterfaceComponentsProvider( moBIE );

		actionPanel = createActionPanel( moBIE );
		displaySettingsPanel = createDisplaySettingsPanel();

		frame = createAndShowFrame( moBIE, actionPanel, displaySettingsPanel );
	}

	public void dispose()
	{
		frame.dispose();
	}

	private JPanel createDisplaySettingsPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );

		sourceToPanel = new HashMap<>();

		return panel;
	}

	private JPanel createActionPanel( MoBIE moBIE )
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( actionPanel, BoxLayout.Y_AXIS ) );

		actionPanel.add( componentsProvider.createInfoPanel( moBIE.getProjectLocation(), moBIE.getOptions().values.getPublicationURL() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( componentsProvider.createDatasetSelectionPanel() );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( componentsProvider.createSourceSelectionPanel( moBIE.getSourcesDisplayManager() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( componentsProvider.createBookmarksPanel( moBIE.getBookmarkManager() )  );
		actionPanel.add( componentsProvider.createMoveToLocationPanel( )  );

		if ( moBIE.getLevelingVector() != null )
		{
			actionPanel.add( componentsProvider.createLevelingPanel( moBIE.getLevelingVector() ) );
		}
		return actionPanel;
	}

	private JFrame createAndShowFrame( MoBIE moBIE, JPanel actionPanel, JPanel displaySettingsPanel )
	{
		JFrame frame = new JFrame( "MoBIE: " + moBIE.getProjectName() + "-" + moBIE.getDataset() );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int sourceSelectionPanelHeight = componentsProvider.getSourceSelectionPanelHeight();
		final int actionPanelHeight = sourceSelectionPanelHeight + 7 * 40;

		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( 700, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		return frame;
	}

	private void refresh()
	{
		displaySettingsPanel.revalidate();
		displaySettingsPanel.repaint();
		frame.revalidate();
		frame.repaint();
	}

	@Override
	public synchronized void addedToBDV( SourceAndMetadata< ? > sam )
	{
		Object panelKey = getPanelKey( sam );

		if ( sourceToPanel.containsKey( panelKey ) )
		{
			return;
		}
		else
		{
			final JPanel panel = componentsProvider.createDisplaySettingsPanel( sam, displayManager );
			displaySettingsPanel.add( panel );
			sourceToPanel.put( panelKey, panel );
			refresh();
		}
	}

	protected Object getPanelKey( SourceAndMetadata< ? > sam )
	{
		Object panelKey;
		if ( sam.metadata().groupId != null )
			panelKey = sam.metadata().groupId;
		else
			panelKey = sam.metadata();
		return panelKey;
	}

	@Override
	public void removedFromBDV( SourceAndMetadata< ? > sam )
	{
		final JPanel panel = sourceToPanel.get( getPanelKey( sam ) );
		displaySettingsPanel.remove( panel );
		sourceToPanel.remove( sam );
		refresh();
	}

	public JFrame getWindow()
	{
		return frame;
	}
}
