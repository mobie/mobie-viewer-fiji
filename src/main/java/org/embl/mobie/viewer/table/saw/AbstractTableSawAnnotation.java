package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.Annotation;

public abstract class AbstractTableSawAnnotation implements Annotation
{
	protected final TableSawAnnotationTableModel< ? > model;
	protected final int rowIndex;

	public AbstractTableSawAnnotation( final TableSawAnnotationTableModel< ? > model, int rowIndex )
	{
		this.model = model;
		this.rowIndex = rowIndex;
	}

	@Override
	public Object getValue( String feature )
	{
		try
		{
			return model.getTable().get( rowIndex, model.getTable().columnIndex( feature ) );
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
