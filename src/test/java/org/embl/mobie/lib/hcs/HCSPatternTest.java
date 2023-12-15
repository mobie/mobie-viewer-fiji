/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.hcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HCSPatternTest
{
	private static String operetta = "r01c01f04p01-ch1sk1fk1fl1.tiff";
	private static String incucyte = "MiaPaCa2-PhaseOriginal_A2_1_03d06h40m.tif";
	private static String moldev = "MIP-2P-2sub_C05_s1_w146C9B2CD-0BB3-4B8A-9187-2805F4C90506.tif";
	private static String incucyteRaw = "/Users/tischer/Downloads/incu-test-data/2207/19/1110/262/B3-1-C2.tif";
	private static  String incell = "A - 01(fld 1 wv Green - dsRed z 3).tif";

	@Test
	void fromPath()
	{
		final HCSPattern operetta = HCSPattern.fromPath( HCSPatternTest.operetta );
		assertTrue( operetta.equals( HCSPattern.Operetta ) );

		final HCSPattern incucyte = HCSPattern.fromPath( HCSPatternTest.incucyte );
		assertTrue( incucyte.equals( HCSPattern.IncuCyte ) );

		final HCSPattern moldev = HCSPattern.fromPath( HCSPatternTest.moldev );
		assertTrue( moldev.equals( HCSPattern.MolecularDevices ) );

		final HCSPattern incucyteRaw = HCSPattern.fromPath( HCSPatternTest.incucyteRaw );
		assertTrue( incucyteRaw.equals( HCSPattern.IncuCyteRaw ) );
	}
}
