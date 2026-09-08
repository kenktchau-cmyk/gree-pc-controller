package com.gree.airconditioner.dto;

import com.gree.airconditioner.util.GreeGcm;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GreeGcmTest {
    private static final String JSON = "{\"t\":\"cmd\",\"opt\":[\"SetTem\",\"TemUn\"],\"p\":[25,0]}";
    private static final String PACK = "JtoKliwtqp23pNCVNTkBFZUaqrBHlnOjbUZ4H46Oy52fNFBDzxF8cLOn4qCUN5g=";
    private static final String TAG = "XJ6o974HRQbMs4QJ00Pdhg==";

    @Test public void matchesIndependentDotNetVector() {
        Command command = new Command();
        GreeGcm.encode(command, JSON, GreeGcm.GENERIC_KEY);
        assertEquals(PACK, command.getPack());
        assertEquals(TAG, command.getTag());
        assertEquals(JSON, GreeGcm.decode(command.toJson(), GreeGcm.GENERIC_KEY).toString());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsTamperedTag() {
        GreeGcm.decode("{\"pack\":\"" + PACK + "\",\"tag\":\"AAAAAAAAAAAAAAAAAAAAAA==\"}", GreeGcm.GENERIC_KEY);
    }
}
