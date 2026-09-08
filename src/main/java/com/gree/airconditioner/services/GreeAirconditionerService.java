package com.gree.airconditioner.services;

import com.gree.airconditioner.GreeAirconditionerDevice;
import com.gree.airconditioner.binding.GreeDeviceBinderService;
import com.gree.airconditioner.binding.GreeDeviceBinding;
import com.gree.airconditioner.communication.GreeCommunicationService;
import com.gree.airconditioner.dto.Command;
import com.gree.airconditioner.dto.CommandBuilder;
import com.gree.airconditioner.dto.packs.StatusResponsePack;
import com.gree.airconditioner.dto.status.GreeDeviceStatus;
import com.gree.airconditioner.dto.status.Switch;
import com.gree.airconditioner.dto.status.Temperature;
import com.gree.airconditioner.dto.status.TemperatureUnit;
import com.gree.airconditioner.dto.status.OperationMode;
import com.gree.airconditioner.dto.status.FanMode;
import com.gree.airconditioner.dto.status.SwingDirection;
import com.gree.airconditioner.util.GreeGcm;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GreeAirconditionerService {
    private static final Logger log = LogManager.getLogger(GreeAirconditionerService.class);

    private final GreeDeviceBinderService binderService;
    private final GreeCommunicationService communicationService;
    private List<GreeAirconditionerDevice> devices;

    public GreeAirconditionerService(GreeDeviceBinderService binderService, GreeCommunicationService communicationService) {
        this.binderService = binderService;
        this.communicationService = communicationService;
    }

    public List<GreeAirconditionerDevice> getDevices() {
        return devices;
    }

    public void discoverDevices() {
        String address = System.getProperty("gree.address", "").trim();
        if (address.isEmpty()) throw new IllegalArgumentException("Set -Dgree.address to your air conditioner's local IP address");
        List<GreeAirconditionerDevice> devices = GreeAirconditionerDeviceFinder.findDevices(address);
        if (devices == null || devices.isEmpty()) {
            throw new IllegalStateException("No Gree device responded at " + address);
        }
        this.devices = devices;
    }

    public boolean turnOn(GreeAirconditionerDevice device) {
        log.info("Turning on the air conditioner");
        GreeDeviceBinding binding = binderService.getBiding(device);

        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setPower(Switch.ON);

        Command command = CommandBuilder.builder().buildControlCommand(status, binding);
        validateControlResponse(device, command, binding);
        return true;
    }

    public boolean turnOff(GreeAirconditionerDevice device) {
        log.info("Turning off the air conditioner");
        GreeDeviceBinding binding = binderService.getBiding(device);

        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setPower(Switch.OFF);

        Command command = CommandBuilder.builder().buildControlCommand(status, binding);
        validateControlResponse(device, command, binding);
        return true;
    }

    public boolean setTemperature(GreeAirconditionerDevice device, Integer temperature) {
        log.info("Setting the temperature to {}", temperature);
        GreeDeviceBinding binding = binderService.getBiding(device);

        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setTemperature(new Temperature(temperature, TemperatureUnit.CELSIUS));

        Command command = CommandBuilder.builder().buildControlCommand(status, binding);
        validateControlResponse(device, command, binding);
        return true;
    }

    public void setSettings(GreeAirconditionerDevice device, OperationMode mode, FanMode fanSpeed, Integer swing) {
        GreeDeviceBinding binding = binderService.getBiding(device);
        GreeDeviceStatus status = new GreeDeviceStatus();
        status.setOperationMode(mode);
        status.setFanMode(fanSpeed);
        if (swing != null) status.setSwingDirection(new SwingDirection(swing));
        Command command = CommandBuilder.builder().buildControlCommand(status, binding);
        validateControlResponse(device, command, binding);
    }

    private void validateControlResponse(GreeAirconditionerDevice device, Command command, GreeDeviceBinding binding) {
        communicationService.sendCommand(device, command, json -> {
            JsonNode response = GreeGcm.decode(json, binding.getAesKey());
            if (!"res".equals(response.path("t").asText()) || response.path("r").asInt() != 200) {
                throw new IllegalStateException("The air conditioner did not accept the command");
            }
            return true;
        });
    }

    public GreeDeviceStatus getStatus(GreeAirconditionerDevice device) {
        log.info("Getting status of device");
        GreeDeviceBinding binding = binderService.getBiding(device);

        Command command = CommandBuilder.builder().buildStatusCommand(binding);
        GreeDeviceStatus result = communicationService.sendCommand(devices.get(0), command, (json) -> StatusResponsePack.build(json, binding).toObject());
        return result;
    }
}
