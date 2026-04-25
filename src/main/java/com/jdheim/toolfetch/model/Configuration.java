/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model;

import java.nio.file.Path;
import java.util.List;
import com.jdheim.toolfetch.model.tool.Tool;
import com.jdheim.toolfetch.service.env.EnvResolver;

public record Configuration(String destination, List<Tool> tools) {

    public Configuration {
        tools = List.copyOf(tools);
    }

    public Path resolvedDestination() {
        return Path.of(EnvResolver.resolve(destination));
    }

}
