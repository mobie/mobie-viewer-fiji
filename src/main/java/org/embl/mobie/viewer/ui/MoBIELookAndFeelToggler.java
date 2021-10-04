package org.embl.mobie.viewer.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class MoBIELookAndFeelToggler {

    private static LookAndFeel before;

    private static boolean isMoBIELaf = false;

    private static void storeCurrentLaf() {
        before = UIManager.getLookAndFeel();
    }

    public static void setMoBIELaf() {
        if ( isMoBIELaf ) return;
        storeCurrentLaf();
        FlatLightLaf.install();
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        isMoBIELaf = true;
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void resetMoBIELaf() {
        // TODO: reset where the menu bar is?
        try {
            UIManager.setLookAndFeel( before );
            isMoBIELaf = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
