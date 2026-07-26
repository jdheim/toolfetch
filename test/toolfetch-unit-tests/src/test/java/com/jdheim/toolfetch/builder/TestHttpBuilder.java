/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.http.ssl.Ssl;
import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import org.jspecify.annotations.Nullable;

/// Http Builder for Tests
public class TestHttpBuilder {

    private static final String DEFAULT_TRUSTSTORE_PATH = "$JAVA_HOME/lib/security/cacerts";

    private static final String DEFAULT_TRUSTSTORE_TYPE = "PKCS12";

    private @Nullable Integer connectTimeout;

    private @Nullable Integer requestTimeout;

    private @Nullable String trustStorePath;

    private @Nullable String trustStoreType;

    private TestHttpBuilder() {}

    public static TestHttpBuilder http() {
        return new TestHttpBuilder();
    }

    public TestHttpBuilder connectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public TestHttpBuilder requestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public TestHttpBuilder trustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
        return this;
    }

    public TestHttpBuilder withDefaultTrustStorePath() {
        trustStorePath = DEFAULT_TRUSTSTORE_PATH;
        return this;
    }

    public TestHttpBuilder trustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
        return this;
    }

    public TestHttpBuilder withDefaultTrustStoreType() {
        trustStoreType = DEFAULT_TRUSTSTORE_TYPE;
        return this;
    }

    public TestHttpBuilder withDefaultTrustStore() {
        trustStorePath = DEFAULT_TRUSTSTORE_PATH;
        trustStoreType = DEFAULT_TRUSTSTORE_TYPE;
        return this;
    }

    public Http build() {
        TrustStore trustStore = null;
        if (trustStorePath != null) {
            trustStore = new TrustStore(trustStorePath, trustStoreType);
        }
        return new Http(connectTimeout, requestTimeout, new Ssl(trustStore));
    }

}
