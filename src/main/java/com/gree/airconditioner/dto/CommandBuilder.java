package com.gree.airconditioner.dto;

import com.gree.airconditioner.DeviceInfo;
import com.gree.airconditioner.binding.GreeDeviceBinding;
import com.gree.airconditioner.dto.packs.BindRequestPack;
import com.gree.airconditioner.dto.packs.ControlRequestPack;
import com.gree.airconditioner.dto.packs.StatusRequestPack;
import com.gree.airconditioner.dto.status.GreeDeviceStatus;
import com.gree.airconditioner.util.GreeGcm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandBuilder {
    private static final Logger log = LogManager.getLogger(CommandBuilder.class);

    public static CommandBuilder builder() {
        return new CommandBuilder();
    }

    public Command buildScanCommand() {
        Command command = new Command();
        command.setCommandType(CommandType.SCAN);
        return command;
    }

    public Command buildBindCommand(DeviceInfo info) {
        Command command = new Command();
        command.setCommandType(CommandType.PACK);
        command.setCid("app");
        command.setI(1);
        command.setTcid(info.getMacAddress());
        command.setUid(0l);

        GreeGcm.encodeObject(command, new BindRequestPack(info.getMacAddress()), GreeGcm.GENERIC_KEY);

        return command;
    }

    public Command buildControlCommand(GreeDeviceStatus status, GreeDeviceBinding binding) {
        DeviceInfo info = binding.getDevice().getDeviceInfo();

        Command command = new Command();
        command.setCommandType(CommandType.PACK);
        command.setCid("app");
        command.setI(0);
        command.setTcid(info.getMacAddress());
        command.setUid(0l);

        GreeGcm.encode(command, new ControlRequestPack(status).toJson(), binding.getAesKey());

        return command;
    }

    public Command buildStatusCommand(GreeDeviceBinding binding) {
        DeviceInfo info = binding.getDevice().getDeviceInfo();

        Command command = new Command();
        command.setCommandType(CommandType.PACK);
        command.setCid("app");
        command.setI(0);
        command.setTcid(info.getMacAddress());
        command.setUid(0l);

        GreeGcm.encodeObject(command, new StatusRequestPack(info.getMacAddress()), binding.getAesKey());

        return command;
    }
}
