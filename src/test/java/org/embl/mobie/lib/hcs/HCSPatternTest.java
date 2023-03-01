package org.embl.mobie.lib.hcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HCSPatternTest
{
	private static String operetta = "r01c01f04p01-ch1sk1fk1fl1.tiff";
	private static String incucyte = "MiaPaCa2-PhaseOriginal_A2_1_03d06h40m.tif";
	private static String moldev = "MIP-2P-2sub_C05_s1_w146C9B2CD-0BB3-4B8A-9187-2805F4C90506.tif";


	@Test
	void fromPath()
	{
		final HCSPattern operetta = HCSPattern.fromPath( HCSPatternTest.operetta );
		assertTrue( operetta.equals( HCSPattern.Operetta ) );

		final HCSPattern incucyte = HCSPattern.fromPath( HCSPatternTest.incucyte );
		assertTrue( incucyte.equals( HCSPattern.IncuCyte ) );

		final HCSPattern moldev = HCSPattern.fromPath( HCSPatternTest.moldev );
		assertTrue( moldev.equals( HCSPattern.MolecularDevices ) );
	}
}