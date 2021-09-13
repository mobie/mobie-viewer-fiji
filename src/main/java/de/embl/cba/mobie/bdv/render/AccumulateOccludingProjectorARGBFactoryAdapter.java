package de.embl.cba.mobie.bdv.render;

import bdv.viewer.render.AccumulateProjectorFactory;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.IClassRuntimeAdapter;

/*
 * For serialization of {@link AccumulateOccludingProjectorARGBFactoryAdapter} objects
 *
 * Used in {@link sc.fiji.bdvpg.bdv.supplier.mobie.MobieSerializableBdvOptions}
 */
@Plugin(type = IClassRuntimeAdapter.class)
public class AccumulateOccludingProjectorARGBFactoryAdapter implements IClassRuntimeAdapter<AccumulateProjectorFactory, AccumulateOccludingProjectorARGBFactory> {
    @Override
    public Class<? extends AccumulateProjectorFactory> getBaseClass() {
        return AccumulateProjectorFactory.class;
    }

    @Override
    public Class<? extends AccumulateOccludingProjectorARGBFactory> getRunTimeClass() {
        return AccumulateOccludingProjectorARGBFactory.class;
    }

    public boolean useCustomAdapter() {return false;}
}
