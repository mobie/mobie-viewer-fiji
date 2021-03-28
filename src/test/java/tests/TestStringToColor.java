package tests;

import de.embl.cba.mobie.utils.Utils;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.junit.Test;

import java.awt.*;

public class TestStringToColor
{
	@Test
	public void stringToColor()
	{
		final Color green = Utils.getColor("green");
		final Color blue = Utils.getColor("blue");
		System.out.println("Test");

		//new N5AmazonS3Reader(  )
	}

	public static void main( String[] args )
	{
		new TestStringToColor().stringToColor();
	}
}
