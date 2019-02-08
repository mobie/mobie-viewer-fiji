import de.embl.cba.platynereis.platybrowser.ExplorePlatynereisAtlasCommand;
import net.imagej.ImageJ;

public class RunExplorePlatynereisAtlasCommand
{

	public static void main(final String... args)
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		ij.command().run( ExplorePlatynereisAtlasCommand.class, true );
	}

}
