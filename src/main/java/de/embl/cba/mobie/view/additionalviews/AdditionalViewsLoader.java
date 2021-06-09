package de.embl.cba.mobie.view.additionalviews;

import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie.ui.UserInterfaceHelper;
import de.embl.cba.mobie.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.mobie.Utils.selectPathsFromProject;

public class AdditionalViewsLoader {

    private MoBIE moBIE;
    private MoBIESettings settings;

    public AdditionalViewsLoader ( MoBIE moBIE ) {
        this.moBIE = moBIE;
        this.settings = moBIE.getSettings();
    }

    public void loadAdditionalViewsDialog() {
        try {
            ArrayList<String> directories = new ArrayList<>();
            directories.add( moBIE.getDatasetPath("misc", "views" ) );

            String selectedFilePath = selectPathsFromProject( directories, "View" ).get(0);
            // to match to the existing view selection panels, we enable the cross platform look and feel
            UserInterfaceHelper.resetCrossPlatformSwingLookAndFeel();

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
