/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.lib;

import bdv.tools.HelpDialog;
import de.embl.cba.tables.Help;
import ij.IJ;
import org.embl.mobie.io.util.IOHelper;

public class MoBIEInfo
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String MOBIE_VIEWER = "MoBIE Viewer";
	public static final String MOBIE_FRAMEWORK = "MoBIE Framework";
	public static final String BIG_DATA_VIEWER = "BigDataViewer";
	public static final String REPOSITORY = "Datasets Repository";
	public static final String PUBLICATION = "Datasets Publication";
	public static final String SEGMENTATIONS = "Segmentations Browsing";
	private final String projectLocation;
	private final String publicationURL;

	public MoBIEInfo( String projectLocation, String publicationURL )
	{
		this.projectLocation = projectLocation;
		this.publicationURL = publicationURL;
	}

	public String[] getInfoChoices()
	{
		return new String[]{
				REPOSITORY,
				PUBLICATION,
				MOBIE_VIEWER,
				MOBIE_FRAMEWORK,
				BIG_DATA_VIEWER,
				SEGMENTATIONS };
	}

	public void showInfo( String selectedItem )
	{
		switch ( selectedItem )
		{
			case MOBIE_VIEWER:
				IOHelper.openURI( "https://github.com/mobie/mobie-viewer-fiji/blob/master/README.md#mobie-fiji-viewer" );
				break;
			case MOBIE_FRAMEWORK:
				IOHelper.openURI( "https://github.com/mobie/mobie#mobie" );
				break;
			case REPOSITORY:
				IOHelper.openURI( IOHelper.combinePath( projectLocation, "blob/master/README.md" ) );
				break;
			case PUBLICATION:
				if ( publicationURL == null )
				{
					IJ.showMessage( "There is no publication registered with this project.");
					return;
				}
				else
				{
					IOHelper.openURI( publicationURL );
				}
				break;
			case BIG_DATA_VIEWER:
				showBdvHelp();
				break;
			case SEGMENTATIONS:
				Help.showSegmentationImageHelp();
		}
	}

	public void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, MoBIE.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
