package org.embl.mobie.ioplugin;

import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.lib.table.saw.TableOpener;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.io.location.Location;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/*
 * Examples:
 *
 * fiji://open/url?p=https://docs.google.com/spreadsheets/d/1hj_JKnBLp1nJzeSG6mcL6INIsFH2meKzNx59vmYL53Y/edit?gid=0#gid=0
 * fiji://open/url?p=https://docs.google.com/spreadsheets/d/11dd3WXS1LJRPC4B_omwAPU0JJtFI8WVwdaLJs3L2XSk/edit?usp=sharing
 *
 * */

@SuppressWarnings( "java:S110" ) // NB: deliberately extends AbstractIOPlugin, which is a long class hierarchy
@Plugin( type = IOPlugin.class, attrs = @Attr( name = "eager" ) )
public class MoBIECollectionTableIOPlugin extends AbstractIOPlugin< Object >
{
    // the "innocent" product of a (hypothetical) file reading, which Fiji will not display
    private static final Object FAKE_INPUT = new ArrayList<>( 0 );

    @Override
    public boolean supportsOpen( final Location source )
    {
        final URI uri = source.getURI();
        if ( uri == null )
            return false;

        return TableOpener.isMoBIECollectionTable( uri.toString() );
    }

    @Override
    public Object open( final Location source ) throws IOException
    {
        // URI examples for testing:
        // https://docs.google.com/spreadsheets/d/1hj_JKnBLp1nJzeSG6mcL6INIsFH2meKzNx59vmYL53Y/edit?gid=0#gid=0
        // https://docs.google.com/spreadsheets/d/11dd3WXS1LJRPC4B_omwAPU0JJtFI8WVwdaLJs3L2XSk/edit?usp=sharing
        final URI inputUri = source.getURI();
        if ( inputUri == null )
            throw new IOException( "Cannot express as a URI, and therefore not open: " + source );

        OpenCollectionTableCommand command = new OpenCollectionTableCommand();
        command.tableUri = inputUri.toString();
        command.run();

        // Returning a non-null object tells SciJava's IO subsystem the drop was fully
        // handled. It then tries to display the result, finds it cannot, and silently
        // gives up.
        return FAKE_INPUT;
    }

    @Override
    public Class< Object > getDataType()
    {
        return Object.class;
    }
}