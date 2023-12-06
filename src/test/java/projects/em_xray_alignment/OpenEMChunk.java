package projects.em_xray_alignment;

import org.embl.mobie.command.open.OpenImageAndLabelsCommand;

import java.io.File;

public class OpenEMChunk
{
    public static void main( String[] args )
    {
        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = new File( "/Volumes/cba/exchange/em-xray-alignment-data/0/em-zarr/test.ome.zarr" );
        command.run();
    }
}
