package org.embl.mobie.lib.transform;

import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.AnnotationTableModel;
import org.embl.mobie.lib.table.TransformedAnnotationTableModel;

public class TransformedAnnData< A extends Annotation, TA extends Annotation > implements AnnData< TA >
{
	private final AnnData< A > annData;
	private final AnnotationTransformer< A, TA > annotationTransformer;
	private TransformedAnnotationTableModel tableModel;

	public TransformedAnnData( AnnData< A > annData, AnnotationTransformer< A, TA > annotationTransformer )
	{
		this.annData = annData;
		this.annotationTransformer = annotationTransformer;
	}

	@Override
	public AnnotationTableModel< TA > getTable()
	{
		if ( tableModel == null )
			tableModel = new TransformedAnnotationTableModel( annData.getTable(), annotationTransformer );

		return tableModel;
	}
}
