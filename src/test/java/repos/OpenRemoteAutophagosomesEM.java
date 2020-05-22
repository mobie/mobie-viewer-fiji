package repos;

import de.embl.cba.platynereis.dataset.DatasetsParser;
import de.embl.cba.platynereis.platybrowser.MoBIEViewer;
import net.imagej.ImageJ;

import java.util.ArrayList;

public class OpenRemoteAutophagosomesEM
{
	public static void main( String[] args )
	{
		new ImageJ().ui().showUI();

		final MoBIEViewer moBIEViewer = new MoBIEViewer(
				"https://github.com/constantinpape/autophagosomes-clem",
				"https://github.com/constantinpape/autophagosomes-clem" );
	}
}
