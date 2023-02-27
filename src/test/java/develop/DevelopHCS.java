package develop;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DevelopHCS
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		new ImageJ().ui().showUI();

		new MoBIE( "/Users/tischer/Downloads/Operetta", new MoBIESettings(), 0.1, 0.0  );

		//new MoBIE( "/Users/tischer/Downloads/IncuCyte", new MoBIESettings(), 0.1, 0.0 );
	}
}