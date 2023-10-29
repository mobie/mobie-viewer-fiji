/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
