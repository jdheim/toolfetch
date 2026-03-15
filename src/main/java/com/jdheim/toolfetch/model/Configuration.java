/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model;

import java.util.List;

public record Configuration(String destination, List<Tool> tools) {

    public Configuration {
        tools = List.copyOf(tools);
    }

}
