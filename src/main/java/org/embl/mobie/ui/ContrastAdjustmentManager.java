package org.embl.mobie.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ContrastAdjustmentManager
{
    private final BdvHandle bdvHandle;
    private final List< ? extends SourceAndConverter< ? > > sacs;
    private boolean onlyVisible = false;
    private boolean individuallyAdjustVisible = false;

    public ContrastAdjustmentManager( BdvHandle bdvHandle,
                                      List< ? extends SourceAndConverter< ? > > sacs )
    {
        this.bdvHandle = bdvHandle;
        this.sacs = sacs;
    }

    public List< ? extends SourceAndConverter< ? > > getAdjustable()
    {
        if ( onlyVisible )
        {
            return getVisible();
        }
        else
        {
            return sacs;
        }
    }

    @NotNull
    public List< SourceAndConverter< ? > > getVisible()
    {
        return MoBIEHelper.getVisibleSacsInCurrentView( bdvHandle )
                .stream()
                .filter( sac -> sacs.contains( sac ) )
                .collect( Collectors.toList() );
    }

    @NotNull
    public List< ? extends SourceAndConverter< ? > > getAll()
    {
        return sacs;
    }

    public void adjustOnlyVisible( boolean onlyVisible )
    {
        this.onlyVisible = onlyVisible;
    }

    public void individuallyAdjustVisible( boolean b )
    {
        this.individuallyAdjustVisible = b;
    }

    public boolean isIndividuallyAdjustVisible()
    {
        return individuallyAdjustVisible;
    }
}
