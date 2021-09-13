package org.embl.mobie.viewer.color;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public interface VolatileRealARGBColorConverter extends ColorConverter, Converter< Volatile< RealType >, ARGBType >
{
}
