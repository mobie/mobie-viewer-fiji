package de.embl.cba.platynereis.platybrowser;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.BdvDialogs;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SourcesDisplayUI
{
	public static JCheckBox createBigDataViewerVisibilityCheckbox(
			int[] dims,
			SourceAndMetadata< ? > sam,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "B" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( sam.metadata().bdvStackSource != null )
					sam.metadata().bdvStackSource.setActive( checkBox.isSelected() );
			}
		} );

		return checkBox;
	}

	public static JCheckBox createVolumeViewVisibilityCheckbox(
			PlatyBrowserSourcesPanel sourcesPanel,
			int[] dims,
			SourceAndMetadata< ? > sam,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
					updateSegments3dView( sam, checkBox, sourcesPanel);
					updateSource3dView( sam, checkBox, sourcesPanel );
				}).start();
			}
		} );

		return checkBox;
	}

	public static void updateSource3dView( SourceAndMetadata< ? > sam, JCheckBox checkBox, PlatyBrowserSourcesPanel sourcesPanel )
	{
		sam.metadata().showImageIn3d = checkBox.isSelected();

		if ( sam.metadata().type.equals( Metadata.Type.Segmentation ) ) return;

		if ( checkBox.isSelected() )
		{
			sourcesPanel.showSourceInVolumeViewer( sam );
		}
		else
		{
			if ( sam.metadata().content != null )
				sam.metadata().content.setVisible( false );
		}
	}

	public static void updateSegments3dView( SourceAndMetadata< ? > sam, JCheckBox checkBox, PlatyBrowserSourcesPanel sourcesPanel )
	{
		if ( sam.metadata().views != null )
		{
			final Segments3dView< TableRowImageSegment > segments3dView = sam.metadata().views.getSegments3dView();

			segments3dView.setShowSelectedSegmentsIn3D( checkBox.isSelected() );

			if ( checkBox.isSelected() )
				sourcesPanel.setUniverse( segments3dView.getUniverse() );
		}
	}


	public static JButton createBrightnessButton( int[] buttonDimensions,
												  SourceAndMetadata< ? > sam,
												  final double rangeMin,
												  final double rangeMax )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( new Dimension(
				buttonDimensions[ 0 ],
				buttonDimensions[ 1 ] ) );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups
					= getConverterSetups( sam.metadata().bdvStackSource );

			BdvDialogs.showBrightnessDialog(
					sam.metadata().displayName,
					converterSetups,
					rangeMin,
					rangeMax );

			// TODO: Can this be done for the content as well?
		} );

		return button;
	}


	private static ArrayList< ConverterSetup > getConverterSetups(
			BdvStackSource bdvStackSource )
	{
		bdvStackSource.setCurrent();
		final int sourceIndex = bdvStackSource.getBdvHandle()
				.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		converterSetups.add( bdvStackSource.getBdvHandle()
				.getSetupAssignments().getConverterSetups().get( sourceIndex ) );
		return converterSetups;
	}


}
