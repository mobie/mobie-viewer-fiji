package org.embl.mobie.lib.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoBIEHelperTest
{
	private Locale defaultLocale;

	@BeforeEach
	void setUp()
	{
		defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterEach
	void tearDown()
	{
		Locale.setDefault(defaultLocale);
	}

	@Test
	void shouldPrintArrayUsingFullPrecisionWhenSignificantDigitsIsMinusOne()
	{
		double[] values = { Math.PI, -1e-12, 2.0 };
		String printed = MoBIEHelper.print(values, -1);

		assertEquals("(" + Math.PI + ", 0.0, 2.0)", printed);
		assertEquals(-1e-12, values[1]);
	}

	@Test
	void shouldPrintSingleValueUsingFullPrecisionWhenSignificantDigitsIsMinusOne()
	{
		assertEquals("" + Math.PI, MoBIEHelper.print(Math.PI, -1));
	}

	@Test
	void shouldKeepExistingFormattingForPositiveSignificantDigits()
	{
		double[] values = { Math.PI, 2.0 };
		assertEquals("(3.14, 2)", MoBIEHelper.print(values, 2));
	}
}
