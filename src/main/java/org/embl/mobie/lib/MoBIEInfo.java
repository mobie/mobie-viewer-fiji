/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.Project;

public class MoBIEInfo
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String MOBIE_PUBLICATION = "MoBIE Publication";
	public static final String MOBIE_GITHUB = "MoBIE Source Code";
	public static final String MOBIE_DOCUMENTATION = "MoBIE Documentation";
	public static final String BIG_DATA_VIEWER = "BigDataViewer Help";
	public static final String PROJECT_REPOSITORY = "Project Repository";
	public static final String PROJECT_REFERENCES = "Project References";
	private final String projectLocation;
	private final Project project;

	public MoBIEInfo( String projectLocation, Project project )
	{
		this.projectLocation = projectLocation;
		this.project = project;
	}

	public String[] getInfoChoices()
	{
		return new String[]{
				PROJECT_REPOSITORY,
				PROJECT_REFERENCES,
				MOBIE_PUBLICATION,
				MOBIE_GITHUB,
				MOBIE_DOCUMENTATION,
				BIG_DATA_VIEWER};
	}

	public void showInfo( String selectedItem )
	{
		switch ( selectedItem )
		{
			case MOBIE_GITHUB:
				IOHelper.openURI( "https://github.com/mobie/" );
				break;
			case MOBIE_DOCUMENTATION:
				IOHelper.openURI( "https://mobie.github.io/" );
				break;
			case MOBIE_PUBLICATION:
				IOHelper.openURI( "https://www.nature.com/articles/s41592-023-01776-4" );
				break;
			case PROJECT_REPOSITORY:
				IOHelper.openURI( IOHelper.combinePath( projectLocation, "blob/master/README.md" ) );
				break;
			case PROJECT_REFERENCES:
				if ( project.getReferences() == null )
					IJ.showMessage( "There are no references for this project.");
				else
				{
					for ( String reference : project.getReferences() )
					{
						IOHelper.openURI( reference );
					}
				}
				break;
			case BIG_DATA_VIEWER:
				showBdvHelp();
				break;
		}
	}

	public void showBdvHelp()
	{
		HelpDialog helpDialog = new HelpDialog( null, MoBIE.class.getResource( "/BdvHelp.html" ) );
		helpDialog.setVisible( true );
	}
}
