/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model;

import java.nio.file.Path;
import java.util.List;
import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.http.ssl.Ssl;
import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.env.EnvResolver;
import org.jspecify.annotations.Nullable;

public record Configuration(String destination, List<Tool> tools, @Nullable Http http) {

    public Configuration {
        tools = List.copyOf(tools);
    }

    public Configuration(String destination, List<Tool> tools) {
        this(destination, tools, null);
    }

    public @Nullable TrustStore trustStore() {
        if (http == null) {
            return null;
        }
        Ssl ssl = http.ssl();
        if (ssl == null) {
            return null;
        }
        return ssl.trustStore();
    }

    public Path resolvedDestination() {
        return EnvResolver.resolveAsPath(destination);
    }

}
