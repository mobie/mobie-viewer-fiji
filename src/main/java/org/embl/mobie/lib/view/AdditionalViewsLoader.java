/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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
package org.embl.mobie.lib.view;

import ij.IJ;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.ui.UserInterfaceHelper;

import java.io.IOException;
import java.util.Map;

public class AdditionalViewsLoader {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private MoBIE moBIE;

    public AdditionalViewsLoader ( MoBIE moBIE ) {
        this.moBIE = moBIE;
    }

    public void loadAdditionalViewsDialog() {
        new Thread( () -> {
            try {
                String selectedFilePath = null;
                FileLocation fileLocation = UserInterfaceHelper.loadFromProjectOrFileSystemDialog();
                if ( fileLocation == null )
                    return;
                if ( fileLocation == FileLocation.Project ) {
                    selectedFilePath = UserInterfaceHelper.selectPathFromProject( moBIE.absolutePath("misc", "views" ), "View" );
                } else {
                    selectedFilePath = UserInterfaceHelper.selectFilePath( "json", "View", true );
                }

                if (selectedFilePath != null) {
                    loadViews( selectedFilePath );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void loadViews( String selectedFilePath ) throws IOException
    {
        Map< String, View > views = new AdditionalViewsJsonParser().getViews( selectedFilePath ).views;
        moBIE.getViews().putAll( views );
        moBIE.getUserInterface().addViews( views );
        IJ.log( "New views loaded from:\n" + selectedFilePath );
    }
}
