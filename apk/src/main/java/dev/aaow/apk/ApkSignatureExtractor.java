package dev.aaow.apk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Trích xuất chứng chỉ số X.509 DER thật từ file APK:
 * <ul>
 *   <li>APK Signature Scheme v2 / v3 / v3.1 (khối ký {@code APK Sig Block 42} trước Central Directory)</li>
 *   <li>JAR Signing Scheme v1 ({@code META-INF/*.RSA}, {@code *.DSA}, {@code *.EC} định dạng PKCS#7 / CMS)</li>
 *   <li>Tự tạo chứng chỉ X.509 v3 DER hợp lệ chuẩn RFC 5280 cho APK chưa ký (mẫu dev) hoặc gói ảo microG-lite.</li>
 * </ul>
 */
public final class ApkSignatureExtractor {
    public static final int SCHEME_NONE = 0;
    public static final int SCHEME_JAR_V1 = 1;
    public static final int SCHEME_APK_V2 = 2;
    public static final int SCHEME_APK_V3 = 3;

    private static final byte[] APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".getBytes(StandardCharsets.US_ASCII);
    private static final int APK_SIG_SCHEME_V2_ID = 0x7109871a;
    private static final int APK_SIG_SCHEME_V3_ID = 0xf05368c0;
    private static final int APK_SIG_SCHEME_V31_ID = 0x1b93ad61;
    private static final int ZIP_EOCD_SIG = 0x06054b50;

    // sha256WithRSAEncryption: SEQUENCE { OID 1.2.840.113549.1.1.11, NULL }
    private static final byte[] SHA256_WITH_RSA_ALG_ID = new byte[]{
            0x30, 0x0d,
            0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x0b,
            0x05, 0x00
    };

    private static final Map<String, byte[]> SELF_SIGNED_CACHE = new ConcurrentHashMap<>();

    public record SigningResult(List<byte[]> certificates, int schemeVersion) {
        public boolean hasCertificates() {
            return certificates != null && !certificates.isEmpty();
        }
    }

    private ApkSignatureExtractor() {
    }

    /**
     * Trích xuất chứng chỉ X.509 DER thật từ APK (ưu tiên v3 -> v2 -> v1 {@code META-INF/*.RSA|*.DSA|*.EC}).
     */
    public static SigningResult extractFromApk(Path apkPath, ZipFile zip) {
        // 1. Thử APK Signature Scheme v3 / v2 từ APK Signing Block
        if (apkPath != null) {
            SigningResult v2v3 = extractV2V3Certificates(apkPath);
            if (v2v3.hasCertificates()) {
                return v2v3;
            }
        }
        // 2. Thử JAR Signing Scheme v1 (META-INF/*.RSA, *.DSA, *.EC)
        if (zip != null) {
            List<byte[]> v1Certs = extractV1Certificates(zip);
            if (!v1Certs.isEmpty()) {
                return new SigningResult(Collections.unmodifiableList(v1Certs), SCHEME_JAR_V1);
            }
        }
        return new SigningResult(Collections.emptyList(), SCHEME_NONE);
    }

    /**
     * Trích xuất chứng chỉ X.509 DER từ các khối PKCS#7 {@code META-INF/*.RSA}, {@code *.DSA}, {@code *.EC}.
     */
    public static List<byte[]> extractV1Certificates(ZipFile zip) {
        if (zip == null) return Collections.emptyList();
        List<ZipEntry> sigEntries = new ArrayList<>();
        zip.stream().forEach(entry -> {
            if (entry.isDirectory()) return;
            String name = entry.getName();
            if (!name.startsWith("META-INF/")) return;
            // Chỉ xét file chữ ký nằm trực tiếp trong META-INF/ (không nằm trong thư mục con)
            if (name.indexOf('/', 9) >= 0) return;
            String upper = name.toUpperCase(Locale.ROOT);
            if (upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC")) {
                sigEntries.add(entry);
            }
        });
        if (sigEntries.isEmpty()) return Collections.emptyList();

        sigEntries.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        List<byte[]> result = new ArrayList<>();
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            return Collections.emptyList();
        }

        for (ZipEntry entry : sigEntries) {
            try (InputStream in = zip.getInputStream(entry)) {
                byte[] pkcs7Bytes = in.readAllBytes();
                extractCertificatesFromPkcs7(cf, pkcs7Bytes, result);
            } catch (Exception ignored) {
            }
        }
        return deduplicate(result);
    }

    private static void extractCertificatesFromPkcs7(CertificateFactory cf, byte[] data, List<byte[]> out) {
        if (data == null || data.length == 0) return;
        try {
            Collection<? extends Certificate> certs = cf.generateCertificates(new ByteArrayInputStream(data));
            if (certs != null && !certs.isEmpty()) {
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate x509) {
                        byte[] encoded = x509.getEncoded();
                        if (encoded != null && encoded.length > 0) {
                            out.add(encoded);
                        }
                    }
                }
                return;
            }
        } catch (Exception ignored) {
        }
        // Fallback: thử giải mã trực tiếp như một X.509 DER đơn lẻ
        try {
            Certificate single = cf.generateCertificate(new ByteArrayInputStream(data));
            if (single instanceof X509Certificate x509) {
                byte[] encoded = x509.getEncoded();
                if (encoded != null && encoded.length > 0) {
                    out.add(encoded);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Trích xuất chứng chỉ X.509 DER từ khối APK Signing Block (Scheme v2 / v3 / v3.1).
     */
    public static SigningResult extractV2V3Certificates(Path apkPath) {
        if (apkPath == null) return new SigningResult(Collections.emptyList(), SCHEME_NONE);
        try (FileChannel ch = FileChannel.open(apkPath, StandardOpenOption.READ)) {
            long fileSize = ch.size();
            if (fileSize < 64) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }
            long cdOffset = findCentralDirectoryOffset(ch, fileSize);
            if (cdOffset < 32 || cdOffset > fileSize) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }

            // Kiểm tra footer của APK Signing Block (24 byte ngay trước Central Directory: sizeOfBlock[8] + magic[16])
            ByteBuffer footer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
            readFully(ch, cdOffset - 24, footer);
            footer.flip();
            long sizeInFooter = footer.getLong();
            byte[] magic = new byte[16];
            footer.get(magic);
            if (!Arrays.equals(APK_SIG_BLOCK_MAGIC, magic)) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }
            if (sizeInFooter < 24 || sizeInFooter > cdOffset - 8 || sizeInFooter > 32L * 1024 * 1024) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }

            long blockStart = cdOffset - sizeInFooter - 8;
            if (blockStart < 0) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }

            int totalBlockLen = (int) (sizeInFooter + 8);
            ByteBuffer block = ByteBuffer.allocate(totalBlockLen).order(ByteOrder.LITTLE_ENDIAN);
            readFully(ch, blockStart, block);
            block.flip();

            long sizeInHeader = block.getLong();
            if (sizeInHeader != sizeInFooter) {
                return new SigningResult(Collections.emptyList(), SCHEME_NONE);
            }

            // Duyệt các cặp (len[8], id[4], value[len-4]) cho đến 24 byte cuối (footer)
            int pairsEnd = totalBlockLen - 24;
            Map<Integer, byte[]> schemeValues = new LinkedHashMap<>();
            while (block.position() + 12 <= pairsEnd) {
                long pairLen = block.getLong();
                if (pairLen < 4 || pairLen > pairsEnd - block.position()) {
                    break;
                }
                int id = block.getInt();
                int valueLen = (int) (pairLen - 4);
                byte[] value = new byte[valueLen];
                block.get(value);
                if (id == APK_SIG_SCHEME_V2_ID || id == APK_SIG_SCHEME_V3_ID || id == APK_SIG_SCHEME_V31_ID) {
                    schemeValues.put(id, value);
                }
            }

            // Ưu tiên v3.1 -> v3 -> v2
            int[] priority = {APK_SIG_SCHEME_V31_ID, APK_SIG_SCHEME_V3_ID, APK_SIG_SCHEME_V2_ID};
            for (int schemeId : priority) {
                byte[] blockValue = schemeValues.get(schemeId);
                if (blockValue == null) continue;
                List<byte[]> certs = parseSignersCertificates(blockValue);
                if (!certs.isEmpty()) {
                    int version = (schemeId == APK_SIG_SCHEME_V2_ID) ? SCHEME_APK_V2 : SCHEME_APK_V3;
                    return new SigningResult(Collections.unmodifiableList(certs), version);
                }
            }
        } catch (Exception ignored) {
        }
        return new SigningResult(Collections.emptyList(), SCHEME_NONE);
    }

    private static List<byte[]> parseSignersCertificates(byte[] schemeBlock) {
        List<byte[]> out = new ArrayList<>();
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            return out;
        }
        ByteBuffer buf = ByteBuffer.wrap(schemeBlock).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer signers = readLengthPrefixedSlice(buf);
        if (signers == null) return out;

        while (signers.hasRemaining()) {
            ByteBuffer signer = readLengthPrefixedSlice(signers);
            if (signer == null) break;
            ByteBuffer signedData = readLengthPrefixedSlice(signer);
            if (signedData == null) continue;
            // Bỏ qua digests (length-prefixed)
            if (readLengthPrefixedSlice(signedData) == null) continue;
            // Đọc certificates (length-prefixed sequence of length-prefixed X.509 DER certs)
            ByteBuffer certsBuf = readLengthPrefixedSlice(signedData);
            if (certsBuf == null) continue;
            while (certsBuf.hasRemaining()) {
                ByteBuffer certSlice = readLengthPrefixedSlice(certsBuf);
                if (certSlice == null) break;
                byte[] rawCert = new byte[certSlice.remaining()];
                certSlice.get(rawCert);
                try {
                    Certificate parsed = cf.generateCertificate(new ByteArrayInputStream(rawCert));
                    if (parsed instanceof X509Certificate x509) {
                        out.add(x509.getEncoded());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return deduplicate(out);
    }

    private static ByteBuffer readLengthPrefixedSlice(ByteBuffer src) {
        if (src == null || src.remaining() < 4) return null;
        int len = src.getInt();
        if (len < 0 || len > src.remaining()) return null;
        ByteBuffer slice = src.slice().order(ByteOrder.LITTLE_ENDIAN);
        slice.limit(len);
        src.position(src.position() + len);
        return slice;
    }

    private static long findCentralDirectoryOffset(FileChannel ch, long fileSize) throws IOException {
        int maxSearch = (int) Math.min(fileSize, 65557L);
        ByteBuffer tail = ByteBuffer.allocate(maxSearch).order(ByteOrder.LITTLE_ENDIAN);
        readFully(ch, fileSize - maxSearch, tail);
        tail.flip();

        for (int i = maxSearch - 22; i >= 0; i--) {
            if (tail.getInt(i) == ZIP_EOCD_SIG) {
                int commentLen = tail.getShort(i + 20) & 0xFFFF;
                if (i + 22 + commentLen == maxSearch) {
                    return Integer.toUnsignedLong(tail.getInt(i + 16));
                }
            }
        }
        return -1L;
    }

    private static void readFully(FileChannel ch, long position, ByteBuffer dst) throws IOException {
        long pos = position;
        while (dst.hasRemaining()) {
            int n = ch.read(dst, pos);
            if (n < 0) {
                throw new IOException("Unexpected EOF while reading APK at offset " + pos);
            }
            pos += n;
        }
    }

    private static List<byte[]> deduplicate(List<byte[]> input) {
        List<byte[]> out = new ArrayList<>();
        for (byte[] b : input) {
            boolean dup = false;
            for (byte[] existing : out) {
                if (Arrays.equals(existing, b)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) out.add(b);
        }
        return out;
    }

    /**
     * Sinh chứng chỉ X.509 v3 DER tự ký hoàn chỉnh (chuẩn RFC 5280, giải mã được bằng
     * {@code CertificateFactory.getInstance("X.509")}) cho APK chưa ký hoặc gói ảo microG-lite.
     */
    public static byte[] buildSelfSignedX509Der(String commonName, String organization) {
        String cn = (commonName != null && !commonName.isBlank()) ? commonName : "AAOW";
        String org = (organization != null && !organization.isBlank()) ? organization : "AAOW";
        String cacheKey = cn + "|" + org;
        return SELF_SIGNED_CACHE.computeIfAbsent(cacheKey, k -> generateSelfSignedX509Der(cn, org)).clone();
    }

    private static byte[] generateSelfSignedX509Der(String cn, String org) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();

            // Serial number dương, xác định từ tên chủ thể
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((cn + ":" + org).getBytes(StandardCharsets.UTF_8));
            byte[] serialBytes = new byte[8];
            System.arraycopy(hash, 0, serialBytes, 0, 8);
            serialBytes[0] = (byte) ((serialBytes[0] & 0x7F) | 0x01);
            BigInteger serial = new BigInteger(1, serialBytes);

            byte[] versionExplicit = derTaggedExplicit(0, derInteger(BigInteger.valueOf(2))); // v3
            byte[] serialDer = derInteger(serial);
            byte[] nameDer = derName(cn, org, "US");
            byte[] validityDer = derSequence(
                    derUtcTime("240101000000Z"),
                    derUtcTime("491231235959Z")
            );
            byte[] spkiDer = kp.getPublic().getEncoded();

            byte[] tbsCert = derSequence(
                    versionExplicit,
                    serialDer,
                    SHA256_WITH_RSA_ALG_ID,
                    nameDer,
                    validityDer,
                    nameDer,
                    spkiDer
            );

            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(kp.getPrivate());
            signer.update(tbsCert);
            byte[] sigBytes = signer.sign();

            byte[] certDer = derSequence(
                    tbsCert,
                    SHA256_WITH_RSA_ALG_ID,
                    derBitString(sigBytes)
            );

            // Tự xác thực ngay rằng chứng chỉ hợp lệ tuyệt đối với X.509 parser của JDK
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate parsed = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certDer));
            return parsed.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể sinh chứng chỉ X.509 DER hợp lệ", e);
        }
    }

    private static byte[] derName(String cn, String org, String country) {
        // RDN CN (2.5.4.3), O (2.5.4.10), C (2.5.4.6)
        byte[] rdnCn = derSet(derSequence(new byte[]{0x06, 0x03, 0x55, 0x04, 0x03}, derUtf8String(cn)));
        byte[] rdnOrg = derSet(derSequence(new byte[]{0x06, 0x03, 0x55, 0x04, 0x0a}, derUtf8String(org)));
        byte[] rdnC = derSet(derSequence(new byte[]{0x06, 0x03, 0x55, 0x04, 0x06}, derPrintableString(country)));
        return derSequence(rdnCn, rdnOrg, rdnC);
    }

    private static byte[] derSequence(byte[]... elements) {
        return derTag(0x30, concat(elements));
    }

    private static byte[] derSet(byte[]... elements) {
        return derTag(0x31, concat(elements));
    }

    private static byte[] derTaggedExplicit(int tagNo, byte[] inner) {
        return derTag(0xA0 | (tagNo & 0x1F), inner);
    }

    private static byte[] derInteger(BigInteger value) {
        return derTag(0x02, value.toByteArray());
    }

    private static byte[] derUtf8String(String s) {
        return derTag(0x0C, s.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] derPrintableString(String s) {
        return derTag(0x13, s.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] derUtcTime(String utc) {
        return derTag(0x17, utc.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] derBitString(byte[] bytes) {
        byte[] payload = new byte[bytes.length + 1];
        payload[0] = 0; // 0 unused bits
        System.arraycopy(bytes, 0, payload, 1, bytes.length);
        return derTag(0x03, payload);
    }

    private static byte[] derTag(int tag, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length + 6);
        out.write(tag);
        int len = content.length;
        if (len < 0x80) {
            out.write(len);
        } else if (len <= 0xFF) {
            out.write(0x81);
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(0x82);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(0x83);
            out.write((len >>> 16) & 0xFF);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        }
        out.write(content, 0, content.length);
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] out = new byte[total];
        int off = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, off, a.length);
            off += a.length;
        }
        return out;
    }
}
