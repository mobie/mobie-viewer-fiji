package develop;

import ij.gui.PlotWindow;
import ij.gui.ProfilePlot;
import ij.measure.ResultsTable;

public class ProfileTest
{
    public static void main( String[] args )
    {
        ProfilePlot profiler = new ProfilePlot();
        ResultsTable resultsTable = profiler.getPlot().getResultsTable();
    }
        
}
