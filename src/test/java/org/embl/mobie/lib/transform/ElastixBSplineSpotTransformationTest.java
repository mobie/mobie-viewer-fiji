package org.embl.mobie.lib.transform;

import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.DefaultAnnotationAdapter;
import org.embl.mobie.lib.image.AnnotatedLabelImage;
import org.embl.mobie.lib.image.DefaultAnnotatedLabelImage;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.SpotLabelImage;
import org.embl.mobie.lib.serialize.transformation.ElastixBSplineTransformation;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpot;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedSpotCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import org.embl.mobie.lib.table.columns.ColumnNames;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RealRandomAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import org.embl.mobie.lib.source.AnnotationType;

import java.io.File;
import java.net.URL;
import java.util.Collections;

class ElastixBSplineSpotTransformationTest
{
	@Test
	void transformsSpotAnnotationsWithElastixBSpline() throws Exception
	{
		final AnnotatedLabelImage< ? > spotImage = createSingleSpotImage( 1, 20.0, 30.0, 40.0 );

		final URL transformResource = ElastixBSplineSpotTransformationTest.class
				.getResource( "/elastix/TransformParameters.BSpline3D.TranslationX.txt" );
		Assertions.assertNotNull( transformResource, "Missing 3D Elastix transform test resource" );

		final ElastixBSplineTransformation transformation = new ElastixBSplineTransformation(
				"bspline-3d",
				new File( transformResource.toURI() ).getAbsolutePath(),
				Collections.singletonList( "spots" ),
				Collections.singletonList( "spots-bspline" ) );

		final Image< ? > transformed = ImageTransformer.elastixBSplineTransform( ( Image< ? > ) spotImage, transformation, false );
		Assertions.assertTrue( transformed instanceof AnnotatedLabelImage );
		final AnnotatedLabelImage< ? > transformedAnnotated = ( AnnotatedLabelImage< ? > ) transformed;

		final Annotation annotation = transformedAnnotated.getAnnData().getTable().annotation( 0 );
		Assertions.assertTrue( annotation instanceof AnnotatedSpot );
		final AnnotatedSpot spot = ( AnnotatedSpot ) annotation;

		Assertions.assertEquals( 10.0, spot.getDoublePosition( 0 ), 1e-6 );
		Assertions.assertEquals( 30.0, spot.getDoublePosition( 1 ), 1e-6 );
		Assertions.assertEquals( 40.0, spot.getDoublePosition( 2 ), 1e-6 );

		// Validate that the transformed label image and transformed spot coordinates are consistent.
		final long transformedCenterLabel = sampleLabelAtGlobalPosition( transformedAnnotated, new double[]{ 10.0, 30.0, 40.0 } );
		final long originalCenterLabel = sampleLabelAtGlobalPosition( transformedAnnotated, new double[]{ 20.0, 30.0, 40.0 } );
		Assertions.assertEquals( 1L, transformedCenterLabel );
		Assertions.assertEquals( 0L, originalCenterLabel );
	}

	private static long sampleLabelAtGlobalPosition( final AnnotatedLabelImage< ? > annotatedImage, final double[] globalPosition )
	{
		final Source< ? > source = annotatedImage.getSourcePair().getSource();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );

		final double[] localPosition = new double[ globalPosition.length ];
		sourceTransform.inverse().apply( globalPosition, localPosition );

		final RealRandomAccess< ? > access = source
				.getInterpolatedSource( 0, 0, Interpolation.NEARESTNEIGHBOR )
				.realRandomAccess();
		access.setPosition( localPosition );
		final AnnotationType< ? > annotationType = ( AnnotationType< ? > ) access.get();
		final Object annotation = annotationType.getAnnotation();
		if ( annotation == null )
			return 0L;
		return ( ( Annotation ) annotation ).label();
	}

	private static AnnotatedLabelImage< ? > createSingleSpotImage( final int id, final double x, final double y, final double z )
	{
		final Table table = Table.create( "spots" )
				.addColumns(
						IntColumn.create( ColumnNames.SPOT_ID, new int[]{ id } ),
						DoubleColumn.create( ColumnNames.SPOT_X, new double[]{ x } ),
						DoubleColumn.create( ColumnNames.SPOT_Y, new double[]{ y } ),
						DoubleColumn.create( ColumnNames.SPOT_Z, new double[]{ z } ),
						IntColumn.create( ColumnNames.TIMEPOINT, new int[]{ 0 } )
				);

		final TableSawAnnotationTableModel< TableSawAnnotatedSpot > tableModel = new TableSawAnnotationTableModel<>(
				"spots",
				new TableSawAnnotatedSpotCreator( table ),
				null,
				null,
				table );

		final DefaultAnnData< AnnotatedSpot > annData = new DefaultAnnData<>( ( org.embl.mobie.lib.table.AnnotationTableModel ) tableModel );
		final SpotLabelImage< AnnotatedSpot, ? > spotLabelImage = new SpotLabelImage<>(
				"spots",
				"pixel",
				annData,
				1.0,
				new double[]{ 0, 0, 0 },
				new double[]{ 64, 64, 64 } );

		final DefaultAnnotationAdapter< AnnotatedSpot > annotationAdapter = new DefaultAnnotationAdapter<>( annData );
		return new DefaultAnnotatedLabelImage<>( spotLabelImage, annData, annotationAdapter );
	}
}

