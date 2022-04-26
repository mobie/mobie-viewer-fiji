package debug;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.ThreadUtils;

import java.io.IOException;

public class DebugMultiThreadedIOCovidIF
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void run() throws IOException
	{
		ThreadUtils.setnIoThreads( 16 );
		new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.OmeZarrS3 ).view( "boundary-segment-test" ) ).close();
		//"default_transform_grid_with_tables_with_segmentations_without_segmentation_tables"
	}
}
