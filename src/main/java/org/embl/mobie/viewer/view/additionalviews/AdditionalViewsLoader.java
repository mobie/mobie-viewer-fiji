package org.embl.mobie.viewer.view.additionalviews;

import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.Utils;
import org.embl.mobie.viewer.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.viewer.ui.UserInterfaceHelper;
import org.embl.mobie.viewer.view.View;

import java.io.IOException;
import java.util.Map;

import static org.embl.mobie.viewer.Utils.*;

public class AdditionalViewsLoader {

    private MoBIE moBIE;
    private MoBIESettings settings;

    public AdditionalViewsLoader ( MoBIE moBIE ) {
        this.moBIE = moBIE;
        this.settings = moBIE.getSettings();
    }

    public void loadAdditionalViewsDialog() {
        try {

            String selectedFilePath = null;
            Utils.FileLocation fileLocation = loadFromProjectOrFileSystemDialog();
            if ( fileLocation == Utils.FileLocation.Project ) {
                selectedFilePath = selectPathFromProject( moBIE.getDatasetPath("misc", "views" ), "View" );
            } else {
                selectedFilePath = selectOpenPathFromFileSystem( "View" );
            }

            // to match to the existing view selection panels, we enable the mobie look and feel
            UserInterfaceHelper.setMoBIESwingLookAndFeel();

            if (selectedFilePath != null) {
                Map< String, View> views = new AdditionalViewsJsonParser().getViews( selectedFilePath ).views;
                moBIE.getUserInterface().addViews( views );
            }

            UserInterfaceHelper.resetSystemSwingLookAndFeel();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
