/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import java.util.HashMap;
import java.util.Map;
import com.jdheim.toolfetch.model.tool.checksums.Checksums;

/// Checksums Builder for Tests
public final class TestChecksumsBuilder {

    private final Map<String, String> values;

    private TestChecksumsBuilder() {
        values = new HashMap<>();
    }

    public static TestChecksumsBuilder checksums() {
        return new TestChecksumsBuilder();
    }

    public TestChecksumsBuilder sha256(String value) {
        values.put("sha256", value);
        return this;
    }

    public TestChecksumsBuilder sha384(String value) {
        values.put("sha384", value);
        return this;
    }

    public TestChecksumsBuilder sha512(String value) {
        values.put("sha512", value);
        return this;
    }

    public Checksums build() {
        return new Checksums(values);
    }

}
