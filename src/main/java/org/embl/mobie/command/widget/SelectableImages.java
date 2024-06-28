package org.embl.mobie.command.widget;

import java.util.List;

public class SelectableImages
{
    private final List< String > names;

    public SelectableImages( List< String > names )
    {
        this.names = names;
    }

    public SelectableImages()
    {
        this.names = null;
    }

    public List< String > getNames()
    {
        return names;
    }
}
