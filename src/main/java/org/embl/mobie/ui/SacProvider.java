package org.embl.mobie.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SacProvider
{
    private final BdvHandle bdvHandle;
    private final List< ? extends SourceAndConverter< ? > > sacs;
    private boolean onlyVisible = false;

    public SacProvider( BdvHandle bdvHandle,
                        List< ? extends SourceAndConverter< ? > > sacs )
    {
        this.bdvHandle = bdvHandle;
        this.sacs = sacs;
    }

    public List< ? extends SourceAndConverter< ? > > get()
    {
        if ( onlyVisible )
        {
            return getOnCanvas();
        }
        else
        {
            return sacs;
        }
    }

    @NotNull
    public List< SourceAndConverter< ? > > getOnCanvas()
    {
        return MoBIEHelper.getVisibleSacsInCurrentView( bdvHandle )
                .stream()
                .filter( sac -> sacs.contains( sac ) )
                .collect( Collectors.toList() );
    }

    public void onlyVisible( boolean onlyVisible )
    {
        this.onlyVisible = onlyVisible;
    }
}
