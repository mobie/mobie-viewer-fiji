package de.embl.cba.mobie2;

import javax.xml.transform.Source;
import java.util.List;

public interface SourceTransformer
{
	List< Source > transform( List< Source > sources );
}
