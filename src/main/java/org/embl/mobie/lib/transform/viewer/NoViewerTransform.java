package org.embl.mobie.lib.transform.viewer;

public class NoViewerTransform implements ViewerTransform
{
    @Override
    public double[] getParameters()
    {
        return new double[ 0 ];
    }

    @Override
    public Integer getTimepoint()
    {
        return null;
    }
}
