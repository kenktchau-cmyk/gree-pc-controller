package com.gree.airconditioner.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gree.airconditioner.dto.Command;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/** Gree protocol v2 constants match the device firmware's wire format. */
public final class GreeGcm {
    public static final String GENERIC_KEY = "{yxAHAY_Lm6pbC/<";
    private static final byte[] NONCE = {0x54,0x40,0x78,0x44,0x49,0x67,0x5a,0x51,0x6c,0x5e,0x63,0x13};
    private static final byte[] AAD = "qualcomm-test".getBytes(StandardCharsets.US_ASCII);

    private static Cipher cipher(int mode, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key.getBytes(StandardCharsets.US_ASCII), "AES"), new GCMParameterSpec(128, NONCE));
        cipher.updateAAD(AAD);
        return cipher;
    }

    public static void encode(Command command, String json, String key) {
        try {
            byte[] data = cipher(Cipher.ENCRYPT_MODE, key).doFinal(json.getBytes(StandardCharsets.UTF_8));
            command.setPack(Base64.getEncoder().encodeToString(Arrays.copyOf(data, data.length - 16)));
            command.setTag(Base64.getEncoder().encodeToString(Arrays.copyOfRange(data, data.length - 16, data.length)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode Gree protocol v2 request", e);
        }
    }

    public static void encodeObject(Command command, Object data, String key) {
        try { encode(command, new ObjectMapper().writeValueAsString(data), key); }
        catch (Exception e) { throw new IllegalStateException("Unable to serialize Gree request", e); }
    }

    public static JsonNode decode(String envelope, String key) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode packet = mapper.readTree(envelope);
            byte[] pack = Base64.getDecoder().decode(packet.get("pack").asText());
            byte[] tag = Base64.getDecoder().decode(packet.get("tag").asText());
            if (tag.length != 16) throw new IllegalArgumentException("Invalid Gree authentication tag");
            byte[] combined = Arrays.copyOf(pack, pack.length + tag.length);
            System.arraycopy(tag, 0, combined, pack.length, tag.length);
            return mapper.readTree(cipher(Cipher.DECRYPT_MODE, key).doFinal(combined));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to authenticate Gree protocol v2 response", e);
        }
    }
}
