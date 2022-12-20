package develop;

import ij.measure.ResultsTable;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class DevelopTableFromResultsTable
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/mobie/";
		final ResultsTable resultsTable = ResultsTable.open2( root + "src/test/resources/golgi-cell-features.csv" );
		final String[] columnNames = resultsTable.getHeadings();

		final Table table = Table.create( resultsTable.getTitle() );
		for ( String columnName : columnNames )
		{
			final DoubleColumn doubleColumn = DoubleColumn.create( columnName, resultsTable.getColumn( columnName ) );
			table.addColumns( doubleColumn );
		}
	}
}
