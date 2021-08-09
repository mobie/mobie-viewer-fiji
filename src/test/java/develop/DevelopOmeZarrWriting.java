package develop;

import de.embl.cba.mobie.projectcreator.n5.DownsampleBlock;
import de.embl.cba.mobie.projectcreator.n5.WriteImgPlusToN5BdvOmeZarr;
import de.embl.cba.mobie.projectcreator.n5.WriteImgPlusToN5OmeZarr;
import ij.IJ;
import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.GzipCompression;

public class DevelopOmeZarrWriting {
    public static void main( String[] args ) {
        ImagePlus imp = IJ.openImage("C:\\Users\\meechan\\Documents\\test_images\\zebrafish\\0B51F8B46C_8bit_lynEGFP.tif");
        new WriteImgPlusToN5OmeZarr().export(imp, "C:\\Users\\meechan\\Documents\\temp\\test_zarr_writing\\omezarr\\zyx.ome.zarr",
                DownsampleBlock.DownsamplingMethod.Average, new GzipCompression());

        new WriteImgPlusToN5BdvOmeZarr().export(imp, "C:\\Users\\meechan\\Documents\\temp\\test_zarr_writing\\omezarr\\bdvzyx.xml",
                DownsampleBlock.DownsamplingMethod.Average, new GzipCompression());
    }
}
