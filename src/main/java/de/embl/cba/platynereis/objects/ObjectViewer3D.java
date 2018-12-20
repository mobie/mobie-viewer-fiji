package de.embl.cba.platynereis.objects;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.objects.BdvObjectExtractor;
import de.embl.cba.platynereis.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import java.util.ArrayList;

public class ObjectViewer3D
{

	private boolean isUniverseCreated;
	private Image3DUniverse universe;
	private double objectLabel;
	private ImagePlus objectMask;

	public ObjectViewer3D()
	{

	}

	public void showSelectedObjectIn3D( Bdv bdv, RealPoint coordinate, double voxelSize )
	{
		createUniverse();

		objectMask = extractObject( bdv, coordinate, voxelSize );

		createMeshAndDisplay( objectMask );
	}

	public void createMeshAndDisplay( ImagePlus objectMask )
	{
		(new Thread(new Runnable()
		{
			public void run()
			{
				while ( !isUniverseCreated )
				{
					Utils.wait( 100 );
				}

				//univ.addUniverseListener( new UniverseListener( bdvObjectExtractor ) );

				long start = System.currentTimeMillis();
				final Content content = universe.addMesh( objectMask, new Color3f( 1.0f, 1.0f, 1.0f ), "object", 250, new boolean[]{ true, true, true }, 1 );
				Utils.log( "Computed mesh and created 3D display in [ms]: " + (System.currentTimeMillis() - start) );
			}
		})).start();
	}

	public ImagePlus extractObject( Bdv bdv, RealPoint coordinate, double voxelSize )
	{
		final BdvObjectExtractor bdvObjectExtractor = new BdvObjectExtractor( bdv, coordinate, 0 );

		final ArrayList< double[] > calibrations = bdvObjectExtractor.getCalibrations();

		int level;

		for ( level = 0; level < calibrations.size(); level++ )
		{
			if ( calibrations.get( level )[ 0 ] > voxelSize ) break;
		}

		final int selectedLevel = level;

		final ImagePlus objectMask = Utils.asImagePlus(
				bdvObjectExtractor.extractObjectMask( selectedLevel ),
				calibrations.get( selectedLevel ) ).duplicate(); // duplicate ImagePlus into RAM

		objectLabel = bdvObjectExtractor.getObjectLabel();

		final long executionTimeMillis = bdvObjectExtractor.getExecutionTimeMillis( );
		Utils.log( "Extracted object at resolution [um] " + calibrations.get( level )[ 0 ]
				+ " in " + executionTimeMillis + " ms" );
		return objectMask;
	}

	public void createUniverse()
	{
		isUniverseCreated = false;

		(new Thread(new Runnable()
		{
			public void run()
			{
				final long start = System.currentTimeMillis();
				universe = new Image3DUniverse();
				universe.show();
				isUniverseCreated = true;
				Utils.log( "Universe created in [ms]: " + (System.currentTimeMillis() - start) );
			}
		})).start();
	}

	public static class UniverseListener implements ij3d.UniverseListener
	{
		final BdvObjectExtractor bdvObjectExtractor;

		public UniverseListener( BdvObjectExtractor bdvObjectExtractor )
		{

			this.bdvObjectExtractor = bdvObjectExtractor;
		}

		@Override
		public void transformationStarted( View view )
		{

		}

		@Override
		public void transformationUpdated( View view )
		{

		}

		@Override
		public void transformationFinished( View view )
		{

		}

		@Override
		public void contentAdded( Content c )
		{

		}

		@Override
		public void contentRemoved( Content c )
		{

		}

		@Override
		public void contentChanged( Content c )
		{

		}

		@Override
		public void contentSelected( Content c )
		{

		}

		@Override
		public void canvasResized()
		{

		}

		@Override
		public void universeClosed()
		{
			System.out.println( "Universe closed!" ); // TODO stop loading higher levels it that happens
			bdvObjectExtractor.stop();
		}
	}
}
