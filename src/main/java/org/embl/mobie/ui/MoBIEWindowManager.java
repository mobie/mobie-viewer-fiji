package org.embl.mobie.ui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

public abstract class MoBIEWindowManager
{
    private static final Set< JFrame > openWindows = new HashSet< >();

    public static void addWindow( JFrame frame )
    {
        // Add window to the list
        openWindows.add( frame );

        // Add listener to remove window from list when it's closed
        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosed( WindowEvent e ) {
                openWindows.remove(frame);
            }
        });
    }

    public static void closeAllWindows() {
        new HashSet<>(openWindows).forEach(JFrame::dispose);
    }
}
