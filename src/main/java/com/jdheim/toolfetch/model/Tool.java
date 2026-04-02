/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model;

import org.jspecify.annotations.Nullable;

public record Tool(String id, @Nullable String version, String url, @Nullable String destination, @Nullable Checksums checksums) {

    public Tool(String id, @Nullable String version, String url, @Nullable String destination) {
        this(id, version, url, destination, null);
    }

}
