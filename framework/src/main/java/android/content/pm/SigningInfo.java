package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Thông tin chứng chỉ ký số của gói Android (API 28+), dùng cùng
 * {@link PackageManager#GET_SIGNING_CERTIFICATES}.
 */
public final class SigningInfo implements Parcelable {
    public static final int VERSION_JAR = 1;
    public static final int VERSION_SIGNING_BLOCK_V2 = 2;
    public static final int VERSION_SIGNING_BLOCK_V3 = 3;
    public static final int VERSION_SIGNING_BLOCK_V4 = 4;

    private final int schemeVersion;
    private final Signature[] apkContentsSigners;
    private final Signature[] signingCertificateHistory;
    private final Collection<PublicKey> publicKeys;

    public SigningInfo() {
        this(0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public SigningInfo(SigningInfo orig) {
        if (orig == null) {
            this.schemeVersion = 0;
            this.apkContentsSigners = new Signature[0];
            this.signingCertificateHistory = new Signature[0];
            this.publicKeys = Collections.emptyList();
        } else {
            this.schemeVersion = orig.schemeVersion;
            this.apkContentsSigners = orig.apkContentsSigners.clone();
            this.signingCertificateHistory = orig.signingCertificateHistory.clone();
            this.publicKeys = new ArrayList<>(orig.publicKeys);
        }
    }

    public SigningInfo(int schemeVersion, Collection<Signature> apkContentsSigners,
                       Collection<PublicKey> publicKeys, Collection<Signature> signingCertificateHistory) {
        this.schemeVersion = schemeVersion;
        this.apkContentsSigners = apkContentsSigners != null
                ? apkContentsSigners.toArray(new Signature[0])
                : new Signature[0];
        this.signingCertificateHistory = signingCertificateHistory != null && !signingCertificateHistory.isEmpty()
                ? signingCertificateHistory.toArray(new Signature[0])
                : this.apkContentsSigners.clone();
        if (publicKeys != null && !publicKeys.isEmpty()) {
            this.publicKeys = Collections.unmodifiableList(new ArrayList<>(publicKeys));
        } else {
            this.publicKeys = Collections.unmodifiableList(extractPublicKeys(this.apkContentsSigners));
        }
    }

    public static SigningInfo fromSignatures(int schemeVersion, Signature[] signatures) {
        List<Signature> list = signatures != null ? Arrays.asList(signatures) : Collections.emptyList();
        return new SigningInfo(schemeVersion, list, null, list);
    }

    private static List<PublicKey> extractPublicKeys(Signature[] sigs) {
        List<PublicKey> keys = new ArrayList<>();
        if (sigs == null || sigs.length == 0) return keys;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (Signature sig : sigs) {
                if (sig == null) continue;
                try {
                    Certificate cert = cf.generateCertificate(new ByteArrayInputStream(sig.toByteArray()));
                    if (cert != null && cert.getPublicKey() != null) {
                        keys.add(cert.getPublicKey());
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return keys;
    }

    public boolean hasMultipleSigners() {
        return apkContentsSigners.length > 1;
    }

    public boolean hasPastSigningCertificates() {
        return signingCertificateHistory.length > 1;
    }

    public Signature[] getSigningCertificateHistory() {
        if (hasMultipleSigners()) {
            return null;
        }
        return signingCertificateHistory.length > 0 ? signingCertificateHistory.clone() : null;
    }

    public Signature[] getApkContentsSigners() {
        return apkContentsSigners.length > 0 ? apkContentsSigners.clone() : null;
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }

    public Collection<PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public boolean signersMatchExactly(SigningInfo other) {
        if (other == null) return false;
        Set<Signature> mine = new LinkedHashSet<>(Arrays.asList(apkContentsSigners));
        Set<Signature> theirs = new LinkedHashSet<>(Arrays.asList(other.apkContentsSigners));
        return mine.equals(theirs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(schemeVersion);
        dest.writeTypedList(Arrays.asList(apkContentsSigners));
        dest.writeTypedList(Arrays.asList(signingCertificateHistory));
    }

    public static final Creator<SigningInfo> CREATOR = new Creator<>() {
        @Override
        public SigningInfo createFromParcel(Parcel source) {
            int version = source.readInt();
            List<Signature> current = source.createTypedArrayList(Signature.CREATOR);
            List<Signature> history = source.createTypedArrayList(Signature.CREATOR);
            return new SigningInfo(version, current, null, history);
        }

        @Override
        public SigningInfo[] newArray(int size) {
            return new SigningInfo[size];
        }
    };
}
