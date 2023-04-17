package hcs;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;

import java.io.IOException;

class HCSOMEZarr
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		new ImageJ().ui().showUI();

		new MoBIE( "/Users/tischer/Desktop/hcs-ome.zarr", new MoBIESettings(), 0.1, 0.0  );
	}
}