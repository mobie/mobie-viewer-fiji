package org.embl.mobie.lib.hcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HCSPatternTest
{
	private static String operetta = "r01c01f04p01-ch1sk1fk1fl1.tiff";
	private static String incucyte = "MiaPaCa2-PhaseOriginal_A2_1_03d06h40m.tif";

	@Test
	void fromPath()
	{
		final HCSPattern operetta = HCSPattern.fromPath( HCSPatternTest.operetta );
		assertTrue( operetta.equals( HCSPattern.Operetta ) );

		final HCSPattern incucyte = HCSPattern.fromPath( HCSPatternTest.incucyte );
		assertTrue( incucyte.equals( HCSPattern.IncuCyte ) );
	}
}