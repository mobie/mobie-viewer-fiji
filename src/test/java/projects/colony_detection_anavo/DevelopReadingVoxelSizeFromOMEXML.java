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
package projects.colony_detection_anavo;

import loci.common.DebugTools;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;

public class DevelopReadingVoxelSizeFromOMEXML
{
    public static void main( String[] args )
    {
        DebugTools.setRootLevel( "OFF" ); // Disable Bio-Formats logging

        String filePath = "/Users/tischer/Desktop/moritz/MeasurementResult.ome.xml";

        System.out.println( FormatTools.VERSION );

        try {
            ImageProcessorReader reader = new ImageProcessorReader();
            IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
            reader.setMetadataStore(omeMeta);
            reader.setId(filePath);
            IMetadata meta = (IMetadata) reader.getMetadataStore();

            double physicalSizeX = meta.getPixelsPhysicalSizeX(1).value().doubleValue();
            double physicalSizeY = meta.getPixelsPhysicalSizeY(1).value().doubleValue();

            System.out.println("Pixel Size (X): " + physicalSizeX + " µm");
            System.out.println("Pixel Size (Y): " + physicalSizeY + " µm");

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
