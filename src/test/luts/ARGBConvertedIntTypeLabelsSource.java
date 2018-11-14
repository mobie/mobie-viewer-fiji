package de.embl.cba.platynereis.labels.luts;

import bdv.AbstractViewerSetupImgLoader;
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
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.volatiles.*;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class ARGBConvertedIntTypeLabelsSource implements Source< VolatileARGBType > {
    private long setupId;
    private SpimData spimData;
    private AbstractViewerSetupImgLoader< IntType, VolatileIntType > setupImgLoader;

    final private InterpolatorFactory< VolatileARGBType, RandomAccessible< VolatileARGBType > >[] interpolatorFactories;
    private AffineTransform3D viewRegistration;
    private AffineTransform3D[] mipmapTransforms;
    private VolatileIntTypeLabelsARGBConverter volatileIntTypeLabelsARGBConverter;

    {
        interpolatorFactories = new InterpolatorFactory[]{
                new NearestNeighborInterpolatorFactory< VolatileARGBType >(),
                new ClampingNLinearInterpolatorFactory< VolatileARGBType >()
        };
    }

    public ARGBConvertedIntTypeLabelsSource( SpimData spimdata, final int setupId )
    {
        this.setupId = setupId;
        this.spimData = spimdata;
        this.viewRegistration = spimData.getViewRegistrations().getViewRegistration( 0, 0 ).getModel();
        ViewerImgLoader imgLoader = ( ViewerImgLoader ) this.spimData.getSequenceDescription().getImgLoader();
        this.setupImgLoader = ( AbstractViewerSetupImgLoader ) imgLoader.getSetupImgLoader( setupId );
        this.mipmapTransforms = this.setupImgLoader.getMipmapTransforms();

        volatileIntTypeLabelsARGBConverter = new VolatileIntTypeLabelsARGBConverter();

        try
        {
            AbstractVolatileNativeRealType type = setupImgLoader.getVolatileImageType();
            if (! ( type instanceof VolatileUnsignedByteType
					|| type instanceof VolatileUnsignedShortType
                    || type instanceof VolatileUnsignedLongType )) {
                throw new Exception("Data type not supported for label LUTs: " + type.toString() );
            }
        }
        catch ( Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public boolean isPresent( final int t )
    {
        boolean flag = t >= 0 && t < this.spimData.getSequenceDescription().getTimePoints().size();
        return flag;
    }

    @Override
    public RandomAccessibleInterval< VolatileARGBType > getSource( final int t, final int mipMapLevel )
    {
        return Converters.convert(
                        setupImgLoader.getVolatileImage( t, mipMapLevel ),
                        volatileIntTypeLabelsARGBConverter,
                        new VolatileARGBType() );
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
    public void getSourceTransform(int t, int level, AffineTransform3D transform)
    {
        final AffineTransform3D sourceTransform = viewRegistration.copy().preConcatenate( mipmapTransforms[ level ] );
        transform.set( sourceTransform );
    }

    @Override
    public VolatileARGBType getType() {
        return new VolatileARGBType();
    }

    @Override
    public String getName() {
        return "labels";
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
