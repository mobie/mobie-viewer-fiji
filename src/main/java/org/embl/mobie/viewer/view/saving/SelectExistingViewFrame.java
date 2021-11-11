package org.embl.mobie.viewer.view.saving;

import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.ui.MoBIELookAndFeelToggler;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.SwingUtils;
import ij.IJ;
import org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper;
import org.embl.mobie.viewer.ui.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper.getGroupToViewsMap;
import static org.embl.mobie.viewer.ui.SwingHelper.createButton;
import static org.embl.mobie.viewer.ui.SwingHelper.getJLabel;
import static org.embl.mobie.viewer.view.saving.ViewSavingHelpers.writeAdditionalViewsJson;
import static org.embl.mobie.viewer.view.saving.ViewSavingHelpers.writeDatasetJson;

public class SelectExistingViewFrame extends JFrame {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private JComboBox<String> groupsComboBox;
    private JComboBox<String> viewsComboBox;
    private AdditionalViews additionalViews;
    private Dataset dataset;
    private View view;
    private String jsonPath;
    private ViewsSaver.ProjectSaveLocation saveLocation;
    private Map<String, ArrayList<String>> groupToViewsMap;

    // writing to dataset json
    public SelectExistingViewFrame( Dataset dataset, View view, String jsonPath ) {
        MoBIELookAndFeelToggler.setMoBIELaf();
        this.dataset = dataset;
        this.view = view;
        this.jsonPath = jsonPath;
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(dataset);
        createPanels();
    }

    // write to additional views json
    public SelectExistingViewFrame( AdditionalViews additionalViews, View view, String jsonPath ) {
        MoBIELookAndFeelToggler.setMoBIELaf();
        this.additionalViews = additionalViews;
        this.view = view;
        this.jsonPath = jsonPath;
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(additionalViews);
        createPanels();
    }

    private void createPanels() {
        this.setTitle( "Choose an existing view..." );
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        createComboBoxes();
        createAcceptPanel();
        // reset swing laf when finished
        this.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                MoBIELookAndFeelToggler.resetMoBIELaf();
                e.getWindow().dispose();
            }
        });
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible( true );
    }

    private void createComboBoxes() {
        groupsComboBox = new JComboBox<>( groupToViewsMap.keySet().toArray(new String[0]) );
        String selectedGroup = (String) groupsComboBox.getSelectedItem();
        viewsComboBox = new JComboBox<>( groupToViewsMap.get( selectedGroup ).toArray( new String[0] ) );
        setComboBoxDimensions( groupsComboBox );
        setComboBoxDimensions( viewsComboBox );
        groupsComboBox.addItemListener( new SyncGroupAndViewComboBox() );

        JPanel groupPanel = SwingUtils.horizontalLayoutPanel();
        groupPanel.add(SwingHelper.getJLabel("Ui selection group", 120, 10));
        groupPanel.add( groupsComboBox );
        JPanel viewPanel = SwingUtils.horizontalLayoutPanel();
        viewPanel.add(SwingHelper.getJLabel("View name", 120, 10));
        viewPanel.add( viewsComboBox );

        this.getContentPane().add( groupPanel );
        this.getContentPane().add( viewPanel );
    }

    private void createAcceptPanel() {
        JPanel acceptPanel = SwingUtils.horizontalLayoutPanel();
        JButton selectButton = SwingHelper.createButton("Select");
        selectButton.addActionListener( e ->
        {
            new Thread( () -> {
                this.setVisible( false );
                String selectedView = (String) viewsComboBox.getSelectedItem();
                if ( dataset != null ) {
                    try {
                        writeDatasetJson( dataset, view, selectedView, jsonPath );
                        IJ.log( selectedView + " overwritten in dataset.json" );
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else if ( additionalViews != null ) {
                    try {
                        writeAdditionalViewsJson( additionalViews, view, selectedView, jsonPath );
                        IJ.log( selectedView + " overwritten in " + new File(jsonPath).getName() );
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));

            } ).start();
        } );
        JButton cancelButton = SwingHelper.createButton("Cancel");
        cancelButton.addActionListener( e ->
        {
            new Thread( () -> {
                this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            } ).start();
        } );
        acceptPanel.add( selectButton );
        acceptPanel.add( cancelButton );
        this.getContentPane().add( acceptPanel );
    }


    private void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE );
        comboBox.setPreferredSize( new Dimension( 200, 20 ) );
        comboBox.setMaximumSize( new Dimension( 200, 20 ) );
    }

    private class SyncGroupAndViewComboBox implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                updateViewsComboBox();
            }
        }
    }

    private void updateViewsComboBox() {
        viewsComboBox.removeAllItems();
        for ( String viewName: groupToViewsMap.get( groupsComboBox.getSelectedItem() ) ) {
            viewsComboBox.addItem( viewName );
        }
    }
}
