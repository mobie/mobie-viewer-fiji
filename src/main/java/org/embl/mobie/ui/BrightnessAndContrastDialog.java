package org.embl.mobie.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BdvHandle;
import bdv.util.BoundedValueDouble;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.bdv.ContrastComputer;
import org.embl.mobie.lib.bdv.blend.BlendingMode;
import org.embl.mobie.lib.color.opacity.MoBIEColorConverter;
import org.embl.mobie.lib.color.opacity.OpacityAdjuster;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BrightnessAndContrastDialog
{
    public static JFrame showDialog(
            String name,
            List< ? extends SourceAndConverter< ? > > sacs,
            BdvHandle bdvHandle,
            boolean addContrastLimitUI )
    {
        ISourceAndConverterService service =
                SourceAndConverterServices.getSourceAndConverterService();

        SacAdjustmentManager sacAdjustmentManager = new SacAdjustmentManager( bdvHandle, sacs );

        JFrame frame = new JFrame( name );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        JPanel panel = new JPanel();
        panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

        if ( addContrastLimitUI )
        {
            // Contrast Limits
            //
            List< ConverterSetup > converterSetups =
                    sacAdjustmentManager.getAdjustable()
                    .stream()
                    .map( sac -> service.getConverterSetup( sac ) )
                    .collect( Collectors.toList() );

            List< ? extends Converter< ?, ARGBType > > converters =
                    sacAdjustmentManager.getAdjustable()
                    .stream()
                    .map( sac -> sac.getConverter() )
                    .collect( Collectors.toList() );

            final double currentContrastLimitsMin = converterSetups.get( 0 ).getDisplayRangeMin();
            final double currentContrastLimitsMax = converterSetups.get( 0 ).getDisplayRangeMax();
            final double absCurrentRange = Math.abs( currentContrastLimitsMax - currentContrastLimitsMin );

            final double rangeFactor = 1.0; // could be changed...

            final double rangeMin = currentContrastLimitsMin - rangeFactor * absCurrentRange;
            final double rangeMax = currentContrastLimitsMax + rangeFactor * absCurrentRange;

            final BoundedValueDouble min =
                    new BoundedValueDouble(
                            rangeMin,
                            rangeMax,
                            currentContrastLimitsMin );

            final BoundedValueDouble max =
                    new BoundedValueDouble(
                            rangeMin,
                            rangeMax,
                            currentContrastLimitsMax );

            double spinnerStepSize = absCurrentRange / 100.0;

            // TODO: adapt the number of decimal places to the current range
            String decimalFormat = "#####.####";

            final SliderPanelDouble minSlider =
                    new SliderPanelDouble( "Min", min, spinnerStepSize );
            minSlider.setNumColummns( 10 );
            minSlider.setDecimalFormat( decimalFormat );

            final SliderPanelDouble maxSlider =
                    new SliderPanelDouble( "Max", max, spinnerStepSize );
            maxSlider.setNumColummns( 10 );
            maxSlider.setDecimalFormat( decimalFormat );

            final BrightnessUpdateListener brightnessUpdateListener =
                    new BrightnessUpdateListener(
                            min,
                            max,
                            minSlider,
                            maxSlider,
                            sacAdjustmentManager );

            min.setUpdateListener( brightnessUpdateListener );
            max.setUpdateListener( brightnessUpdateListener );

            JPanel minPanel = SwingHelper.horizontalFlowLayoutPanel();
            minPanel.add( minSlider );
            panel.add( minPanel );

            JPanel maxPanel = SwingHelper.horizontalFlowLayoutPanel();
            maxPanel.add( maxSlider );
            panel.add( maxPanel );

            addAutoContrastPanel( bdvHandle, sacAdjustmentManager, min, max, panel );

            addInvertContrastPanel( bdvHandle, converters, panel );
        }

        addBlendingModePanel( bdvHandle, service, sacAdjustmentManager, panel );

        addOpacityPanel( bdvHandle, sacAdjustmentManager, panel );

        if ( sacAdjustmentManager.getAll().size() > 1 ) // Multiple sources are part of this layer
        {
            JCheckBox adjustOnlyVisibleCheckBox = new JCheckBox( "Change settings only for visible images" );
            adjustOnlyVisibleCheckBox.setSelected( false );
            adjustOnlyVisibleCheckBox.setToolTipText( "If checked, only the settings of images that are visible inside the current BDV window will be adjusted." );
            adjustOnlyVisibleCheckBox.addActionListener( e -> sacAdjustmentManager.adjustOnlyVisible( adjustOnlyVisibleCheckBox.isSelected() ) );
            JPanel adjustOnlyVisiblePanel = SwingHelper.horizontalFlowLayoutPanel();
            adjustOnlyVisiblePanel.add( adjustOnlyVisibleCheckBox );
            panel.add( adjustOnlyVisiblePanel );
            panel.add( new JLabel( "" ) ); // Create space

            JCheckBox autoContrastAllCheckBox = new JCheckBox( "Auto contrast all (visible) images individually" );
            autoContrastAllCheckBox.setSelected( false );
            autoContrastAllCheckBox.setToolTipText( "If checked, the contrast will measured and applied for all (visible) images individually." );
            autoContrastAllCheckBox.addActionListener( e -> sacAdjustmentManager.autoContrastAll( autoContrastAllCheckBox.isSelected() ) );
            JPanel autoContrastAllPanel = SwingHelper.horizontalFlowLayoutPanel();
            autoContrastAllPanel.add( autoContrastAllCheckBox );
            panel.add( autoContrastAllPanel );
            panel.add( new JLabel( "" ) );
        }

        // Display the window
        frame.setContentPane( panel );
        frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
                MouseInfo.getPointerInfo().getLocation().y,
                120, 10);
        frame.setResizable( false );
        frame.pack();
        frame.setVisible( true );

        return frame;
    }

    private static void addOpacityPanel( BdvHandle bdvHandle, SacAdjustmentManager sacAdjustmentManager, JPanel panel )
    {
        // Opacity Slider
        //
        // TODO: This cast requires that the sourceAndConverter implements
        //   an OpacityAdjuster; how to do this more cleanly?
        //   Maybe we should rather operate on the coloring model that is
        //   wrapped in the converter?
        final double current = ( ( OpacityAdjuster ) sacAdjustmentManager.getAdjustable().get( 0 ).getConverter()).getOpacity();

        final BoundedValueDouble selection =
                new BoundedValueDouble(
                        0.0,
                        1.0,
                        current );

        final SliderPanelDouble opacitySlider = new SliderPanelDouble( "Opacity", selection, 0.05 );
        opacitySlider.setNumColummns( 3 );
        opacitySlider.setDecimalFormat( "#.##" );

        final UserInterfaceHelper.OpacityUpdateListener opacityUpdateListener =
                new UserInterfaceHelper.OpacityUpdateListener(
                        selection,
                        opacitySlider,
                        sacAdjustmentManager,
                        bdvHandle );

        selection.setUpdateListener( opacityUpdateListener );
        JPanel opacityPanel = SwingHelper.horizontalFlowLayoutPanel();
        opacityPanel.add( opacitySlider );
        panel.add( opacityPanel );
    }

    private static void addBlendingModePanel( BdvHandle bdvHandle, ISourceAndConverterService service, SacAdjustmentManager sacAdjustmentManager, JPanel panel )
    {
        // Blending mode
        JPanel blendingPanel = SwingHelper.horizontalFlowLayoutPanel();
        blendingPanel.add( new JLabel("Blending  ") );
        JComboBox< BlendingMode > blendingModeComboBox = new JComboBox<>(
                new BlendingMode[]{ BlendingMode.Sum, BlendingMode.Alpha } );
        BlendingMode currentBlendingMode = ( BlendingMode ) service.getMetadata(
                sacAdjustmentManager.getAdjustable().get( 0 ),
                BlendingMode.class.getName() );
        blendingModeComboBox.setSelectedItem( currentBlendingMode );
        blendingModeComboBox.addActionListener( e ->
        {
            List< ? extends SourceAndConverter< ? > > sourceAndConverters = sacAdjustmentManager.getAdjustable();
            for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
            {
                service.setMetadata( sourceAndConverter,
                        BlendingMode.class.getName(),
                        blendingModeComboBox.getSelectedItem() );
            }
            bdvHandle.getViewerPanel().requestRepaint();
        } );
        blendingPanel.add( blendingModeComboBox );
        panel.add( blendingPanel );
    }

    private static void addAutoContrastPanel( BdvHandle bdvHandle, SacAdjustmentManager sacAdjustmentManager, BoundedValueDouble min, BoundedValueDouble max, JPanel panel )
    {
        JButton autoButton = new JButton("Auto Contrast");
        autoButton.addActionListener( e -> autoContrast( bdvHandle, sacAdjustmentManager, min, max ) );
        JPanel autoPanel = SwingHelper.horizontalFlowLayoutPanel();
        autoPanel.add( autoButton );
        panel.add( autoPanel );
    }

    private static void addInvertContrastPanel( BdvHandle bdvHandle, List< ? extends Converter< ?, ARGBType > > converters, JPanel panel )
    {
        boolean isInvert = false;
        for ( Converter< ?, ARGBType > converter : converters )
        {
            if ( converter instanceof MoBIEColorConverter )
            {
                isInvert = ( ( MoBIEColorConverter ) converter ).invert();
                break;
            }
        }
        JCheckBox invertContrast = new JCheckBox( "Invert LUT" );
        invertContrast.setSelected( isInvert );
        invertContrast.setToolTipText( "Invert the current LUT" );
        invertContrast.addActionListener( e ->
        {
            for ( Converter< ?, ARGBType > converter : converters )
            {
                if ( converter instanceof MoBIEColorConverter )
                {
                    ( ( MoBIEColorConverter ) converter ).invert( invertContrast.isSelected() );
                }
            }
            bdvHandle.getViewerPanel().requestRepaint();
        } );
        JPanel invertPanel = SwingHelper.horizontalFlowLayoutPanel();
        invertPanel.add( invertContrast );
        panel.add( invertPanel );
        panel.add( new JLabel("") ); // create some Luft
    }

    private static void autoContrast( BdvHandle bdvHandle,
                                      SacAdjustmentManager sacAdjustmentManager,
                                      BoundedValueDouble min,
                                      BoundedValueDouble max )
    {
        List< SourceAndConverter< ? > > onCanvasSacs = sacAdjustmentManager.getVisible();

        if ( onCanvasSacs.size() == 0 )
        {
            IJ.log( "[WARNING] There is no image visible and thus the contrast cannot be determined." );
            return;
        }

        if ( sacAdjustmentManager.isAutoContrastAll() )
        {
            IJ.log("\n## Contrast adjustment" );
            List< ? extends SourceAndConverter< ? > > adjustable = sacAdjustmentManager.getAdjustable();
            for ( SourceAndConverter< ? > sac : adjustable )
            {
                ContrastComputer contrastAdjuster = new ContrastComputer( bdvHandle, sac );
                double[] minMax = contrastAdjuster.computeMinMax();
                // Directly modify the SAC without updating the sliders,
                // because that would be meaningless, as it is different for each one.
                SourceAndConverterServices.getSourceAndConverterService()
                        .getConverterSetup( sac )
                        .setDisplayRange( minMax[ 0 ], minMax[ 1 ] );
                IJ.log(  "Contrast limits " + sac.getSpimSource().getName() + ": " + Arrays.toString( minMax ) );
            }
        }
        else
        {
            // Pick the first of the visible images to determine the auto-contrast
            // and update the sliders, which will trigger changing the contrast limits the sacs
            // TODO: It would make more sense to take all visible sacs here to compute the overall min and max
            //   but that may be slow when being zoomed out...
            SourceAndConverter< ? > sac = onCanvasSacs.get( 0 );
            ContrastComputer contrastAdjuster = new ContrastComputer( bdvHandle, sac );
            double[] minMax = contrastAdjuster.computeMinMax();
            min.setCurrentValue( minMax[ 0 ] );
            max.setCurrentValue( minMax[ 1 ] );
        }
    }
}
