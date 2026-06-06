/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import java.util.ArrayList;
import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.http.Http;
import com.jdheim.toolfetch.model.tool.Tool;
import org.jspecify.annotations.Nullable;

/// Configuration Builder for Tests
public final class TestConfigurationBuilder {

    private static final String DEFAULT_DESTINATION = "/tmp";

    private final List<Tool> tools;

    private String destination;

    private @Nullable Http http;

    private TestConfigurationBuilder() {
        tools = new ArrayList<>();
        destination = DEFAULT_DESTINATION;
    }

    public static TestConfigurationBuilder configuration() {
        return new TestConfigurationBuilder();
    }

    public TestConfigurationBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public TestConfigurationBuilder addTool(Tool tool) {
        tools.add(tool);
        return this;
    }

    public TestConfigurationBuilder addTools(Tool... tools) {
        this.tools.addAll(List.of(tools));
        return this;
    }

    public TestConfigurationBuilder http(Http http) {
        this.http = http;
        return this;
    }

    public TestConfigurationBuilder defaultHttp() {
        http = TestHttpBuilder.http().build();
        return this;
    }

    public Configuration build() {
        if (tools.isEmpty()) {
            tools.add(TestToolBuilder.tool().build());
        }
        return new Configuration(destination, tools, http);
    }

}
