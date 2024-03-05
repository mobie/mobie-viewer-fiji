package projects.lysosomal_lipids;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenTableCommand;

import java.io.File;

public class OpenHeikoCellTable
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenTableCommand command = new OpenTableCommand();
        command.root = null; // new File( "/Volumes/cba/exchange/lysosomal-lipids/oro/cp-out" );
        command.table = new File( "/Volumes/cba/exchange/lysosomal-lipids/oro/cp-out/cells.txt" );
        command.pathMapping = "/g/,/Volumes/";
        command.images = "FileName_dapiRaw=DAPI,FileName_cherryRaw=Cherry,FileName_gfpRaw=GFP";
        command.labels = "FileName_cells=cells,FileName_nuclei=nuclei,FileName_vesicle=vesicles";
        command.removeSpatialCalibration = true;
        command.run();
    }
}
