package com.gree.airconditioner.controllers;

import com.gree.airconditioner.GreeAirconditionerDevice;
import com.gree.airconditioner.dto.status.GreeDeviceStatus;
import com.gree.airconditioner.dto.status.OperationMode;
import com.gree.airconditioner.dto.status.FanMode;
import com.gree.airconditioner.services.GreeAirconditionerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GreeAirconditionerController {

    @Autowired
    private GreeAirconditionerService airconditionerService;

    @GetMapping("/powerOn")
    public String powerOn() {
        List<GreeAirconditionerDevice> devices = this.airconditionerService.getDevices();
        this.airconditionerService.turnOn(devices.get(0));
        return "done";
    }

    @GetMapping("/temperature")
    public String temperature(@RequestParam() Integer temperature) {
        List<GreeAirconditionerDevice> devices = this.airconditionerService.getDevices();
        this.airconditionerService.setTemperature(devices.get(0), temperature);
        return "done";
    }

    @GetMapping("/status")
    public GreeDeviceStatus getStatus() {
        List<GreeAirconditionerDevice> devices = this.airconditionerService.getDevices();
        return this.airconditionerService.getStatus(devices.get(0));
    }

    @PostMapping("/settings")
    public String settings(@RequestParam(required = false) OperationMode mode,
                           @RequestParam(required = false) FanMode fanSpeed,
                           @RequestParam(required = false) Integer swing) {
        if (mode == null && fanSpeed == null && swing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a setting to change");
        }
        if (swing != null && (swing < 0 || swing > 11)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Swing must be between 0 and 11");
        }
        GreeAirconditionerDevice device = airconditionerService.getDevices().get(0);
        airconditionerService.setSettings(device, mode, fanSpeed, swing);
        return "done";
    }

    @GetMapping("/powerOff")
    public String powerOff() {
        List<GreeAirconditionerDevice> devices = this.airconditionerService.getDevices();
        this.airconditionerService.turnOff(devices.get(0));
        return "done";
    }
}
