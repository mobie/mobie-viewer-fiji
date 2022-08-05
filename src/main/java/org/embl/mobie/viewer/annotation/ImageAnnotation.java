package org.embl.mobie.viewer.annotation;

import bdv.viewer.Source;
import net.imglib2.RealInterval;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.SourceHelper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface ImageAnnotation extends Region, Annotation
{
}
