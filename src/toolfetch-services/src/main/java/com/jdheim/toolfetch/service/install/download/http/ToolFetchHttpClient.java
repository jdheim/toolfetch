/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ToolFetchHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(ToolFetchHttpClient.class);

    private static final String TLS = "TLS";

    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    private static final String TOOLFETCH_HTTP_SSL_TRUSTSTORE_PASSWORD_ENV = "TOOLFETCH_HTTP_SSL_TRUSTSTORE_PASSWORD";

    private static final Path JDK_8_CACERTS_RELATIVE_PATH = Path.of("jre", "lib", "security", "cacerts");

    private static final Path JDK_9_CACERTS_RELATIVE_PATH = Path.of("lib", "security", "cacerts");

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    @SuppressWarnings("java:S3077")
    private static volatile @Nullable ToolFetchHttpClient instance;

    private final HttpClient httpClient;

    private ToolFetchHttpClient(Configuration configuration) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(connectTimeout(configuration));
        SSLContext sslContext = initSslContext(configuration);
        if (sslContext != null) {
            httpClientBuilder.sslContext(sslContext);
        }
        httpClient = httpClientBuilder.build();
    }

    public static HttpClient getInstance(Configuration configuration) {
        if (instance == null) {
            synchronized (ToolFetchHttpClient.class) {
                if (instance == null) {
                    instance = new ToolFetchHttpClient(configuration);
                }
            }
        }
        return Objects.requireNonNull(instance).httpClient();
    }

    static void reset() {
        synchronized (ToolFetchHttpClient.class) {
            instance = null;
        }
    }

    static char @Nullable [] trustStorePasswordOrDefault(Supplier<@Nullable String> toolfetchHttpSslTrustStorePasswordFromEnv,
            char @Nullable [] defaultPassword) {
        String passwordEnv = toolfetchHttpSslTrustStorePasswordFromEnv.get();
        return StringUtils.isNotBlank(passwordEnv) ? passwordEnv.toCharArray() : defaultPassword;
    }

    private HttpClient httpClient() {
        return httpClient;
    }

    private Duration connectTimeout(Configuration configuration) {
        Http http = configuration.http();
        if (http != null) {
            Integer connectTimeout = http.connectTimeout();
            if (connectTimeout != null) {
                return Duration.ofSeconds(connectTimeout);
            }
        }
        return DEFAULT_CONNECT_TIMEOUT;
    }

    private @Nullable SSLContext initSslContext(Configuration configuration) {
        TrustStore trustStore = configuration.trustStore();
        if (trustStore != null) {
            SSLContext sslContext = initSslContext(trustStore.resolvedPath(),
                    trustStore.type(),
                    customTrustStoreDefaultPassword());
            if (sslContext != null) {
                return sslContext;
            }
        }
        String javaHome = javaHomeEnv();
        if (StringUtils.isNotBlank(javaHome)) {
            Path javaCacerts = resolveJavaCacerts(Path.of(javaHome));
            if (javaCacerts == null) {
                LOG.warn("{} does not exist. Falling back to the bundled default TrustStore",
                        Path.of(javaHome).resolve(JDK_9_CACERTS_RELATIVE_PATH));
                return null;
            }
            return initSslContext(javaCacerts, javaCacertsDefaultPassword());
        }
        LOG.info("Using bundled default TrustStore");
        return null;
    }

    private @Nullable Path resolveJavaCacerts(Path javaHome) {
        Path java9Cacerts = javaHome.resolve(JDK_9_CACERTS_RELATIVE_PATH);
        if (Files.isRegularFile(java9Cacerts)) {
            return java9Cacerts;
        }
        Path java8Cacerts = javaHome.resolve(JDK_8_CACERTS_RELATIVE_PATH);
        if (Files.isRegularFile(java8Cacerts)) {
            return java8Cacerts;
        }
        return null;
    }

    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV", justification =
            "JAVA_HOME is intentionally used as an external JDK location. "
                    + "java.home is not reliable for locating lib/security/cacerts in GraalVM Native Image")
    private @Nullable String javaHomeEnv() {
        return System.getenv(JAVA_HOME_ENV);
    }

    private @Nullable SSLContext initSslContext(Path resolvedPath, char[] defaultPassword) {
        return initSslContext(resolvedPath, null, defaultPassword);
    }

    private @Nullable SSLContext initSslContext(Path resolvedPath, @Nullable String type, char @Nullable [] defaultPassword) {
        try {
            KeyStore keyStore = loadKeyStore(resolvedPath, type, defaultPassword);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, tmf.getTrustManagers(), null);
            LOG.info("Using TrustStore from {}", resolvedPath);
            return sslContext;
        } catch (Exception e) {
            LOG.warn(
                    "Failed to build SSL context from \"{}\" due to exception: \"{}: {}\". Falling back to the next available TrustStore",
                    resolvedPath,
                    e.getClass().getName(),
                    e.getMessage());
            return null;
        }
    }

    private KeyStore loadKeyStore(Path resolvedPath, @Nullable String type, char @Nullable [] defaultPassword) throws
            KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore;
        char[] trustStorePassword = trustStorePasswordOrDefault(this::toolfetchHttpSslTrustStorePasswordEnv, defaultPassword);
        if (StringUtils.isNotBlank(type)) {
            keyStore = KeyStore.getInstance(type);
            try (InputStream is = Files.newInputStream(resolvedPath)) {
                keyStore.load(is, trustStorePassword);
            }
        } else {
            keyStore = KeyStore.getInstance(resolvedPath.toFile(), trustStorePassword);
        }
        return keyStore;
    }

    private @Nullable String toolfetchHttpSslTrustStorePasswordEnv() {
        return System.getenv(TOOLFETCH_HTTP_SSL_TRUSTSTORE_PASSWORD_ENV);
    }

    private char @Nullable [] customTrustStoreDefaultPassword() {
        return null;
    }

    private char[] javaCacertsDefaultPassword() {
        return "changeit".toCharArray();
    }

}
