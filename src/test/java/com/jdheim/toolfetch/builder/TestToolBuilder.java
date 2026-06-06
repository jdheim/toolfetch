/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.builder;

import com.jdheim.toolfetch.model.tool.Tool;
import org.jspecify.annotations.Nullable;

/// Tool Builder for Tests
public final class TestToolBuilder {

    private static final String DEFAULT_ID = "toolfetch";

    private static final String DEFAULT_RESOLVED_URL = "https://github.com/jdheim/toolfetch/releases/download/v1.0.0/toolfetch-1.0.0-linux-amd64.tar.gz";

    private static final String DEFAULT_UNRESOLVED_URL = "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz";

    private static final String DEFAULT_VERSION = "1.0.0";

    private static final String DEFAULT_DESTINATION = "/tmp/custom";

    private String id;

    private String url;

    private @Nullable String version;

    private @Nullable String destination;

    private TestToolBuilder() {
        id = DEFAULT_ID;
        url = DEFAULT_RESOLVED_URL;
    }

    public static TestToolBuilder tool() {
        return new TestToolBuilder();
    }

    public TestToolBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestToolBuilder url(String url) {
        this.url = url;
        return this;
    }

    public TestToolBuilder withDefaultUrlVersion() {
        url = DEFAULT_UNRESOLVED_URL;
        version = DEFAULT_VERSION;
        return this;
    }

    public TestToolBuilder version(String version) {
        this.version = version;
        return this;
    }

    public TestToolBuilder withDefaultVersion() {
        version = DEFAULT_VERSION;
        return this;
    }

    public TestToolBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public TestToolBuilder withDefaultDestination() {
        destination = DEFAULT_DESTINATION;
        return this;
    }

    public Tool build() {
        return new Tool(id, url, version, destination);
    }

}
