package org.embl.mobie.viewer.view.saving;

import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.ui.MoBIELookAndFeelToggler;
import org.embl.mobie.viewer.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.SwingUtils;
import org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper;
import org.embl.mobie.viewer.ui.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Map;

public class SelectExistingViewDialog {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private JComboBox<String> groupsComboBox;
    private JComboBox<String> viewsComboBox;
    private JDialog dialog;

    private Map<String, ArrayList<String>> groupToViewsMap;
    private String selectedView;

    // writing to dataset json
    public SelectExistingViewDialog(Dataset dataset ) {
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(dataset);
    }

    // write to additional views json
    public SelectExistingViewDialog(AdditionalViews additionalViews ) {
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(additionalViews);
    }

    public String getSelectedView() {
        MoBIELookAndFeelToggler.setMoBIELaf();
        createPanels();
        return selectedView;
    }

    private void createPanels() {
        dialog = new JDialog((Frame)null, true);
        dialog.setTitle( "Choose an existing view..." );
        dialog.getContentPane().setLayout( new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS ) );
        dialog.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        createComboBoxes();
        createAcceptPanel();
        // reset swing laf when finished
        dialog.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                MoBIELookAndFeelToggler.resetMoBIELaf();
                e.getWindow().dispose();
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible( true );
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

        dialog.getContentPane().add( groupPanel );
        dialog.getContentPane().add( viewPanel );
    }

    private void createAcceptPanel() {
        JPanel acceptPanel = SwingUtils.horizontalLayoutPanel();
        JButton selectButton = SwingHelper.createButton("Select");
        selectButton.addActionListener( e ->
        {
            new Thread( () -> {
                selectedView = (String) viewsComboBox.getSelectedItem();
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            } ).start();
        } );
        JButton cancelButton = SwingHelper.createButton("Cancel");
        cancelButton.addActionListener( e ->
        {
            new Thread( () -> {
                selectedView = null;
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            } ).start();
        } );
        acceptPanel.add( selectButton );
        acceptPanel.add( cancelButton );
        dialog.getContentPane().add( acceptPanel );
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
