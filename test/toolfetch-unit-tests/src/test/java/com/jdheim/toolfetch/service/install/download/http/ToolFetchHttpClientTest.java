/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download.http;

import static com.jdheim.toolfetch.builder.TestConfigurationBuilder.configuration;
import static com.jdheim.toolfetch.builder.TestHttpBuilder.http;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.net.ssl.SSLContext;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.service.info.JavaHome;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/// OOC Tests for [ToolFetchHttpClient]
class ToolFetchHttpClientTest {

    @TempDir
    Path tempDir;

    TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() {
        ToolFetchHttpClient.reset();
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
    }

    @AfterEach
    void tearDown() {
        ToolFetchHttpClient.reset();
    }

    @Test
    void testHttpClient_FromJavaHome() throws GeneralSecurityException, IOException {
        Path javaHomePath = tempDir.resolve("jdk");
        Path javaCacerts = createCacerts(javaHomePath.resolve("lib", "security", "cacerts"));
        Configuration configuration = configuration().build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            javaHome.when(JavaHome::get).thenReturn(javaHomePath);
            testHttpClient(configuration);
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using TrustStore from %s".formatted(javaCacerts));
        }
    }

    @Test
    void testHttpClient_FromJdk8JavaHome() throws GeneralSecurityException, IOException {
        Path javaHomePath = tempDir.resolve("jdk");
        Path jdk8Cacerts = createCacerts(javaHomePath.resolve("jre", "lib", "security", "cacerts"));
        Configuration configuration = configuration().build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            javaHome.when(JavaHome::get).thenReturn(javaHomePath);
            testHttpClient(configuration);
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using TrustStore from %s".formatted(jdk8Cacerts));
        }
    }

    @Test
    void testHttpClient_FromConfiguration() throws GeneralSecurityException, IOException {
        Path cacerts = createCacerts(tempDir.resolve("custom", "cacerts"));
        Configuration configuration = configuration().http(http().trustStorePath(cacerts.toString()).build()).build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            testHttpClient(configuration);
            javaHome.verifyNoInteractions();
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using TrustStore from %s".formatted(cacerts));
        }
    }

    @Test
    void testHttpClient_WithoutJavaHome() {
        Configuration configuration = configuration().build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            javaHome.when(JavaHome::get).thenReturn(null);
            testHttpClient(configuration, "Default");
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using bundled default TrustStore");
        }
    }

    @Test
    void testHttpClient_JavaHomeWithoutCacerts() {
        Configuration configuration = configuration().build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            javaHome.when(JavaHome::get).thenReturn(tempDir);
            testHttpClient(configuration, "Default");
            testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(),
                    "%s does not exist. Falling back to the bundled default TrustStore".formatted(
                            tempDir.resolve("lib", "security", "cacerts")));
        }
    }

    @Test
    void testHttpClient_InvalidCustomTrustStore() {
        Configuration configuration = configuration().http(http().trustStorePath(tempDir.resolve("missing").toString()).build())
                .build();
        try (MockedStatic<JavaHome> javaHome = mockStatic()) {
            javaHome.when(JavaHome::get).thenReturn(null);
            testHttpClient(configuration, "Default");
            testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(),
                    "Failed to build SSL context from \"%s\"".formatted(tempDir.resolve("missing")));
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using bundled default TrustStore");
        }
    }

    @Test
    void testHttpClient_CustomTrustStoreWithIncorrectPassword() throws GeneralSecurityException, IOException {
        Path cacerts = createCacerts(tempDir.resolve("custom", "cacerts"));
        Configuration configuration = configuration().http(
                http().trustStorePath(cacerts.toString()).trustStoreType("PKCS12").build()).build();
        try (MockedStatic<ToolFetchHttpClient> toolfetchHttpClient = mockStatic(Mockito.CALLS_REAL_METHODS);
             MockedStatic<JavaHome> javaHome = mockStatic()) {
            toolfetchHttpClient.when(() -> ToolFetchHttpClient.trustStorePasswordOrDefault(any(), nullable(char[].class)))
                    .thenReturn("incorrect".toCharArray());
            javaHome.when(JavaHome::get).thenReturn(null);

            testHttpClient(configuration, "Default");

            testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(),
                    "Failed to build SSL context from \"%s\" due to exception:".formatted(cacerts));
            testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "Using bundled default TrustStore");
        }
    }

    private void testHttpClient(Configuration configuration) {
        testHttpClient(configuration, "TLS");
    }

    private void testHttpClient(Configuration configuration, String protocol) {
        HttpClient httpClient = ToolFetchHttpClient.getInstance(configuration);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_2);
        assertThat(httpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
        SSLContext sslContext = httpClient.sslContext();
        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo(protocol);
    }

    private Path createCacerts(Path cacerts) throws GeneralSecurityException, IOException {
        Files.createDirectories(Objects.requireNonNull(cacerts.getParent()));
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, javaCacertsDefaultPassword());
        try (OutputStream outputStream = Files.newOutputStream(cacerts)) {
            keyStore.store(outputStream, javaCacertsDefaultPassword());
        }
        return cacerts;
    }

    private char[] javaCacertsDefaultPassword() {
        return "changeit".toCharArray();
    }

    @Test
    void testHttpClient_DefaultConnectTimeout() {
        Configuration configuration = configuration().build();
        testHttpClient_ConnectTimeout(configuration, 10);
    }

    @Test
    void testHttpClient_CustomConnectTimeout() {
        Configuration configuration = configuration().http(http().connectTimeout(30).build()).build();
        testHttpClient_ConnectTimeout(configuration, 30);
    }

    private void testHttpClient_ConnectTimeout(Configuration configuration, int expectedConnectTimeout) {
        HttpClient httpClient = ToolFetchHttpClient.getInstance(configuration);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.connectTimeout()).hasValue(Duration.ofSeconds(expectedConnectTimeout));
    }

    @Test
    void testHttpClient_FromJavaHome_SingletonThreadSafety() {
        Configuration configuration = configuration().build();
        testHttpClient_SingletonThreadSafety(configuration);
    }

    @Test
    void testHttpClient_FromConfiguration_SingletonThreadSafety() throws GeneralSecurityException, IOException {
        Path cacerts = createCacerts(tempDir.resolve("custom", "cacerts"));
        Configuration configuration = configuration().http(http().trustStorePath(cacerts.toString()).build()).build();
        testHttpClient_SingletonThreadSafety(configuration);
    }

    private void testHttpClient_SingletonThreadSafety(Configuration configuration) {
        List<Object> instances = Collections.synchronizedList(new ArrayList<>());

        IntStream.range(0, 100).parallel().forEach(i -> instances.add(ToolFetchHttpClient.getInstance(configuration)));

        assertThat(new HashSet<>(instances)).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"test123,", ",test456", "test123,test456", ","})
    void testTrustStorePassword(String customPassword, String defaultPassword) {
        char[] password = ToolFetchHttpClient.trustStorePasswordOrDefault(() -> customPassword,
                defaultPassword != null ? defaultPassword.toCharArray() : null);
        if (customPassword == null && defaultPassword == null) {
            assertThat(password).isNull();
        } else {
            assertThat(password).isEqualTo(StringUtils.firstNonBlank(customPassword, defaultPassword).toCharArray());
        }
    }

}
