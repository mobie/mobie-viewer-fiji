package org.embl.mobie.lib.image;

import org.embl.mobie.lib.serialize.transformation.Transformation;

public interface TransformedImage extends ImageWrapper
{
    Transformation getTransformation();

    void setTransformation( Transformation transformation );
}
