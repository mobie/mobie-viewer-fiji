package de.embl.cba.platynereis.objects;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.objects.BdvObjectExtractor;
import de.embl.cba.platynereis.utils.Utils;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import java.util.ArrayList;

import static de.embl.cba.platynereis.utils.Utils.log;
import static de.embl.cba.platynereis.utils.Utils.wait100ms;

public class ObjectViewer3D
{
	public ObjectViewer3D()
	{
	}

	public static void showSelectedObjectIn3D( Bdv bdv, RealPoint coordinate, double voxelSize )
	{

		final BdvObjectExtractor bdvObjectExtractor = new BdvObjectExtractor( bdv, coordinate, 0 );

		final ArrayList< double[] > calibrations = bdvObjectExtractor.getCalibrations();

		int level;

		for ( level = 0; level < calibrations.size(); level++ )
		{
			if ( calibrations.get( level )[ 0 ] > voxelSize ) break;
		}

		final ImagePlus objectMask = Utils.asImagePlus(
				bdvObjectExtractor.extractObjectMask( level ),
				calibrations.get( level ) ).duplicate(); // duplicate ImagePlus to copy into RAM

		final long executionTimeMillis = bdvObjectExtractor.getExecutionTimeMillis( );
		Utils.log( "Extracted object at resolution " + calibrations.get( level )[ 0 ]
				+ " in " + executionTimeMillis + " ms" );

		//objectMask.show();

		final int ll = level;

		(new Thread(new Runnable()
		{
			public void run()
			{
				final long start = System.currentTimeMillis();
				final Image3DUniverse univ = new Image3DUniverse();
				univ.show();
				//univ.addUniverseListener( new UniverseListener( bdvObjectExtractor ) );
				final Content content = univ.addMesh( objectMask, new Color3f( 1.0f, 1.0f, 1.0f ), "object at level " + ll, 250, new boolean[]{ true, true, true }, 1 );
				Utils.log( "Computed mesh and created 3D display in " + (System.currentTimeMillis() - start) + " ms" );
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
