package org.embl.mobie;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MoBIEHCSTest
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		new ImageJ().ui().showUI();
		final MoBIESettings settings = new MoBIESettings();
		settings.isHCSProject( true );
		new MoBIE( "/Users/tischer/Downloads/Operetta", settings );
	}
}