package examples;

import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

public class OpenProjectTable
{
    public static void main( String[] args )
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        final MoBIE moBIE = new MoBIE( "src/test/resources/project-tables/clem-table.txt",
                new MoBIESettings(),
                true );
    }
}
