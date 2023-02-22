package org.embl.mobie;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MoBIEHCSTest
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		// TODO: make BioFormats ignore the XML file! => Ask Nico and Josh and DGault how to do this
		// TODO: if files endwith TIFF just use ImageJ to open?
		new ImageJ();
		final MoBIESettings settings = new MoBIESettings();
		settings.isHCSProject( true );
		new MoBIE( "/Users/tischer/Downloads/OperettaHarmony4_1_dataexport", settings );
	}
}