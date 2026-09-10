# Gree PC Controller

[English](README.md) | [繁體中文](README.zh-TW.md)

A local browser control panel for a compatible Gree Wi-Fi air conditioner, with Windows launchers and a Java REST backend.

## Controls

- Power on and off.
- Set temperature from 16–30°C, with an explicit Apply button.
- Change operating mode, fan speed, and vertical swing.
- Read live settings every 15 seconds while the page is visible.
- Preserve unsent edits during refresh and show the last confirmed settings when disconnected.
- Optional access from other devices on a trusted home network.

Commands are authenticated with Gree's AES-GCM protocol and the panel reads settings back to confirm the result. Some modes and models restrict fan speed, temperature, heating, or swing options.

## Compatibility

This adaptation was tested with a Gree Wi-Fi unit that uses AES-ECB discovery responses and **AES-GCM protocol v2** for binding, status, and control. The unit must already be connected to Wi-Fi using GREE+ or its manufacturer app. This does not provision Wi-Fi and is not a universal driver for every Gree model. Pure ECB control and GCM discovery are not implemented in this adaptation.

## Build and run on Windows

Install a **Java 8 JDK** and **Maven 3.5 or later**, set `JAVA_HOME` to the JDK, and make Maven available on `PATH`.

From the repository folder:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build-Windows.ps1
```

The build runs the tests and produces a self-contained `dist` folder with a copy of your JDK's Java runtime. Keep the runtime's license and notice files with it.

1. Double-click `dist\Start-Gree.cmd`.
2. On first launch, enter the AC's local IPv4 address. Find it in your router's connected-device list.
3. Open [the control panel](http://127.0.0.1:8081/).
4. Use `Stop-Gree.cmd` to stop the PC service. This does not change the AC's power state.

You can move the complete `dist` folder to another location. The PC must remain awake and on the AC's network. No startup task is installed. If a router changes the AC's address, update `ac-address.txt` and restart the service.

### Run directly from Java

```powershell
mvn -DforkCount=0 clean package
java -Dgree.address=YOUR_AC_IP -jar target/airconditioner-remote-1.0-SNAPSHOT.jar
```

Using in-process tests avoids an old Surefire fork issue on Windows. The API documentation is available at [Swagger UI](http://127.0.0.1:8081/swagger-ui.html).

## Optional home-network access

The default listener is **127.0.0.1:8081 only**. For LAN access:

1. Create `lan-address.txt` beside the launcher with this PC's LAN IPv4 address.
2. In an administrator PowerShell, run `Enable-LAN.ps1 -RemoteSubnet YOUR_HOME_SUBNET_CIDR` from that same folder. For example, use `192.168.0.0/24` only if it matches your home network.
3. Restart the controller and open `http://YOUR_PC_IP:8081/` from another device on that subnet.

The optional connector binds to the configured LAN address as well as localhost. The firewall rule is restricted to this Java executable, local address, TCP port 8081, and the specified remote subnet. Empty `lan-address.txt` and restart to disable the LAN listener. If your PC's address changes, update the file and rerun the firewall script with the correct subnet.

**This is an older, unauthenticated HTTP service. Anyone with network access to it can control the AC. Use only on a trusted local network; do not expose it to the internet.** It inherits an old Spring Boot dependency stack and is not a hardened public web service.

## API

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/status` | GET | Read current AC settings |
| `/powerOn` | GET | Turn on the AC |
| `/powerOff` | GET | Turn off the AC |
| `/temperature?temperature=25` | GET | Set Celsius temperature |
| `/settings` | POST | Change mode, fan speed, and/or swing |

`/settings` accepts URL-encoded form fields; only supplied settings are changed:

- `mode`: `AUTO`, `COOL`, `DRY`, `FAN`, `HEAT`
- `fanSpeed`: `AUTO`, `LOW`, `MEDIUM_LOW`, `MEDIUM`, `MEDIUM_HIGH`, `HIGH`
- `swing`: integer `0`–`11`

One configured AC is controlled. Legacy GET mutation routes remain for compatibility. No cloud credentials or GREE+ account tokens are required.

## Validation

The Java tests cover scan serialization, command wire values, firmware metadata compatibility, an independently generated AES-GCM test vector, authentication-tag rejection, and settings API validation. The original physical-device test remains skipped. Live binding, status, temperature and settings command acknowledgements were checked on one compatible unit; not every operating mode was exercised.

## Attribution and license status

Based on [alexmuntean/gree-airconditioner-rest](https://github.com/alexmuntean/gree-airconditioner-rest), snapshot `1afb96585857bd7655255f3a0d634751c6172437`. The original author credits the protocol research in [tomikaa87/gree-remote](https://github.com/tomikaa87/gree-remote). Protocol v2 constants were cross-checked against [cmroche/greeclimate](https://github.com/cmroche/greeclimate/blob/master/greeclimate/cipher.py); the Java AES-GCM implementation here uses the JDK crypto API.

Changes include the browser GUI, protocol v2 support, additional firmware metadata handling, bounded timeouts, serialized UDP commands, authenticated control responses, mode/fan/swing endpoints, Windows packaging, and an optional LAN listener.

No license file was present in the upstream snapshot. This repository does not add a license grant for upstream code. Check the original authors' permissions and dependency licenses before redistributing it or using it commercially.

Runtime downloads, compiled files, logs, local addresses, and device-specific configuration are excluded from Git.

