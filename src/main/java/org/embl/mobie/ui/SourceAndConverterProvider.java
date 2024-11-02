package org.embl.mobie.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.lib.util.MoBIEHelper;

import java.util.List;
import java.util.stream.Collectors;

public class SourceAndConverterProvider
{
    private final BdvHandle bdvHandle;
    private final List< ? extends SourceAndConverter< ? > > sacs;
    private boolean onlyVisible;

    public SourceAndConverterProvider( BdvHandle bdvHandle,
                                       List< ? extends SourceAndConverter< ? > > sacs )
    {
        this.bdvHandle = bdvHandle;
        this.sacs = sacs;
    }

    public List< ? extends SourceAndConverter< ? > > get()
    {
        if ( onlyVisible )
        {
            return MoBIEHelper.getVisibleSacsInCurrentView( bdvHandle )
                    .stream()
                    .filter( sac -> sacs.contains( sac ) )
                    .collect( Collectors.toList() );
        }
        else
        {
            return sacs;
        }
    }

    public void onlyVisible( boolean onlyVisible )
    {
        this.onlyVisible = onlyVisible;
    }
}
