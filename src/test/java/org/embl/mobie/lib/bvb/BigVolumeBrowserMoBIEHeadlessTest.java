package org.embl.mobie.lib.bvb;

import net.imagej.patcher.LegacyInjector;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.MoBIEColoringModel;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.select.Listeners;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.table.AnnotationListener;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.transform.RealTransformedAnnotatedSpot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BigVolumeBrowserMoBIEHeadlessTest
{
	static
	{
		LegacyInjector.preinit();
	}

	@Test
	void acceptsGenericAnnotatedSpotImplementations()
	{
		final BigVolumeBrowserMoBIE bigVolumeBrowserMoBIE = new BigVolumeBrowserMoBIE();
		final SpotDisplay< ? > spotDisplay = new SpotDisplay<>( "spots" );
		spotDisplay.spotRadius = 1.0;

		final AnnotatedSpot rawSpot = new DummyAnnotatedSpot( 7, new double[]{ 10.0, 20.0, 30.0 } );
		final AnnotatedSpot transformedSpot = new RealTransformedAnnotatedSpot<>( rawSpot, new AffineTransform3D() );
		final ArrayList< AnnotatedSpot > annotations = new ArrayList<>();
		annotations.add( transformedSpot );

		final DefaultAnnData< AnnotatedSpot > annData = new DefaultAnnData<>( new MinimalAnnotationTableModel( annotations ) );
		setAnnData( spotDisplay, annData );

		setColoringModel( spotDisplay, new MoBIEColoringModel<>( new ConstantColoringModel(), null, null, 1.0 ) );

		// bvb is intentionally left null in headless mode; reaching this NPE means
		// the loop over annotations succeeded without a ClassCastException.
		assertThrows( NullPointerException.class, () -> bigVolumeBrowserMoBIE.addSpotsToBVB( spotDisplay ) );
	}

	private static void setAnnData( final SpotDisplay< ? > spotDisplay, final DefaultAnnData< AnnotatedSpot > annData )
	{
		try
		{
			final Field annDataField = AbstractAnnotationDisplay.class.getDeclaredField( "annData" );
			annDataField.setAccessible( true );
			annDataField.set( spotDisplay, annData );
		}
		catch ( final ReflectiveOperationException e )
		{
			throw new RuntimeException( "Could not inject annotation data into SpotDisplay", e );
		}
	}

	private static void setColoringModel( final SpotDisplay< ? > spotDisplay, final MoBIEColoringModel< AnnotatedSpot > coloringModel )
	{
		try
		{
			final Field coloringModelField = AbstractAnnotationDisplay.class.getDeclaredField( "coloringModel" );
			coloringModelField.setAccessible( true );
			coloringModelField.set( spotDisplay, coloringModel );
		}
		catch ( final ReflectiveOperationException e )
		{
			throw new RuntimeException( "Could not inject coloring model into SpotDisplay", e );
		}
	}

	private static class ConstantColoringModel implements ColoringModel< AnnotatedSpot >
	{
		private final Listeners< ColoringListener > listeners = new Listeners.SynchronizedList<>();

		@Override
		public void convert( final AnnotatedSpot value, final ARGBType color )
		{
			color.set( ARGBType.rgba( 255, 0, 0, 255 ) );
		}

		@Override
		public Listeners< ColoringListener > listeners()
		{
			return listeners;
		}
	}

	private static class MinimalAnnotationTableModel implements AnnotationTableModel< AnnotatedSpot >
	{
		private final ArrayList< AnnotatedSpot > annotations;

		private MinimalAnnotationTableModel( final ArrayList< AnnotatedSpot > annotations )
		{
			this.annotations = annotations;
		}

		@Override
		public List< String > columnNames()
		{
			return Collections.emptyList();
		}

		@Override
		public List< String > numericColumnNames()
		{
			return Collections.emptyList();
		}

		@Override
		public Class< ? > columnClass( final String columnName )
		{
			return Object.class;
		}

		@Override
		public int numAnnotations()
		{
			return annotations.size();
		}

		@Override
		public int rowIndexOf( final AnnotatedSpot annotation )
		{
			return annotations.indexOf( annotation );
		}

		@Override
		public AnnotatedSpot annotation( final int rowIndex )
		{
			return annotations.get( rowIndex );
		}

		@Override
		public void loadTableChunk( final String tableChunk )
		{
		}

		@Override
		public void loadExternalTableChunk( final StorageLocation location )
		{
		}

		@Override
		public Collection< String > getAvailableTableChunks()
		{
			return Collections.emptySet();
		}

		@Override
		public LinkedHashSet< String > getLoadedTableChunks()
		{
			return new LinkedHashSet<>();
		}

		@Override
		public Pair< Double, Double > getMinMax( final String columnName )
		{
			return new ValuePair<>( Double.NaN, Double.NaN );
		}

		@Override
		public ArrayList< AnnotatedSpot > annotations()
		{
			return annotations;
		}

		@Override
		public void addStringColumn( final String columnName )
		{
		}

		@Override
		public void addNumericColumn( final String columnName )
		{
		}

		@Override
		public StorageLocation getStorageLocation()
		{
			return null;
		}

		@Override
		public void transform( final AffineTransform3D affineTransform3D )
		{
			for ( AnnotatedSpot annotation : annotations )
				annotation.transform( affineTransform3D );
		}

		@Override
		public void addAnnotationListener( final AnnotationListener< AnnotatedSpot > listener )
		{
		}
	}

	private static class DummyAnnotatedSpot implements AnnotatedSpot
	{
		private final int label;
		private final double[] position;

		private DummyAnnotatedSpot( final int label, final double[] position )
		{
			this.label = label;
			this.position = position;
		}

		@Override
		public Integer timePoint()
		{
			return 0;
		}

		@Override
		public String uuid()
		{
			return "spot-" + label;
		}

		@Override
		public String source()
		{
			return "test-source";
		}

		@Override
		public int label()
		{
			return label;
		}

		@Override
		public Object getValue( final String feature )
		{
			return null;
		}

		@Override
		public Double getNumber( final String feature )
		{
			return null;
		}

		@Override
		public void setString( final String columnName, final String value )
		{
		}

		@Override
		public void setNumber( final String columnName, final double value )
		{
		}

		@Override
		public void transform( final AffineTransform3D affineTransform3D )
		{
			affineTransform3D.apply( position, position );
		}

		@Override
		public void localize( final float[] targetPosition )
		{
			for ( int d = 0; d < position.length; d++ )
				targetPosition[ d ] = ( float ) position[ d ];
		}

		@Override
		public void localize( final double[] targetPosition )
		{
			System.arraycopy( position, 0, targetPosition, 0, position.length );
		}

		@Override
		public float getFloatPosition( final int d )
		{
			return ( float ) position[ d ];
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return position[ d ];
		}

		@Override
		public int numDimensions()
		{
			return position.length;
		}
	}
}




