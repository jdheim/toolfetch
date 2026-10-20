/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import org.jspecify.annotations.Nullable;

/// TrustStore Builder for Tests
public final class TestTrustStoreBuilder {

    private String path;

    private @Nullable String type;

    private TestTrustStoreBuilder() {
        path = "custom/cacerts";
    }

    public static TestTrustStoreBuilder trustStore() {
        return new TestTrustStoreBuilder();
    }

    public TestTrustStoreBuilder path(String path) {
        this.path = path;
        return this;
    }

    public TestTrustStoreBuilder type(@Nullable String type) {
        this.type = type;
        return this;
    }

    public TrustStore build() {
        return new TrustStore(path, type);
    }

}
