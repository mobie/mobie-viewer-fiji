package hcs;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

class HCSOperetta5
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		new ImageJ().ui().showUI();
		new MoBIE( "/Volumes/cba/exchange/hcs-test/operetta5/CLSsamples", new MoBIESettings(), 0.1, 0.0  );
	}
}