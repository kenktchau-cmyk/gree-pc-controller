package com.gree.airconditioner.dto;

import com.gree.airconditioner.GreeAirconditionerDevice;
import com.gree.airconditioner.controllers.GreeAirconditionerController;
import com.gree.airconditioner.services.GreeAirconditionerService;
import com.gree.airconditioner.dto.status.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SettingsControllerTest {
    private MockMvc mvc;
    private GreeAirconditionerService service;
    private GreeAirconditionerDevice device;
    @Before public void setup() {
        service = mock(GreeAirconditionerService.class);
        device = mock(GreeAirconditionerDevice.class);
        when(service.getDevices()).thenReturn(Collections.singletonList(device));
        GreeAirconditionerController controller = new GreeAirconditionerController();
        ReflectionTestUtils.setField(controller, "airconditionerService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    @Test public void acceptsPartialUpdateWithoutResettingOtherSettings() throws Exception {
        mvc.perform(post("/settings").param("mode","COOL"))
            .andExpect(status().isOk()).andExpect(content().string("done"));
        verify(service).setSettings(device, OperationMode.COOL, null, null);
    }
    @Test public void acceptsFanAndSwing() throws Exception {
        mvc.perform(post("/settings").param("fanSpeed","HIGH").param("swing","4"))
            .andExpect(status().isOk());
        verify(service).setSettings(device, null, FanMode.HIGH, 4);
    }
    @Test public void rejectsInvalidSettingsBeforeContactingDevice() throws Exception {
        mvc.perform(post("/settings")).andExpect(status().isBadRequest());
        mvc.perform(post("/settings").param("swing","12")).andExpect(status().isBadRequest());
        mvc.perform(post("/settings").param("swing","-1")).andExpect(status().isBadRequest());
        mvc.perform(post("/settings").param("mode","UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(post("/settings").param("fanSpeed","MAX")).andExpect(status().isBadRequest());
        verifyZeroInteractions(service);
    }
}
