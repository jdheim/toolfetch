/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import java.util.ArrayList;
import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.tool.Tool;
import org.jspecify.annotations.Nullable;

/// Configuration Builder for Tests
public final class TestConfigurationBuilder {

    private static final String DEFAULT_DESTINATION = "/tmp";

    private final List<TestToolBuilder> tools;

    private boolean defaultTool;

    private String destination;

    private @Nullable TestHttpBuilder http;

    private TestConfigurationBuilder() {
        tools = new ArrayList<>();
        defaultTool = true;
        destination = DEFAULT_DESTINATION;
    }

    public static TestConfigurationBuilder configuration() {
        return new TestConfigurationBuilder();
    }

    public TestConfigurationBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public TestConfigurationBuilder addTool(TestToolBuilder tool) {
        tools.add(tool);
        return this;
    }

    public TestConfigurationBuilder addTools(TestToolBuilder... tools) {
        for (TestToolBuilder tool : tools) {
            addTool(tool);
        }
        return this;
    }

    public TestConfigurationBuilder withoutTools() {
        tools.clear();
        defaultTool = false;
        return this;
    }

    public TestConfigurationBuilder http(TestHttpBuilder http) {
        this.http = http;
        return this;
    }

    public TestConfigurationBuilder defaultHttp() {
        http = TestHttpBuilder.http();
        return this;
    }

    public Configuration build() {
        if (defaultTool && tools.isEmpty()) {
            tools.add(TestToolBuilder.tool());
        }
        List<Tool> builtTools = tools.stream().map(TestToolBuilder::build).toList();
        if (http == null) {
            return new Configuration(destination, builtTools);
        }
        return new Configuration(destination, builtTools, http.build());
    }

}
