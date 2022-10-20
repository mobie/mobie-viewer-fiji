package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;

import java.util.Collection;

public interface AnnotationListener< A extends Annotation >
{
	void addAnnotations( Collection< A > annotations );
	void addAnnotation( A annotation );
}
