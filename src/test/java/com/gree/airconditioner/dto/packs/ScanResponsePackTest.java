package com.gree.airconditioner.dto.packs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ScanResponsePackTest {
    @Test
    public void acceptsAdditionalFirmwareMetadata() throws Exception {
        ScanResponsePack pack = new ObjectMapper().readValue(
            "{\"t\":\"dev\",\"mac\":\"001122334455\",\"ver\":\"V3.4.M\",\"ModelType\":\"2168684544\",\"hid\":\"firmware.bin\"}",
            ScanResponsePack.class);
        assertEquals("001122334455", pack.getMac());
        assertEquals("V3.4.M", pack.getVer());
    }
}
