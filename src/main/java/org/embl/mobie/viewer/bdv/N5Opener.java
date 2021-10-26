package org.embl.mobie.viewer.bdv;

import bdv.util.volatiles.SharedQueue;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;
import org.embl.mobie.io.util.openers.BDVOpener;

import java.io.File;
import java.io.IOException;

public class N5Opener extends BDVOpener
{
    private final String filePath;

    public N5Opener(String filePath) {
        this.filePath = filePath;
    }

    public static SpimData openFile(String filePath, SharedQueue sharedQueue) throws IOException
    {
        N5Opener omeZarrOpener = new N5Opener(filePath);
        return omeZarrOpener.readFile(sharedQueue);
    }

    private SpimData readFile(SharedQueue sharedQueue) throws IOException
    {
        N5FSImageLoader imageLoader = new N5FSImageLoader(new File("/home/katerina/Documents/embl/mnt2/kreshuk/pape/Work/mobie/arabidopsis-root-lm-datasets/data/arabidopsis-root/images/local/lm-cells.n5"), sharedQueue);
        return new SpimData(
                new File(this.filePath),
                Cast.unchecked( imageLoader.getSequenceDescription() ),
                imageLoader.getViewRegistrations());
    }

}
