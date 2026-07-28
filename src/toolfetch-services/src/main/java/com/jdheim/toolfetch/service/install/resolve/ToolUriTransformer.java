/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.net.URI;
import com.jdheim.toolfetch.model.tool.Tool;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolUriTransformer implements UriTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(ToolUriTransformer.class);

    private static final String VERSION_VARIABLE = "${version}";

    private static final String HTTP_SCHEME = "http";

    private static final String HTTPS_SCHEME = "https";

    @Override
    public @Nullable URI transform(Tool tool) {
        String url = tool.url();
        if (url.contains(VERSION_VARIABLE)) {
            String version = tool.version();
            if (version == null) {
                LOG.warn("Missing required parameter: version");
                return null;
            }
            url = url.replace(VERSION_VARIABLE, version);
        }
        return toUri(url);
    }

    private @Nullable URI toUri(String url) {
        URI uri = URI.create(url);
        if (isNotHttpHttpsScheme(uri)) {
            LOG.warn("Forbidden scheme detected. Only {}/{} are allowed", HTTP_SCHEME, HTTPS_SCHEME);
            return null;
        }
        return uri;
    }

    private boolean isNotHttpHttpsScheme(URI uri) {
        return !HTTP_SCHEME.equalsIgnoreCase(uri.getScheme()) && !HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme());
    }

}
