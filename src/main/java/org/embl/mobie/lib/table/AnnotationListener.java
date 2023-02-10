package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;

import java.util.Collection;

public interface AnnotationListener< A extends Annotation >
{
	void annotationsAdded( Collection< A > annotations );
	void columnAdded( String columnName );
}
