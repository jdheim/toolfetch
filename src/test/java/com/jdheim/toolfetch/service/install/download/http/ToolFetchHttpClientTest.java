/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.download.http;

import static com.jdheim.toolfetch.builder.TestConfigurationBuilder.configuration;
import static com.jdheim.toolfetch.builder.TestHttpBuilder.http;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import javax.net.ssl.SSLContext;
import com.jdheim.toolfetch.model.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// OOC Tests for [ToolFetchHttpClient]
class ToolFetchHttpClientTest {

    @BeforeEach
    void setUp() {
        ToolFetchHttpClient.reset();
    }

    @AfterEach
    void tearDown() {
        ToolFetchHttpClient.reset();
    }

    @Test
    void testHttpClient_FromJavaHome() {
        Configuration configuration = configuration().build();
        testHttpClient(configuration);
    }

    @Test
    void testHttpClient_FromConfiguration() {
        Configuration configuration = configuration().http(http().withDefaultTrustStore().build()).build();
        testHttpClient(configuration);
    }

    private void testHttpClient(Configuration configuration) {
        HttpClient httpClient = ToolFetchHttpClient.getInstance(configuration);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_2);
        assertThat(httpClient.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
        SSLContext sslContext = httpClient.sslContext();
        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");
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
    void testHttpClient_FromConfiguration_SingletonThreadSafety() {
        Configuration configuration = configuration().http(http().withDefaultTrustStore().build()).build();
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
