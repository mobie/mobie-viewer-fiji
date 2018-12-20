package de.embl.cba.platynereis;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.BdvSource;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.labels.VolatileRealToRandomARGBConverter;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.awt.*;
import java.io.File;

public class PlatynereisDataSource
{
    // Loading formats
    public SpimData spimData; // default

    public SpimDataMinimal spimDataMinimal;
    public boolean isSpimDataMinimal = false;

    public Source< VolatileARGBType > labelSource;
    public boolean isLabelSource = false;

    // Display
    public BdvStackSource bdvStackSource;

    // Other
    public File file;
    public Integer maxLutValue;
    public boolean isActive;
    public Color color;
    public String name;

    public VolatileRealToRandomARGBConverter labelSourceConverter;
    public File attributeFile;
}