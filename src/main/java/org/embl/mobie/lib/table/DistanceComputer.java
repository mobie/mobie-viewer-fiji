package org.embl.mobie.lib.table;

import ij.gui.GenericDialog;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.select.SelectionModel;

import java.util.Set;

public class DistanceComputer
{
    public static < A extends Annotation > void showUI( AnnotationTableModel< A > tableModel, SelectionModel< A > selectionModel )
    {
        // show dialog
        //
        final GenericDialog gd = new GenericDialog( "" );
        gd.addStringField( "Distance Columns RegEx", ".*" );
        gd.addStringField( "Results Column Name", "distance" );
        gd.showDialog();
        if( gd.wasCanceled() ) return;
        final String columnsRegEx = gd.getNextChoice();
        final String resultColumnName = gd.getNextString();

        // TODO
//        tableModel.addNumericColumn( resultColumnName );
//
//        selectedColumns = tableModel.columnNames().stream() // TODO: select columns that match columnsRegEx
//
//        // for all selected selectedColumns compute the average or median value
//        // of all selectedAnnotations
//        Set< A > selectedAnnotations = selectionModel.getSelected();
//        for ( A annotation : selectedAnnotations )
//        {
//            annotation.getNumber( column ) // TODO
//        }
//
//        // for all annotations compute the Euclidean distance
//        // to the above computed average of the selected annotations
//
//        tableModel.annotations()
//        tableModel.annotation( index ).setString(  );
    }
}
