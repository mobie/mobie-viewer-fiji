package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import java.io.IOException;

class MoBIETest {

    @org.junit.jupiter.api.Test
    void openPlatybrowser() throws IOException {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        new MoBIE( "https://github.com/mobie/platybrowser-datasets", MoBIESettings.settings().gitProjectBranch( "spec-v2" ) );
    }

}