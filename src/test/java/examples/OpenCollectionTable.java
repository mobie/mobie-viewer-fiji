package examples;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

public class OpenCollectionTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

//        String tablePath = "src/test/resources/project-tables/clem-table.txt";
        String tablePath = "src/test/resources/project-tables/blobs-table.txt";

        new MoBIE( tablePath, new MoBIESettings(), true );
    }
}
