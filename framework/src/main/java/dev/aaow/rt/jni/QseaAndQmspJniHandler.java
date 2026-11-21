package dev.aaow.rt.jni;

import android.content.Context;
import dev.aaow.rt.NativeBridge;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * Bộ giả lập JNI HLE cho các thư viện bảo mật/telemetry thương mại phổ biến trên APK thực tế:
 * - {@code libqsealib.so} ({@code com.ola.qsea.encrypt.U}): cung cấp khóa công khai RSA X.509 hợp lệ
 *   (tránh {@code InvalidKeySpecException: Missing key encoding}), mã hóa/giải mã AES-CBC thật và
 *   định danh thiết bị AAOW nhất quán.
 * - {@code libqmp.so} ({@code com.tencent.qmsp.sdk.u.U}): trả mã phiên bản / trạng thái an toàn.
 */
public final class QseaAndQmspJniHandler implements NativeJniHandler {
    private static volatile String cachedRsaPubKeyBase64;
    private static final String AAOW_DEVICE_SEED = UUID.nameUUIDFromBytes(
            ("aaow-qsea:" + System.getProperty("user.name", "windows")).getBytes(StandardCharsets.UTF_8))
            .toString().replace("-", "");

    @Override
    public boolean supports(String className) {
        return "com.ola.qsea.encrypt.U".equals(className)
                || "com.tencent.qmsp.sdk.u.U".equals(className);
    }

    @Override
    public Object invoke(String methodSig, String className, String methodName,
                         String descriptor, Object self, Object[] args) {
        if ("com.ola.qsea.encrypt.U".equals(className)) {
            return handleQsea(methodName, descriptor, args);
        }
        if ("com.tencent.qmsp.sdk.u.U".equals(className)) {
            return handleQmsp(methodName, descriptor, args);
        }
        return NativeBridge.UNHANDLED;
    }

    private Object handleQsea(String methodName, String descriptor, Object[] args) {
        return switch (methodName) {
            case "n" -> null; // init(Context, String) -> void
            case "p" -> getOrGenerateRsaPublicKeyBase64(); // X.509 Base64 RSA public key
            case "o" -> "AAOW-QSEA-2.0";
            case "r" -> AAOW_DEVICE_SEED;
            case "s" -> sha256Hex("qsea-salt:" + AAOW_DEVICE_SEED).substring(0, 32);
            case "u" -> "windows-aaow-x86_64";
            case "z" -> {
                String pkg = (args != null && args.length > 0 && args[0] instanceof Context c && c.getPackageName() != null)
                        ? c.getPackageName() : "aaow.app";
                yield sha256Hex("qsea-ctx:" + pkg + ":" + AAOW_DEVICE_SEED).substring(0, 32);
            }
            case "x" -> {
                String prop = (args != null && args.length > 0 && args[0] instanceof String s) ? s : "";
                if (prop.isEmpty()) yield "";
                yield android.os.SystemProperties.get(prop, "");
            }
            case "c" -> new String[]{
                    AAOW_DEVICE_SEED,
                    "AAOW",
                    "windows_pc",
                    "15"
            };
            case "d" -> "{}".getBytes(StandardCharsets.UTF_8);
            case "e" -> (byte) 0;
            case "a" -> {
                if ("(I[B[B[B)[B".equals(descriptor) && args != null && args.length >= 4) {
                    yield handleQseaAes(args);
                }
                yield NativeBridge.UNHANDLED;
            }
            default -> NativeBridge.UNHANDLED;
        };
    }

    private static byte[] handleQseaAes(Object[] args) {
        int mode = args[0] instanceof Number n ? n.intValue() : 1;
        byte[] key = args[1] instanceof byte[] b ? b : null;
        byte[] iv = args[2] instanceof byte[] b ? b : null;
        byte[] input = args[3] instanceof byte[] b ? b : null;
        if (key == null || input == null || input.length == 0) {
            return null;
        }
        try {
            byte[] key16 = normalize16(key);
            byte[] iv16 = normalize16(iv != null ? iv : key);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int opMode = (mode == Cipher.DECRYPT_MODE) ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE;
            cipher.init(opMode, new SecretKeySpec(key16, "AES"), new IvParameterSpec(iv16));
            return cipher.doFinal(input);
        } catch (Throwable ignored) {
            // Trả null để com.ola.qsea.b.a tự rơi về nhánh Java AES chuẩn nếu dữ liệu không khớp
            return null;
        }
    }

    private static byte[] normalize16(byte[] src) {
        if (src.length == 16 || src.length == 24 || src.length == 32) {
            return src;
        }
        byte[] out = new byte[16];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, 16));
        return out;
    }

    private Object handleQmsp(String methodName, String descriptor, Object[] args) {
        if ("a".equals(methodName) && descriptor.endsWith(")Ljava/lang/String;")) {
            return "{\"ret\":0,\"aaow\":true}";
        }
        if ("b".equals(methodName) && descriptor.endsWith(")I")) {
            return 0;
        }
        return NativeBridge.UNHANDLED;
    }

    public static String getOrGenerateRsaPublicKeyBase64() {
        String key = cachedRsaPubKeyBase64;
        if (key != null) return key;
        synchronized (QseaAndQmspJniHandler.class) {
            if (cachedRsaPubKeyBase64 != null) return cachedRsaPubKeyBase64;
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                cachedRsaPubKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate RSA public key for QSea HLE", e);
            }
            return cachedRsaPubKeyBase64;
        }
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return AAOW_DEVICE_SEED;
        }
    }
}
