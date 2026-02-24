/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.net.URI;
import com.jdheim.toolfetch.model.Tool;
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
            if (tool.version() == null) {
                LOG.warn("Missing required parameter: version");
                return null;
            }
            url = url.replace(VERSION_VARIABLE, tool.version());
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
