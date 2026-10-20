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
public final class TestHttpBuilder {

    private @Nullable Integer connectTimeout;

    private @Nullable Integer requestTimeout;

    private @Nullable TestTrustStoreBuilder trustStore;

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

    public TestHttpBuilder trustStore(TestTrustStoreBuilder trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public Http build() {
        TrustStore builtTrustStore = trustStore == null ? null : trustStore.build();
        return new Http(connectTimeout, requestTimeout, new Ssl(builtTrustStore));
    }

}
