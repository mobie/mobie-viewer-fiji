package mobie3.viewer.ui;

import ij.gui.GenericDialog;
import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.color.LUTs;
import mobie3.viewer.color.ColoringModel;
import mobie3.viewer.color.AnnotationColoringModelCreator;
import mobie3.viewer.table.AnnData;
import net.imglib2.util.Pair;

import java.util.List;

import static mobie3.viewer.color.LUTs.TRANSPARENT;
import static mobie3.viewer.color.LUTs.COLORING_LUTS;

public class ColumnColoringModelDialog< A extends Annotation>
{
	private static String lut;
	private static String columnName;
	private static boolean paintZeroTransparent;
	private final AnnData< A > annData;

	public ColumnColoringModelDialog( AnnData< A > annData )
	{
		this.annData = annData;
	}

	public ColoringModel< A > showDialog( )
	{
		final List< String > columnNames = annData.getTable().columnNames();
		final String[] columnNameArray = columnNames.toArray( new String[ 0 ] );
		final GenericDialog gd = new GenericDialog( "Color by Column" );
		if ( columnName == null || ! columnNames.contains( columnName ) ) columnName = columnNameArray[ 0 ];
		gd.addChoice( "Column", columnNameArray, columnName );

		if ( lut == null ) lut = COLORING_LUTS[ 0 ];
		gd.addChoice( "Coloring Mode", COLORING_LUTS, lut );

		gd.addCheckbox( "Paint Zero Transparent", paintZeroTransparent );

		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		columnName = gd.getNextChoice();
		lut = gd.getNextChoice();
		paintZeroTransparent = gd.getNextBoolean();

		if ( paintZeroTransparent )
			lut += LUTs.ZERO_TRANSPARENT;

		if ( LUTs.isNumeric( lut ) )
		{
			final Pair< Double, Double > minMax = annData.getTable().computeMinMax( columnName );
			return AnnotationColoringModelCreator.createNumericModel( columnName, paintZeroTransparent, minMax, LUTs.getLut( lut ) );
		}
		else if ( LUTs.isCategorical( lut ) )
		{
			return AnnotationColoringModelCreator
					.createCategoricalModel( columnName, LUTs.getLut( lut ), paintZeroTransparent, TRANSPARENT );
		}
		else
		{
			throw new UnsupportedOperationException( "LUT " + lut + " is not supported." );
		}
	}
}
