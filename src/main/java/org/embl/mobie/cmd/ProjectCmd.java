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
package org.embl.mobie.cmd;

import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mobie-project", mixinStandardHelpOptions = true, version = "4.0.3", description = "Visualise multi-modal big image data stored as a MoBIE project, see https://mobie.github.io/")
public class ProjectCmd implements Callable< Void > {

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Option(names = {"-p", "--project"}, required = false, description = "MoBIE project, e.g., \"https://github.com/mobie/platybrowser-datasets\"")
	public String project = null;

	@Option(names = {"-v", "--view"}, required = false, description = "view within the above MoBIE project, e.g., \"Figure 2C: Muscle segmentation\"")
	public String view = null;

	@Override
	public Void call() throws Exception {

		final MoBIESettings settings = new MoBIESettings().openedFromCLI( true );

		if ( view != null ) settings.view( view );

		new MoBIE( project, settings );

		return null;
	}

	public static final void main( final String... args ) {

		final ProjectCmd projectCmd = new ProjectCmd();

		System.out.println( Arrays.toString( args ) );

		if ( args == null || args.length == 0 )
			new CommandLine( projectCmd ).execute( "--help" );
		else
			new CommandLine( projectCmd ).execute( args );
	}
}
