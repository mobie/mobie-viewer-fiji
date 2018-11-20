import bdv.tools.transformation.TransformedSource;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.labels.luts.ARGBConvertedUnsignedLongTypeLabelsSource;
import de.embl.cba.bdv.utils.labels.luts.LabelsSource;
import de.embl.cba.bdv.utils.transformhandlers.BehaviourTransformEventHandler3DGoogleMouse;
import ij.IJ;
import jdk.nashorn.internal.ir.Labels;
import loci.poi.hssf.record.LabelSSTRecord;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.volatiles.VolatileARGBType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.io.File;
import java.util.Map;

public class TestSpimDataUsignedLongLabelsLoading
{

	public static void main( String[] args ) throws SpimDataException
	{

		// Loader class auto-discovery happens here:
		// https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/generic/sequence/ImgLoaders.java#L53

		//final File file = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-segmented-cells-parapodium-labels-test.xml" );

		final File file = new File( "/Users/tischer/Desktop/bdv_test_data/test.xml" );

		SpimData spimData = new XmlIoSpimData().load( file.toString() );

		final Source< VolatileARGBType > labelSource = new ARGBConvertedUnsignedLongTypeLabelsSource( spimData, 0 );

		final BdvStackSource< VolatileARGBType > bdvStackSource =
				BdvFunctions.show( labelSource,
						BdvOptions.options().transformEventHandlerFactory( new BehaviourTransformEventHandler3DGoogleMouse.BehaviourTransformEventHandler3DFactory() ) );


		final Bdv bdv = bdvStackSource.getBdvHandle();

		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdv );
			final Map< Integer, Long > integerLongMap = BdvUtils.selectObjectsInActiveLabelSources( bdv, globalMouseCoordinates );
			for ( int sourceIndex : integerLongMap.keySet())
			{
				System.out.println( "Label " + integerLongMap.get( sourceIndex ) + " selected in source #" + sourceIndex );
			};
		}, "select object", "Q"  ) ;



		behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "behaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			BdvUtils.deselectAllObjectsInActiveLabelSources( bdv );
		}, "select none", "W" );


		final SourceAndConverter< VolatileARGBType > sourceAndConverter = bdvStackSource.getSources().get( 0 );

		final Source< VolatileARGBType > spimSource = sourceAndConverter.getSpimSource();

		if ( spimSource instanceof TransformedSource )
		{
			final Source wrappedSource = ( ( TransformedSource ) spimSource ).getWrappedSource();

			if ( wrappedSource instanceof LabelsSource )
			{
//				IJ.wait( 5000 );
//				((LabelsSource)wrappedSource).incrementSeed();
//				bdvStackSource.getBdvHandle().getViewerPanel().requestRepaint();
//				IJ.wait( 5000 );
//				((LabelsSource)wrappedSource).incrementSeed();
//				bdvStackSource.getBdvHandle().getViewerPanel().requestRepaint();

//				final RandomAccessibleInterval< IntegerType > indexImg = ( ( LabelsSource ) wrappedSource ).getIndexImg( 0, 0 );
//
//				final RandomAccess< IntegerType > access = indexImg.randomAccess();
//
//				final long integerLong = access.get().getIntegerLong();
//
//				int a = 1;

			}
		}
		else
		{
			int b = 2;
		}



	}
}
