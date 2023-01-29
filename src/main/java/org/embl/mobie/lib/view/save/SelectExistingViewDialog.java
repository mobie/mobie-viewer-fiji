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
package org.embl.mobie.lib.view.save;

import de.embl.cba.tables.SwingUtils;
import org.embl.mobie.lib.MoBIE;
import org.embl.mobie.lib.create.ProjectCreatorHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.ui.MoBIELaf;
import org.embl.mobie.lib.ui.SwingHelper;
import org.embl.mobie.lib.view.AdditionalViews;

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
    public SelectExistingViewDialog( Dataset dataset ) {
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(dataset);
    }

    // write to additional views json
    public SelectExistingViewDialog( AdditionalViews additionalViews ) {
        groupToViewsMap = ProjectCreatorHelper.getGroupToViewsMap(additionalViews);
    }

    public String getSelectedView() {
        showViewSelectionUI();
        return selectedView;
    }

    private void showViewSelectionUI() {
        MoBIELaf.MoBIELafOn();
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
                MoBIELaf.MoBIELafOff();
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
        groupPanel.add( SwingHelper.getJLabel("Ui selection group", 120, 10));
        groupPanel.add( groupsComboBox );
        JPanel viewPanel = SwingUtils.horizontalLayoutPanel();
        viewPanel.add( SwingHelper.getJLabel("View name", 120, 10));
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
