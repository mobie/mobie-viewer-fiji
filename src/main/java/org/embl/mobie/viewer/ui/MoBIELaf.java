package org.embl.mobie.viewer.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class MoBIELaf
{
    private static LookAndFeel systemLaf;
    private static boolean isMoBIELaf = false;

    private static void storeSystemLaf() {
        systemLaf = UIManager.getLookAndFeel();
    }

    public static void MoBIELafOn() {
        if ( isMoBIELaf ) return;
        storeSystemLaf();
        FlatLightLaf.install();
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        isMoBIELaf = true;
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void MoBIELafOff() {
        try {
            UIManager.setLookAndFeel( systemLaf );
            isMoBIELaf = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
