package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.annotation.Annotation;

public abstract class AbstractTableSawAnnotation implements Annotation
{
	protected final TableSawAnnotationTableModel< ? > model;
	protected final int rowIndex;

	public AbstractTableSawAnnotation( final TableSawAnnotationTableModel< ? > model, int rowIndex )
	{
		// in principle only the model.getTable() is needed
		// however, the table object within the model may change,
		// e.g. due to the merging of new columns
		// thus, the model is referenced here and the table is retrieved
		// from it on demand
		this.model = model;
		this.rowIndex = rowIndex;
	}

	@Override
	public Object getValue( String feature )
	{
		try
		{
			final Object object = model.getTable().get( rowIndex, model.getTable().columnIndex( feature ) );
			return object;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	@Override
	public Double getNumber( String feature )
	{
		return model.getTable().numberColumn( feature ).getDouble( rowIndex );
	}

	@Override
	public void setString( String columnName, String value )
	{
		model.getTable().stringColumn( columnName ).set( rowIndex, value );
	}

}
