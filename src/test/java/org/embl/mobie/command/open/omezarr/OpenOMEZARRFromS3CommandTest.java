package org.embl.mobie.command.open.omezarr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenOMEZARRFromS3CommandTest
{
    //@Test
    public void test( )
    {
        OpenOMEZARRFromS3Command command = new OpenOMEZARRFromS3Command();
        command.s3URL = "https://s3.embl.de/i2k-2020/platy-raw.ome.zarr";
        command.run();
    }
}