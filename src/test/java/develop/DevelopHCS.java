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

		final MoBIESettings settings = new MoBIESettings();
		settings.isHCSProject( true );

		// new MoBIE( "/Users/tischer/Downloads/Operetta", settings );

		new MoBIE( "/Users/tischer/Downloads/IncuCyte", settings );

	}
}