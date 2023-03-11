package develop;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

class HCS
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		new ImageJ().ui().showUI();

		//new MoBIE( "/Users/tischer/Downloads/Operetta", new MoBIESettings(), 0.1, 0.0  );

		//new MoBIE( "/Users/tischer/Downloads/IncuCyte", new MoBIESettings(), 0.1, 0.0 );

		//new MoBIE( "/Users/tischer/Downloads/incu2/", new MoBIESettings(), 0.1, 0.0  );

		//new MoBIE( "/g/cba/exchange/mobie-data/incucyte-drug-screen/images", new MoBIESettings(), 0.1, 0.0  );

		//new MoBIE( "/Users/tischer/Documents/faim-hcs/resources/MIP-2P-2sub/2022-07-05/1075", new MoBIESettings(), 0.1, 0.0  );

		new MoBIE( "/Volumes/cba/exchange/hcs-test/Samples", new MoBIESettings(), 0.1, 0.0  );
	}
}