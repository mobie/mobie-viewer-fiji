package org.embl.mobie.viewer.bdv;

import bdv.util.volatiles.SharedQueue;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.util.Cast;
import org.embl.mobie.io.util.openers.BDVOpener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
        SpimData spimData = new SpimData(
                new File(this.filePath),
                Cast.unchecked( imageLoader.getSequenceDescription() ),
                imageLoader.getViewRegistrations());
//        if (spimData.getSequenceDescription().) {
//            ArrayList<V> list = new ArrayList();
//            Iterator var2 = this.setups.values().iterator();
//
//            while(var2.hasNext()) {
//                V setup = (BasicViewSetup)var2.next();
//                list.add(setup);
//            }
//
//            this.viewSetupsOrdered = Entity.sortById(list);
//            this.viewSetupsOrderedDirty = false;
//        }
        return spimData;
    }

}
