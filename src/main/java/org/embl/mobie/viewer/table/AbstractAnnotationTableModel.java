package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.select.Listeners;

public abstract class AbstractAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	protected final Listeners.SynchronizedList< AnnotationListener< A > > listeners = new Listeners.SynchronizedList<>();
}
