package tests;

import de.embl.cba.mobie.utils.Utils;
import org.junit.Test;

import java.awt.*;

public class TestStringToColor
{
	@Test
	public void stringToColor()
	{
		final Color green = Utils.getColor("green");
		final Color blue = Utils.getColor("blue");
	}

	public static void main( String[] args )
	{
		new TestStringToColor().stringToColor();
	}
}
