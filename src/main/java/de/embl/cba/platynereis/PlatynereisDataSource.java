package de.embl.cba.platynereis;

import bdv.spimdata.SpimDataMinimal;
import bdv.util.BdvSource;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.awt.*;
import java.io.File;

public class PlatynereisDataSource
{
    public SpimData spimData;
    public SpimDataMinimal spimDataMinimal;
    public BdvSource bdvSource;
    public Source< VolatileARGBType > labelSource;

    public File file;
    public Integer maxLutValue;
    public boolean isActive;
    public Color color;
    public String name;
    public boolean isSpimDataMinimal = false;
    public boolean isLabelSource = false;
}