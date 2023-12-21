package org.embl.mobie.lib.table;

import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.MobieColoringModel;
import org.embl.mobie.lib.color.NumericAnnotationColoringModel;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.select.SelectionModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DistanceComputer
{
    enum Average
    {
        Mean,
        Median;

        public static String[] asArray()
        {
            return Arrays.stream(Average.values()).map(Enum::name).toArray(String[]::new);
        }

    }

    enum Metric {
        Euclidian,
        Cosine;

        public static String[] asArray()
        {
            return Arrays.stream(Metric.values()).map(Enum::name).toArray(String[]::new);
        }
    }

    public static < A extends Annotation > void showUI( AnnotationTableModel< A > tableModel, SelectionModel< A > selectionModel, MobieColoringModel< A > coloringModel )
    {
        Set< A > referenceRows = selectionModel.getSelected();
        if ( referenceRows.isEmpty() )
        {
            IJ.showMessage( "Please select at least one table row as a reference." );
            return;
        }

        // show dialog
        //
        final GenericDialog gd = new GenericDialog( "" );
        gd.addStringField( "Distance Columns RegEx", "anchor_.*" );
        gd.addChoice( "Distance Metric", Metric.asArray(), Metric.Euclidian.toString() );
        gd.addChoice( "Averaging Method", Average.asArray(), Average.Median.toString() );
        gd.addStringField( "Results Column Name", "distance" );
        gd.addCheckbox( "Color by Results", true );
        gd.showDialog();
        if( gd.wasCanceled() ) return;

        // parse user input
        //
        final String columnNamesRegEx = gd.getNextString();
        Metric metric = Metric.valueOf( gd.getNextChoice() );
        Average average = Average.valueOf( gd.getNextChoice() );
        final String resultColumnName = gd.getNextString();
        boolean colorByDistances = gd.getNextBoolean();

        List< String > distanceFeatures = tableModel.columnNames().stream()
                .filter( columnName -> columnName.matches( columnNamesRegEx ) )
                .collect( Collectors.toList() );
        if ( distanceFeatures.isEmpty() )
        {
            IJ.showMessage( "The regular expression " + columnNamesRegEx + " did not match any column names." );
            return;
        }

        // compute
        //

        Map< String, Double > referenceValues = computeReferenceValues( average, distanceFeatures, referenceRows );

        computeDistancesAndAddToTable( tableModel, resultColumnName, metric, distanceFeatures, referenceValues );

        // visualise
        //
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

    // TODO: multi-thread the computations if they become too slow
    private static < A extends Annotation > void computeDistancesAndAddToTable( AnnotationTableModel< A > tableModel, String resultColumnName, Metric metric, List< String > selectedColumnNames, Map< String, Double > columnAverages )
    {
        if ( tableModel.columnNames().contains( resultColumnName ) )
        {
            IJ.log( "Overwriting values in existing column: " + resultColumnName );
        }
        else
        {
            IJ.log( "Adding new column: " + resultColumnName );
            tableModel.addNumericColumn( resultColumnName );
        }

        long start = System.currentTimeMillis();
        switch ( metric )
        {
            case Euclidian:
                computeEuclidanDistances( tableModel, selectedColumnNames, columnAverages, resultColumnName );
                break;
            case Cosine:
            default:
                computeCosineDistances( tableModel, selectedColumnNames, columnAverages, resultColumnName );
                break;
        }
        IJ.log( "Computed the " + metric + " distance of " + selectedColumnNames.size()
                + " features for " + tableModel.annotations().size() + " annotations in " +
                ( System.currentTimeMillis() - start ) + " ms.");
    }

    @NotNull
    private static < A extends Annotation > Map< String, Double > computeReferenceValues( Average average, List< String > selectedColumnNames, Set< A > selectedAnnotations )
    {
        switch ( average )
        {
            case Mean:
                return averageSelectedAnnotations( selectedColumnNames, selectedAnnotations );
            case Median:
            default:
               return medianSelectedAnnotations( selectedColumnNames, selectedAnnotations );
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

    private static < A extends Annotation > void computeEuclidanDistances( AnnotationTableModel< A > tableModel, List< String > selectedColumnNames, Map< String, Double > originCoordinates, String resultColumnName )
    {
        // Compute Euclidean distances
        List< A > annotations = tableModel.annotations();

        annotations.parallelStream().forEach(annotation -> {
            double sumOfSquares = 0.0;
            for (String column : selectedColumnNames) {
                final double value = annotation.getNumber(column).doubleValue();
                final double origin = originCoordinates.get(column);
                sumOfSquares += Math.pow(value - origin, 2);
            }
            final double euclideanDistance = Math.sqrt(sumOfSquares);
            annotation.setNumber(resultColumnName, euclideanDistance);
        });

    }

    private static <A extends Annotation> void computeCosineDistances(AnnotationTableModel<A> tableModel, List<String>
            selectedColumnNames, Map<String, Double> originCoordinates, String resultColumnName)
    {
        List< A > annotations = tableModel.annotations();

        annotations.parallelStream().forEach( annotation ->
        {
            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;

            for ( String column : selectedColumnNames )
            {
                final double value = annotation.getNumber( column ).doubleValue();
                final double origin = originCoordinates.get( column );

                dotProduct += value * origin;
                normA += Math.pow( value, 2 );
                normB += Math.pow( origin, 2 );
            }

            normA = Math.sqrt( normA );
            normB = Math.sqrt( normB );

            final double cosineSimilarity = dotProduct / ( normA * normB );
            // Cosine distance is defined as 1 - cosine similarity
            final double cosineDistance = 1 - cosineSimilarity;

            annotation.setNumber( resultColumnName, cosineDistance );
        } );
    }
}
