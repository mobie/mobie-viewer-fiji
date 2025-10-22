package org.embl.mobie.command.open.project;

import net.imagej.ImageJ;

class OpenMoBIEProjectCommandTest
{
    static { net.imagej.patcher.LegacyInjector.preinit(); }

    //@Test
    public void test( )
    {
        new ImageJ().ui().showUI(); // initialise SciJava Services
        final OpenPlatyBrowserCommand command = new OpenPlatyBrowserCommand();
        command.run();
    }

    public static void main( String[] args )
    {
        new OpenMoBIEProjectCommandTest().test();
    }
}