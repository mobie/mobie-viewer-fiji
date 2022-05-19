package org.embl.mobie.viewer.view;

import ij.IJ;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.viewer.ui.MoBIELookAndFeelToggler;
import org.embl.mobie.viewer.view.View;

import java.io.IOException;
import java.util.Map;

public class AdditionalViewsLoader {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private MoBIE moBIE;

    public AdditionalViewsLoader ( MoBIE moBIE ) {
        this.moBIE = moBIE;
    }

    public void loadAdditionalViewsDialog() {
        new Thread( () -> {
            try {
                String selectedFilePath = null;
                MoBIEHelper.FileLocation fileLocation = MoBIEHelper.loadFromProjectOrFileSystemDialog();
                if ( fileLocation == null )
                    return;
                if ( fileLocation == MoBIEHelper.FileLocation.Project ) {
                    selectedFilePath = MoBIEHelper.selectPathFromProject( moBIE.getDatasetPath("misc", "views" ), "View" );
                } else {
                    selectedFilePath = MoBIEHelper.selectFilePath( "json", "View", true );
                }

                if (selectedFilePath != null) {
                    MoBIELookAndFeelToggler.setMoBIELaf();
                    loadViews( selectedFilePath );
                    MoBIELookAndFeelToggler.resetMoBIELaf();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void loadViews( String selectedFilePath ) throws IOException
    {
        Map< String, View> views = new AdditionalViewsJsonParser().getViews( selectedFilePath ).views;
        moBIE.getViews().putAll( views );
        moBIE.getUserInterface().addViews( views );
        IJ.log( "New views loaded from:\n" + selectedFilePath );
    }
}
