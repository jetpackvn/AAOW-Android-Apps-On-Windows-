package org.conscrypt;

import java.security.Provider;
import java.security.Security;

/** Cầu nối Conscrypt an toàn sang nhà cung cấp TLS chuẩn của Java 21 (SunJSSE). */
public final class Conscrypt {
    public static final class ProviderBuilder {
        private ProviderBuilder() {
        }

        public ProviderBuilder setName(String name) {
            return this;
        }

        public ProviderBuilder provideTrustManager() {
            return this;
        }

        public ProviderBuilder provideTrustManager(boolean provide) {
            return this;
        }

        public ProviderBuilder defaultTlsProtocol(String protocol) {
            return this;
        }

        public Provider build() {
            Provider p = Security.getProvider("SunJSSE");
            if (p != null) return p;
            Provider[] providers = Security.getProviders();
            return providers.length > 0 ? providers[0] : null;
        }
    }

    private Conscrypt() {
    }

    public static boolean isAvailable() {
        return true;
    }

    public static Provider newProvider() {
        return newProviderBuilder().build();
    }

    public static ProviderBuilder newProviderBuilder() {
        return new ProviderBuilder();
    }

    public static boolean isConscrypt(Provider provider) {
        return false;
    }
}
