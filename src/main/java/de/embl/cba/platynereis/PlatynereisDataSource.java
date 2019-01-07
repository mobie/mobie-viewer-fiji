package de.embl.cba.platynereis;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.selection.BdvSelectionEventHandler;
import de.embl.cba.bdv.utils.sources.SelectableVolatileARGBConvertedRealSource;
import mpicbg.spim.data.SpimData;

import java.awt.*;
import java.io.File;

public class PlatynereisDataSource
{
    public SpimData spimData;

    public SpimDataMinimal spimDataMinimal;
    public boolean isSpimDataMinimal = false;

    public SelectableVolatileARGBConvertedRealSource labelSource;
    public BdvSelectionEventHandler bdvSelectionEventHandler;
    public boolean isLabelSource = false;

    // Display
    public BdvStackSource bdvStackSource;

    // Other
    public File file;
    public Integer maxLutValue;
    public boolean isActive;
    public Color color;
    public String name;

    public File attributeFile;
}