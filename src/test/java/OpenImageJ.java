import net.imagej.ImageJ;

public class OpenImageJ
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
	}
}
