/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model.http.ssl.truststore;

import java.nio.file.Path;
import com.jdheim.toolfetch.service.env.EnvResolver;
import org.jspecify.annotations.Nullable;

public record TrustStore(String path, @Nullable String type) {

    public Path resolvedPath() {
        return EnvResolver.resolveAsPath(path);
    }

}
