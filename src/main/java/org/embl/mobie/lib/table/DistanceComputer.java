package org.embl.mobie.lib.table;

import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.color.NumericAnnotationColoringModel;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.select.SelectionModel;
import org.jetbrains.annotations.NotNull;
import org.scijava.util.ColorRGB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DistanceComputer
{
    public static < A extends Annotation > void showUI( AnnotationTableModel< A > tableModel, SelectionModel< A > selectionModel, MobieColoringModel< A > coloringModel )
    {
        // show dialog
        //
        final GenericDialog gd = new GenericDialog( "" );
        gd.addStringField( "Distance Columns RegEx", "anchor_.*" );
        gd.addStringField( "Results Column Name", "distance" );
        gd.addCheckbox( "Color by Distances", true );
        gd.showDialog();
        if( gd.wasCanceled() ) return;
        final String columnNamesRegEx = gd.getNextString();
        final String resultColumnName = gd.getNextString();
        boolean colorByDistances = gd.getNextBoolean();

        List< String > selectedColumnNames = tableModel.columnNames().stream()
                .filter( columnName -> columnName.matches( columnNamesRegEx ) )
                .collect( Collectors.toList() );

        if ( selectedColumnNames.isEmpty() )
        {
            IJ.log( "The regular expression " + columnNamesRegEx + " did not match any column names." );
            return;
        }

        // for all selected selectedColumns compute the average or median value
        // of all selectedAnnotations and store this in a Map< ColumnName, double >
        Set< A > selectedAnnotations = selectionModel.getSelected();

        // TODO provide options for other averaging methods, e.g. median
        Map< String, Double > columnAverages = averageSelectedAnnotations( selectedColumnNames, selectedAnnotations );

        // for all annotations compute the Euclidean distance
        // to the above computed average of the selected annotations
        tableModel.addNumericColumn( resultColumnName );

        long start = System.currentTimeMillis();
        computeEuclidanDistances( tableModel, selectedColumnNames, columnAverages, resultColumnName );
        String distanceMetric = "Euclidan";
        IJ.log( "Computed the " + distanceMetric + " distance of " + selectedColumnNames.size()
                + " features for " + tableModel.annotations().size() + " annotations in " +
                ( System.currentTimeMillis() - start ) + " ms.");

        if ( colorByDistances )
        {
            NumericAnnotationColoringModel< A > numericModel =
                    ColoringModels.createNumericModel(
                            resultColumnName,
                            LUTs.BLUE_WHITE_RED,
                            tableModel.getMinMax( resultColumnName ),
                            true );
            coloringModel.setColoringModel( numericModel );
            coloringModel.setOpacityNotSelected( 1.0 );
            coloringModel.setSelectionColor( new ARGBType( ARGBType.rgba( 255, 255, 0, 255 ) ) );
        }

    }

    @NotNull
    private static < A extends Annotation > Map< String, Double > averageSelectedAnnotations( List< String > selectedColumnNames, Set< A > selectedAnnotations )
    {
        Map<String, Double> columnAverages = new HashMap<>();
        for (String column : selectedColumnNames ) {
            double sum = selectedAnnotations.stream()
                    .mapToDouble(annotation -> annotation.getNumber(column).doubleValue())
                    .sum();
            double average = sum / selectedAnnotations.size();
            columnAverages.put(column, average);
        }
        return columnAverages;
    }

    @NotNull
    private static < A extends Annotation > Map< String, Double > medianSelectedAnnotations( List< String > selectedColumnNames, Set< A > selectedAnnotations )
    {
        Map<String, Double> columnMedians = new HashMap<>();
        for (String column : selectedColumnNames) {
            List<Double> values = selectedAnnotations.stream()
                    .map(annotation -> annotation.getNumber(column).doubleValue())
                    .sorted()
                    .collect(Collectors.toList());
            double median;
            int size = values.size();
            if (size % 2 == 0) {
                median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
            } else {
                median = values.get(size / 2);
            }
            columnMedians.put(column, median);
        }
        return columnMedians;
    }

    private static < A extends Annotation > void computeEuclidanDistances( AnnotationTableModel< A > tableModel, List< String > selectedColumnNames, Map< String, Double > columnAverages, String resultColumnName )
    {
        // Compute Euclidean distances
        List< A > annotations = tableModel.annotations();

        for ( A annotation : annotations )
        {
            double sumOfSquares = 0.0;
            for ( String column : selectedColumnNames ) {
                final double value = annotation.getNumber(column).doubleValue();
                final double average = columnAverages.get(column);
                sumOfSquares += Math.pow(value - average, 2);
            }
            final double euclideanDistance = Math.sqrt( sumOfSquares );
            annotation.setNumber( resultColumnName, euclideanDistance);
        }

    }
}
