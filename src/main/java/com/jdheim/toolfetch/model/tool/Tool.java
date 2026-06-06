/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model.tool;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.tool.checksums.Checksums;
import com.jdheim.toolfetch.service.env.EnvResolver;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public record Tool(String id, String url, @Nullable String version, @Nullable String destination, @Nullable Checksums checksums) {

    public Tool(String id, String url) {
        this(id, url, null);
    }

    public Tool(String id, String url, @Nullable String version) {
        this(id, url, version, null);
    }

    public Tool(String id, String url, @Nullable String version, @Nullable String destination) {
        this(id, url, version, destination, null);
    }

    public @Nullable Path resolvedDestination() {
        if (StringUtils.isBlank(destination)) {
            return null;
        }
        return EnvResolver.resolveAsPath(destination);
    }

}
