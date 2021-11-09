package org.embl.mobie.viewer.view.additionalviews;

import ij.IJ;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.Utils;
import org.embl.mobie.viewer.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.viewer.ui.MoBIELookAndFeelToggler;
import org.embl.mobie.viewer.ui.UserInterfaceHelper;
import org.embl.mobie.viewer.view.View;

import java.io.IOException;
import java.util.Map;

public class AdditionalViewsLoader {

    private MoBIE moBIE;

    public AdditionalViewsLoader ( MoBIE moBIE ) {
        this.moBIE = moBIE;
    }

    public void loadAdditionalViewsDialog() {
        try {
            String selectedFilePath = null;
            Utils.FileLocation fileLocation = Utils.loadFromProjectOrFileSystemDialog();
            if ( fileLocation == Utils.FileLocation.Project ) {
                selectedFilePath = Utils.selectPathFromProject( moBIE.getDatasetPath("misc", "views" ), "View" );
            } else {
                selectedFilePath = Utils.selectOpenPathFromFileSystem( "View" );
            }

            if (selectedFilePath != null) {
                MoBIELookAndFeelToggler.setMoBIELaf();
                loadViews( selectedFilePath );
                MoBIELookAndFeelToggler.resetMoBIELaf();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadViews( String selectedFilePath ) throws IOException
    {
        Map< String, View> views = new AdditionalViewsJsonParser().getViews( selectedFilePath ).views;
        moBIE.getViews().putAll( views );
        moBIE.getUserInterface().addViews( views );
        IJ.log( "New views loaded from:\n" + selectedFilePath );
    }
}
