package develop;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.embl.mobie.OMEZarrViewer;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrOpener;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener;

import java.io.IOException;

public class DevelopHeadlessOMEZarr
{
	public static void main( String[] args ) throws IOException, SpimDataException
	{
		new ImageJ();
		//imageJ.ui().showUI();
		final String filePath = "https://s3.embl.de/i2k-2020/ngff-example-data/v0.3/zyx.ome.zarr";
		final SpimData spimData = OMEZarrS3Opener.readURL(filePath);
		final OMEZarrViewer viewer = new OMEZarrViewer(spimData);
		viewer.show();
	}
}
