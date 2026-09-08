package com.gree.airconditioner.dto.packs;

import com.gree.airconditioner.dto.status.GreeDeviceStatus;
import com.gree.airconditioner.dto.status.Switch;
import com.gree.airconditioner.dto.status.OperationMode;
import com.gree.airconditioner.dto.status.FanMode;
import com.gree.airconditioner.dto.status.SwingDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControlRequestPackTest {
    @Test
    public void testSettingsWireCodesWithoutPowerOrTemperature() {
        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setOperationMode(OperationMode.COOL);
        status.setFanMode(FanMode.HIGH);
        status.setSwingDirection(new SwingDirection(4));
        assertEquals("{\"t\":\"cmd\",\"opt\":[\"Mod\",\"WdSpd\",\"SwUpDn\"],\"p\":[1,5,4]}", new ControlRequestPack(status).toJson());
    }

    @Test
    public void testPowerCommand() throws Exception {
        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setPower(Switch.ON);

        ControlRequestPack pack = new ControlRequestPack(status);
        assertEquals("{\"t\":\"cmd\",\"opt\":[\"Pow\"],\"p\":[1]}", pack.toJson());
    }
}
