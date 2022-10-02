package develop;

import net.imagej.ImageJ;
import org.scijava.log.LogService;

public class DevelopLogService
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final LogService log = imageJ.log();

		log.log( 0, "Level 0");
		log.log( 1, "Level 1");
		log.log( 2, "Level 2");
		log.info( "Info" );
	}
}
