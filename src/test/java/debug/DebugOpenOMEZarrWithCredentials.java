package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

public class DebugOpenOMEZarrWithCredentials
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        OpenOMEZARRCommand command = new OpenOMEZARRCommand();
        command.imageUri = "https://s3.embl.de/imatrec/IMATREC_HiTT_20240501_AS/135.ome.zarr";
        command.s3AccessKey = "PyPxwMSjp23EX5ENWspO";
        command.s3SecretKey = "3DBknEts4Cc8IuYzxNNDqFVGPvMHVJDroomYXdJl";
        command.run();
    }
}
