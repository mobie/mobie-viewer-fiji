package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.select.Listeners;

public abstract class AbstractAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	protected final Listeners.SynchronizedList< AnnotationListener< A > > listeners = new Listeners.SynchronizedList<>();
}
