package de.embl.cba.mobie.bdv;

import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.persist.IClassRuntimeAdapter;

/**
 * For serialization of {@link DefaultBdvSupplier} objects
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class MobieBdvSupplierAdapter implements IClassRuntimeAdapter< IBdvSupplier, MobieBdvSupplier > {

    @Override
    public Class<? extends IBdvSupplier> getBaseClass() {
        return IBdvSupplier.class;
    }

    @Override
    public Class<? extends MobieBdvSupplier > getRunTimeClass() {
        return MobieBdvSupplier.class;
    }

    public boolean useCustomAdapter() {return false;}
}
