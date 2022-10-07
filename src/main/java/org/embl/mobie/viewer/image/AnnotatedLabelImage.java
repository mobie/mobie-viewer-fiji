package org.embl.mobie.viewer.image;

import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;

public interface AnnotatedLabelImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();

	Image< ? extends IntegerType< ? > > getLabelImage();
}
