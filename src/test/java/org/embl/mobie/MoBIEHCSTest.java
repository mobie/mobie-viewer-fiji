package org.embl.mobie;

import mpicbg.spim.data.SpimDataException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MoBIEHCSTest
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		final MoBIESettings settings = new MoBIESettings();
		settings.isHCSProject( true );
		new MoBIE( "/Users/tischer/Downloads/OperettaHarmony4_1_dataexport", settings );
	}
}