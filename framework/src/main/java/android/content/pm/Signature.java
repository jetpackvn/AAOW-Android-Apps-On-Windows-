package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/** Chữ ký số của gói ứng dụng Android. */
public class Signature implements Parcelable {
    private final byte[] signature;

    public Signature(byte[] signature) {
        this.signature = signature != null ? signature.clone() : new byte[0];
    }

    public Signature(String text) {
        if (text == null || text.isEmpty()) {
            this.signature = new byte[0];
        } else {
            int len = text.length();
            byte[] out = new byte[len / 2];
            for (int i = 0; i + 1 < len; i += 2) {
                out[i / 2] = (byte) ((Character.digit(text.charAt(i), 16) << 4)
                        + Character.digit(text.charAt(i + 1), 16));
            }
            this.signature = out;
        }
    }

    public byte[] toByteArray() {
        return signature.clone();
    }

    public char[] toChars() {
        return toCharsString().toCharArray();
    }

    public char[] toChars(char[] existingArray, int[] outLen) {
        char[] chars = toChars();
        if (outLen != null && outLen.length > 0) {
            outLen[0] = chars.length;
        }
        if (existingArray != null && existingArray.length >= chars.length) {
            System.arraycopy(chars, 0, existingArray, 0, chars.length);
            return existingArray;
        }
        return chars;
    }

    public String toCharsString() {
        StringBuilder sb = new StringBuilder(signature.length * 2);
        for (byte b : signature) {
            int v = b & 0xFF;
            sb.append(Character.forDigit((v >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(v & 0xF, 16));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Signature other && Arrays.equals(signature, other.signature);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signature);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeByteArray(signature);
    }

    public static final Creator<Signature> CREATOR = new Creator<>() {
        @Override
        public Signature createFromParcel(Parcel source) {
            return new Signature(source.createByteArray());
        }

        @Override
        public Signature[] newArray(int size) {
            return new Signature[size];
        }
    };
}
