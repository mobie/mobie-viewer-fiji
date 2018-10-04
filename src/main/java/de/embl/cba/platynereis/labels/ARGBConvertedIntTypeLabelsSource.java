package de.embl.cba.platynereis.labels;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.volatiles.*;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.lang.invoke.WrongMethodTypeException;

public class ARGBConvertedIntTypeLabelsSource implements Source<VolatileARGBType> {
    private long setupId;
    private SpimData spimData;
    private ViewerSetupImgLoader<?, ?> setupImgLoader;

    final private InterpolatorFactory<VolatileARGBType, RandomAccessible<VolatileARGBType>>[] interpolatorFactories;
    {
        interpolatorFactories = new InterpolatorFactory[]{
                new NearestNeighborInterpolatorFactory<VolatileARGBType>(),
                new ClampingNLinearInterpolatorFactory<VolatileARGBType>()
        };
    }

    public ARGBConvertedIntTypeLabelsSource( SpimData spimdata, final int setupId )
    {
        this.setupId = setupId;
        this.spimData = spimdata;
        ViewerImgLoader imgLoader = (ViewerImgLoader) this.spimData.getSequenceDescription().getImgLoader();
        this.setupImgLoader = imgLoader.getSetupImgLoader(setupId);
        try
        {
            AbstractVolatileNativeRealType type = (AbstractVolatileNativeRealType) setupImgLoader.getVolatileImageType();
            if (! ( type instanceof VolatileUnsignedShortType
                    || type instanceof VolatileUnsignedByteType
                    || type instanceof VolatileLongType )) {
                throw new Exception("Data type not matching.");
            }
        }
        catch ( Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPresent(final int t) {
        boolean flag = t >= 0 && t < this.spimData.getSequenceDescription().getTimePoints().size();
        return flag;
    }

    @Override
    public RandomAccessibleInterval<VolatileARGBType> getSource(final int t, final int mipMapLevel) {
        RandomAccessibleInterval image = setupImgLoader.getImage(t, mipMapLevel);
        RandomAccessibleInterval<VolatileARGBType> output = Converters.convert( image, new VolatileIntTypeLabelsARGBConverter(), new VolatileARGBType() );
        return output;
    }

    @Override
    public RealRandomAccessible<VolatileARGBType> getInterpolatedSource(final int t, final int level, final Interpolation method) {
        final ExtendedRandomAccessibleInterval<VolatileARGBType, RandomAccessibleInterval<VolatileARGBType>> extendedSource =
                Views.extendValue(getSource(t, level), new VolatileARGBType(0));
        switch (method) {
            case NLINEAR:
                return Views.interpolate(extendedSource, interpolatorFactories[1]);
            default:
                return Views.interpolate(extendedSource, interpolatorFactories[0]);
        }
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        transform.set(this.setupImgLoader.getMipmapTransforms()[level]);
    }

    @Override
    public VolatileARGBType getType() {
        return new VolatileARGBType();
    }

    @Override
    public String getName() {
        return setupId + "";
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return null;
    }

    @Override
    public int getNumMipmapLevels() {
        return setupImgLoader.getMipmapTransforms().length;
    }
}
