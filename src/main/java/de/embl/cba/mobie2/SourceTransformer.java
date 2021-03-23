package de.embl.cba.mobie2;

import javax.xml.transform.Source;
import java.util.List;

public interface SourceTransformer
{
	/**
	 * @param sources
	 * 			a list of input sources
	 * @return
	 * 			a transformed list of sources
	 * 			(a subset of the input sources)
	 */
	List< Source > transform( List< Source > sources );
}
